package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.exception.DuplicateEmailException;
import com.modernizedkitechensink.kitchensinkmodernized.exception.MemberNotFoundException;
import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for Member operations.
 *
 * Includes caching for improved performance:
 * - @Cacheable: Cache the result
 * - @CachePut: Update cache with new value
 * - @CacheEvict: Remove from cache when data changes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements IMemberService {

  private final MemberRepository memberRepository;

  /**
   * Find all members - cached.
   * Cache key: "all" (since no parameters)
   */
  @Override
  @Cacheable(value = "members", key = "'all'")
  public List<Member> findAll() {
    log.debug("Cache MISS - fetching all members from database");
    return memberRepository.findAllByOrderByNameAsc();
  }

  /**
   * Find all members with pagination - NO CACHING for now.
   * 
   * TODO: Fix Redis serialization for Page<Member> objects.
   * Spring Data's Page interface is not directly serializable by Redis JSON serializer.
   * For production, we'd either:
   * 1. Convert Page to a custom DTO before caching
   * 2. Use a different cache serialization strategy
   * 3. Cache the content list separately from the pagination metadata
   */
  @Override
  // @Cacheable(value = "members", key = "'page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
  public Page<Member> findAll(Pageable pageable) {
    log.debug("Fetching members page {} from database (caching disabled)", pageable.getPageNumber());
    return memberRepository.findAll(pageable);
  }

  /**
   * Find member by ID - cached.
   * Cache key: the member ID
   */
  @Override
  @Cacheable(value = "members", key = "#id")
  public Member findById(String id) {
    log.debug("Cache MISS - fetching member {} from database", id);
    return memberRepository.findById(id)
      .orElseThrow(() -> new MemberNotFoundException(id));
  }

  /**
   * Register new member.
   * Evicts the "all" cache since the list has changed.
   * Puts the new member into cache.
   * 
   * Race Condition Protection:
   * - Application-level check (line 64-66): Provides fast fail with clear error
   * - Database unique index: Ultimate protection against concurrent inserts
   * - Catches DuplicateKeyException from MongoDB if race condition occurs
   */
  @Override
  @CacheEvict(value = "members", key = "'all'")
  public Member register(Member member) {
    log.info("Registering new member: {}", member.getName());

    // Application-level check (fast path)
    if (memberRepository.findByEmail(member.getEmail()).isPresent()) {
      throw new DuplicateEmailException(member.getEmail());
    }

    try {
      Member saved = memberRepository.save(member);
      log.info("Member registered with id: {}", saved.getId());
      return saved;
    } catch (DuplicateKeyException e) {
      // Race condition: Another thread/session saved the same email between check and save
      log.warn("Race condition detected: duplicate email {} during concurrent registration", member.getEmail());
      throw new DuplicateEmailException(member.getEmail());
    }
  }

  /**
   * Update member.
   * Evicts both the specific member cache and the "all" list cache.
   * 
   * Race Condition Protection:
   * - Checks email uniqueness before updating
   * - Catches DuplicateKeyException if concurrent update creates conflict
   */
  @Override
  @CacheEvict(value = "members", allEntries = true)
  public Member update(String id, Member memberData) {
    log.info("Updating member with id: {}", id);

    Member existingMember = memberRepository.findById(id)
      .orElseThrow(() -> new MemberNotFoundException(id));

    // Check email uniqueness if email is changing
    if (!existingMember.getEmail().equals(memberData.getEmail())) {
      Optional<Member> memberWithEmail = memberRepository.findByEmail(memberData.getEmail());
      if (memberWithEmail.isPresent() && !memberWithEmail.get().getId().equals(id)) {
        throw new DuplicateEmailException(memberData.getEmail());
      }
    }

    existingMember.setName(memberData.getName());
    existingMember.setEmail(memberData.getEmail());
    existingMember.setPhoneNumber(memberData.getPhoneNumber());

    try {
      Member updated = memberRepository.save(existingMember);
      log.info("Member updated: {}", updated.getId());
      return updated;
    } catch (DuplicateKeyException e) {
      // Race condition: Email became duplicate during update
      log.warn("Race condition detected: duplicate email {} during concurrent update", memberData.getEmail());
      throw new DuplicateEmailException(memberData.getEmail());
    }
  }

  /**
   * Delete member.
   * Evicts all cache entries since data has changed.
   */
  @Override
  @CacheEvict(value = "members", allEntries = true)
  public void delete(String id) {
    log.info("Deleting member with id: {}", id);

    if (!memberRepository.existsById(id)) {
      throw new MemberNotFoundException(id);
    }

    memberRepository.deleteById(id);
    log.info("Member deleted: {}", id);
  }
}
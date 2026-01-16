package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.exception.DuplicateEmailException;
import com.modernizedkitechensink.kitchensinkmodernized.exception.MemberNotFoundException;
import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
   */
  @Override
  @CacheEvict(value = "members", key = "'all'")
  public Member register(Member member) {
    log.info("Registering new member: {}", member.getName());

    if (memberRepository.findByEmail(member.getEmail()).isPresent()) {
      throw new DuplicateEmailException(member.getEmail());
    }

    Member saved = memberRepository.save(member);
    log.info("Member registered with id: {}", saved.getId());
    return saved;
  }

  /**
   * Update member.
   * Evicts both the specific member cache and the "all" list cache.
   */
  @Override
  @CacheEvict(value = "members", allEntries = true)
  public Member update(String id, Member memberData) {
    log.info("Updating member with id: {}", id);

    Member existingMember = memberRepository.findById(id)
      .orElseThrow(() -> new MemberNotFoundException(id));

    if (!existingMember.getEmail().equals(memberData.getEmail())) {
      Optional<Member> memberWithEmail = memberRepository.findByEmail(memberData.getEmail());
      if (memberWithEmail.isPresent() && !memberWithEmail.get().getId().equals(id)) {
        throw new DuplicateEmailException(memberData.getEmail());
      }
    }

    existingMember.setName(memberData.getName());
    existingMember.setEmail(memberData.getEmail());
    existingMember.setPhoneNumber(memberData.getPhoneNumber());

    Member updated = memberRepository.save(existingMember);
    log.info("Member updated: {}", updated.getId());
    return updated;
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
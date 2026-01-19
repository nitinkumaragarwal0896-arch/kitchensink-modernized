package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.exception.DuplicateEmailException;
import com.modernizedkitechensink.kitchensinkmodernized.exception.MemberNotFoundException;
import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.repository.MemberRepository;
import com.modernizedkitechensink.kitchensinkmodernized.validation.EmailValidationService;
import com.modernizedkitechensink.kitchensinkmodernized.validation.PhoneValidationService;
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
  private final PhoneValidationService phoneValidationService;
  private final EmailValidationService emailValidationService;

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
   * Search members by name, email, or phone number.
   * Uses MongoDB regex for case-insensitive partial matching.
   * 
   * NO CACHING for search results (they're dynamic and user-specific).
   * 
   * @param searchTerm The search term
   * @param pageable Pagination and sorting parameters
   * @return Page of matching members
   */
  @Override
  public Page<Member> searchMembers(String searchTerm, Pageable pageable) {
    log.debug("Searching members with term: '{}', page: {}", searchTerm, pageable.getPageNumber());
    
    if (searchTerm == null || searchTerm.trim().isEmpty()) {
      return findAll(pageable);
    }
    
    return memberRepository.searchMembers(searchTerm.trim(), pageable);
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

    // Step 1: Validate phone and email using validation services
    validateMember(member);

    // Step 2: Application-level duplicate check (fast path)
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

    // Step 1: Validate phone and email using validation services
    validateMember(memberData);

    Member existingMember = memberRepository.findById(id)
      .orElseThrow(() -> new MemberNotFoundException(id));

    // Step 2: Check email uniqueness if email is changing
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

  /**
   * Validates a member's phone and email using validation services.
   * Throws IllegalArgumentException with detailed error message if validation fails.
   * 
   * This provides consistent, production-grade validation with clear error messages
   * that match what the frontend will show.
   * 
   * @param member The member to validate
   * @throws IllegalArgumentException if validation fails (with specific error message)
   */
  private void validateMember(Member member) {
    // Validate email
    EmailValidationService.ValidationResult emailResult = 
        emailValidationService.validate(member.getEmail());
    if (!emailResult.isValid()) {
      log.warn("Email validation failed for {}: {}", member.getEmail(), emailResult.getErrorMessage());
      throw new IllegalArgumentException(emailResult.getErrorMessage());
    }

    // Validate phone number
    PhoneValidationService.ValidationResult phoneResult = 
        phoneValidationService.validate(member.getPhoneNumber());
    if (!phoneResult.isValid()) {
      log.warn("Phone validation failed for {}: {}", member.getPhoneNumber(), phoneResult.getErrorMessage());
      throw new IllegalArgumentException(phoneResult.getErrorMessage());
    }

    log.debug("Member validation passed for email: {} and phone: {}", 
              emailValidationService.normalize(member.getEmail()),
              phoneValidationService.normalize(member.getPhoneNumber()));
  }
}
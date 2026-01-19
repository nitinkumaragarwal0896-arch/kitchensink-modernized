package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IMemberService {

  /**
   * Find all members, ordered by name.
   */
  List<Member> findAll();

  /**
   * Find all members with pagination and sorting.
   * @param pageable Pagination and sorting parameters
   * @return Page of members
   */
  Page<Member> findAll(Pageable pageable);

  /**
   * Search members by name, email, or phone number.
   * @param searchTerm The search term (case-insensitive)
   * @param pageable Pagination and sorting parameters
   * @return Page of matching members
   */
  Page<Member> searchMembers(String searchTerm, Pageable pageable);

  /**
   * Find member by ID.
   * @throws MemberNotFoundException if not found
   */
  Member findById(String id);

  /**
   * Register (create) a new member.
   * @throws DuplicateEmailException if email already exists
   */
  Member register(Member member);

  /**
   * Update an existing member.
   * @throws MemberNotFoundException if not found
   * @throws DuplicateEmailException if new email already taken by another member
   */
  Member update(String id, Member member);

  /**
   * Delete a member by ID.
   * @throws MemberNotFoundException if not found
   */
  void delete(String id);
}

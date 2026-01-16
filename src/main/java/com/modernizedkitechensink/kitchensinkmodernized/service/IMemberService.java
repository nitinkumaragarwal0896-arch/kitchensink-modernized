package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;

import java.util.List;

public interface IMemberService {

  /**
   * Find all members, ordered by name.
   */
  List<Member> findAll();

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

package com.modernizedkitechensink.kitchensinkmodernized.controller;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.service.IMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Member operations.
 *
 * Endpoints are protected with permission-based access control.
 * Permissions are checked via @PreAuthorize annotations.
 *
 * Legacy equivalent: MemberResourceRESTService.java (JAX-RS)
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

  private final IMemberService memberService;

  /**
   * Get all members.
   *
   * GET /api/v1/members
   * Required permission: member:read
   */
  @GetMapping
  @PreAuthorize("hasAuthority('member:read')")
  public List<Member> getAllMembers() {
    log.debug("Fetching all members");
    return memberService.findAll();
  }

  /**
   * Get member by ID.
   *
   * GET /api/v1/members/{id}
   * Required permission: member:read
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('member:read')")
  public Member getMemberById(@PathVariable String id) {
    log.debug("Fetching member with id: {}", id);
    return memberService.findById(id);
  }

  /**
   * Create new member.
   *
   * POST /api/v1/members
   * Required permission: member:create
   */
  @PostMapping
  @PreAuthorize("hasAuthority('member:create')")
  public ResponseEntity<Member> createMember(@Valid @RequestBody Member member) {
    log.info("Creating new member: {}", member.getName());
    Member created = memberService.register(member);
    return ResponseEntity.ok(created);
  }

  /**
   * Update existing member.
   *
   * PUT /api/v1/members/{id}
   * Required permission: member:update
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('member:update')")
  public ResponseEntity<Member> updateMember(
    @PathVariable String id,
    @Valid @RequestBody Member member) {
    log.info("Updating member with id: {}", id);
    Member updated = memberService.update(id, member);
    return ResponseEntity.ok(updated);
  }

  /**
   * Delete member.
   *
   * DELETE /api/v1/members/{id}
   * Required permission: member:delete
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('member:delete')")
  public ResponseEntity<Void> deleteMember(@PathVariable String id) {
    log.info("Deleting member with id: {}", id);
    memberService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
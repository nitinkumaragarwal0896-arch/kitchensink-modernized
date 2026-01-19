package com.modernizedkitechensink.kitchensinkmodernized.controller;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.service.IMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
   * Get all members with pagination.
   *
   * GET /api/v1/members?page=0&size=10&sort=name,asc
   * Required permission: member:read
   *
   * Query parameters:
   * - page: Page number (0-indexed, default: 0)
   * - size: Items per page (default: 10, max: 100)
   * - sort: Sort field and direction (e.g., "name,asc", "email,desc")
   *
   * Response format:
   * {
   *   "content": [...],           // Array of members
   *   "page": 0,                  // Current page number
   *   "size": 10,                 // Items per page
   *   "totalElements": 47,        // Total number of members
   *   "totalPages": 5,            // Total number of pages
   *   "first": true,              // Is this the first page?
   *   "last": false               // Is this the last page?
   * }
   */
  @GetMapping
  @PreAuthorize("hasAuthority('member:read')")
  public ResponseEntity<Map<String, Object>> getAllMembers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "name,asc") String sort,
    @RequestParam(required = false) String search
  ) {
    log.debug("Fetching members: page={}, size={}, sort={}, search={}", page, size, sort, search);

    // Validate and limit page size (max 100)
    if (size > 100) {
      size = 100;
      log.warn("Page size limited to 100");
    }

    // Parse sort parameter (e.g., "name,asc" -> Sort.by("name").ascending())
    String[] sortParams = sort.split(",");
    String sortField = sortParams[0];
    Sort.Direction sortDirection = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
      ? Sort.Direction.DESC
      : Sort.Direction.ASC;

    // Create pageable request
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

    // Fetch paginated data (with or without search)
    Page<Member> memberPage;
    if (search != null && !search.trim().isEmpty()) {
      memberPage = memberService.searchMembers(search, pageable);
      log.info("Found {} members matching search term: '{}'", memberPage.getTotalElements(), search);
    } else {
      memberPage = memberService.findAll(pageable);
    }

    // Build response with pagination metadata
    Map<String, Object> response = new HashMap<>();
    response.put("content", memberPage.getContent());
    response.put("page", memberPage.getNumber());
    response.put("size", memberPage.getSize());
    response.put("totalElements", memberPage.getTotalElements());
    response.put("totalPages", memberPage.getTotalPages());
    response.put("first", memberPage.isFirst());
    response.put("last", memberPage.isLast());

    return ResponseEntity.ok(response);
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
package com.modernizedkitechensink.kitchensinkmodernized.controller;

import com.modernizedkitechensink.kitchensinkmodernized.model.Job;
import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.service.IMemberService;
import com.modernizedkitechensink.kitchensinkmodernized.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * REST Controller for Member operations.
 *
 * Endpoints are protected with permission-based access control.
 * Permissions are checked via @PreAuthorize annotations.
 *
 * Supports internationalization (i18n) - responses localized based on Accept-Language header.
 *
 * Legacy equivalent: MemberResourceRESTService.java (JAX-RS)
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

  private final IMemberService memberService;
  private final JobService jobService;
  private final MessageSource messageSource;

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
  public ResponseEntity<Map<String, Object>> createMember(@Valid @RequestBody Member member) {
    log.info("Creating new member: {}", member.getName());
    Member created = memberService.register(member);
    
    // Get localized success message
    Locale locale = LocaleContextHolder.getLocale();
    String message = messageSource.getMessage("member.created", null, locale);
    
    Map<String, Object> response = Map.of(
      "data", created,
      "message", message
    );
    
    return ResponseEntity.ok(response);
  }

  /**
   * Update existing member.
   *
   * PUT /api/v1/members/{id}
   * Required permission: member:update
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('member:update')")
  public ResponseEntity<Map<String, Object>> updateMember(
    @PathVariable String id,
    @Valid @RequestBody Member member) {
    log.info("Updating member with id: {}", id);
    Member updated = memberService.update(id, member);
    
    // Get localized success message
    Locale locale = LocaleContextHolder.getLocale();
    String message = messageSource.getMessage("member.updated", null, locale);
    
    Map<String, Object> response = Map.of(
      "data", updated,
      "message", message
    );
    
    return ResponseEntity.ok(response);
  }

  /**
   * Delete member.
   *
   * DELETE /api/v1/members/{id}
   * Required permission: member:delete
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('member:delete')")
  public ResponseEntity<Map<String, String>> deleteMember(@PathVariable String id) {
    log.info("Deleting member with id: {}", id);
    memberService.delete(id);
    
    // Get localized success message
    Locale locale = LocaleContextHolder.getLocale();
    String message = messageSource.getMessage("member.deleted", null, locale);
    
    return ResponseEntity.ok(Map.of("message", message));
  }

  /**
   * Bulk delete members (asynchronous).
   *
   * POST /api/v1/members/bulk-delete
   * Body: { "memberIds": ["id1", "id2", "id3", ...] }
   * Required permission: member:delete
   * 
   * Returns immediately with jobId. Client should poll /api/v1/jobs/{jobId} for status.
   */
  @PostMapping("/bulk-delete")
  @PreAuthorize("hasAuthority('member:delete')")
  public ResponseEntity<?> bulkDeleteMembers(
    @RequestBody Map<String, List<String>> request,
    Authentication authentication
  ) {
    List<String> memberIds = request.get("memberIds");
    
    if (memberIds == null || memberIds.isEmpty()) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "memberIds is required and must not be empty"));
    }

    String userId = authentication.getName();
    String username = authentication.getName(); // In production, get from UserDetails

    log.info("User {} initiated bulk delete of {} members", username, memberIds.size());

    // Create job
    Job job = jobService.createJob(Job.JobType.BULK_DELETE, userId, username, memberIds.size());

    // Start async processing
    jobService.processBulkDelete(job.getId(), memberIds);

    // Return job ID immediately
    return ResponseEntity.status(HttpStatus.ACCEPTED)
      .body(Map.of(
        "jobId", job.getId(),
        "status", job.getStatus().name(),
        "totalItems", job.getTotalItems(),
        "message", "Bulk delete job created successfully. Poll /api/v1/jobs/" + job.getId() + " for status."
      ));
  }

  /**
   * Excel bulk upload members (asynchronous) - ONE job for entire file.
   *
   * POST /api/v1/members/excel-upload
   * Content-Type: multipart/form-data
   * Required permission: member:create
   * 
   * Expected Excel format:
   * Row 1: Header (Name, Email, Phone)
   * Row 2+: Data rows
   * 
   * Returns immediately with jobId. Client should poll /api/v1/jobs/{jobId} for status.
   */
  @PostMapping("/excel-upload")
  @PreAuthorize("hasAuthority('member:create')")
  public ResponseEntity<?> excelUpload(
    @RequestParam("file") MultipartFile file,
    Authentication authentication
  ) {
    // Validate file
    if (file.isEmpty()) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "File is required and must not be empty"));
    }

    String fileName = file.getOriginalFilename();
    if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "File must be an Excel file (.xlsx or .xls)"));
    }

    String userId = authentication.getName();
    String username = authentication.getName();

    log.info("User {} initiated Excel upload: {}", username, fileName);

    // Read file bytes before async processing (temp file gets deleted after controller returns)
    byte[] fileBytes;
    try {
      fileBytes = file.getBytes();
    } catch (Exception e) {
      log.error("Failed to read file bytes: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", "Failed to read file: " + e.getMessage()));
    }

    // Create job (we don't know row count yet, will be updated during processing)
    Job job = jobService.createJob(Job.JobType.EXCEL_UPLOAD, userId, username, 0);

    // Start async processing (pass file bytes instead of MultipartFile)
    jobService.processExcelUpload(job.getId(), fileBytes, fileName);

    // Return job ID immediately
    return ResponseEntity.status(HttpStatus.ACCEPTED)
      .body(Map.of(
        "jobId", job.getId(),
        "status", job.getStatus().name(),
        "fileName", fileName,
        "message", "Excel upload job created successfully. Poll /api/v1/jobs/" + job.getId() + " for status."
      ));
  }
}
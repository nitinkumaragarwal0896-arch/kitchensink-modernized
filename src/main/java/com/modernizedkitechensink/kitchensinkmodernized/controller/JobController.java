package com.modernizedkitechensink.kitchensinkmodernized.controller;

import com.modernizedkitechensink.kitchensinkmodernized.model.Job;
import com.modernizedkitechensink.kitchensinkmodernized.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Job management.
 * 
 * Provides endpoints for:
 * - Polling job status
 * - Listing user's jobs
 * - Cancelling jobs
 * - Deleting completed jobs
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

  private final JobService jobService;

  /**
   * Get job by ID (for polling status).
   * 
   * GET /api/v1/jobs/{jobId}
   * 
   * Required permission: Authenticated user (must own the job)
   */
  @GetMapping("/{jobId}")
  public ResponseEntity<?> getJobById(@PathVariable String jobId, Authentication authentication) {
    String userId = authentication.getName();

    Optional<Job> jobOpt = jobService.getJobById(jobId);
    if (jobOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", "Job not found"));
    }

    Job job = jobOpt.get();

    // Verify ownership
    if (!job.getUserId().equals(userId)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("error", "Access denied"));
    }

    return ResponseEntity.ok(job);
  }

  /**
   * Get all jobs for the current user.
   * 
   * GET /api/v1/jobs
   * 
   * Required permission: Authenticated user
   */
  @GetMapping
  public ResponseEntity<List<Job>> getAllJobs(Authentication authentication) {
    String userId = authentication.getName();
    List<Job> jobs = jobService.getJobsByUserId(userId);
    return ResponseEntity.ok(jobs);
  }

  /**
   * Get active jobs (PENDING or IN_PROGRESS) for the current user.
   * 
   * GET /api/v1/jobs/active
   * 
   * Required permission: Authenticated user
   */
  @GetMapping("/active")
  public ResponseEntity<List<Job>> getActiveJobs(Authentication authentication) {
    String userId = authentication.getName();
    List<Job> jobs = jobService.getActiveJobsByUserId(userId);
    return ResponseEntity.ok(jobs);
  }

  /**
   * Cancel a job.
   * 
   * POST /api/v1/jobs/{jobId}/cancel
   * 
   * Required permission: Authenticated user (must own the job)
   */
  @PostMapping("/{jobId}/cancel")
  public ResponseEntity<?> cancelJob(@PathVariable String jobId, Authentication authentication) {
    String userId = authentication.getName();

    boolean cancelled = jobService.cancelJob(jobId, userId);
    if (!cancelled) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "Job cannot be cancelled (not found, not owned, or already completed)"));
    }

    return ResponseEntity.ok(Map.of("message", "Job cancelled successfully"));
  }

  /**
   * Delete a completed job from history.
   * 
   * DELETE /api/v1/jobs/{jobId}
   * 
   * Required permission: Authenticated user (must own the job)
   */
  @DeleteMapping("/{jobId}")
  public ResponseEntity<?> deleteJob(@PathVariable String jobId, Authentication authentication) {
    String userId = authentication.getName();

    boolean deleted = jobService.deleteJob(jobId, userId);
    if (!deleted) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "Job cannot be deleted (not found, not owned, or still in progress)"));
    }

    return ResponseEntity.ok(Map.of("message", "Job deleted successfully"));
  }
}


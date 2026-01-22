package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.model.Job;
import com.modernizedkitechensink.kitchensinkmodernized.model.JobResultItem;
import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.repository.JobRepository;
import com.modernizedkitechensink.kitchensinkmodernized.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing background jobs (bulk operations).
 * 
 * Supports:
 * - Creating and tracking jobs
 * - Async bulk delete operations
 * - Job status updates
 * - Automatic cleanup of old jobs
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

  private final JobRepository jobRepository;
  private final MemberRepository memberRepository;
  private final IMemberService memberService;

  /**
   * Create a new job.
   */
  public Job createJob(Job.JobType type, String userId, String username, int totalItems) {
    Job job = Job.builder()
      .type(type)
      .status(Job.JobStatus.PENDING)
      .userId(userId)
      .username(username)
      .totalItems(totalItems)
      .createdAt(LocalDateTime.now())
      .build();

    return jobRepository.save(job);
  }

  /**
   * Get job by ID.
   */
  public Optional<Job> getJobById(String jobId) {
    return jobRepository.findById(jobId);
  }

  /**
   * Get all jobs for a user.
   */
  public List<Job> getJobsByUserId(String userId) {
    return jobRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  /**
   * Get active jobs (PENDING or IN_PROGRESS) for a user.
   */
  public List<Job> getActiveJobsByUserId(String userId) {
    List<Job.JobStatus> activeStatuses = List.of(Job.JobStatus.PENDING, Job.JobStatus.IN_PROGRESS);
    return jobRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, activeStatuses);
  }

  /**
   * Cancel a job (if it's still pending or in progress).
   */
  public boolean cancelJob(String jobId, String userId) {
    Optional<Job> jobOpt = jobRepository.findById(jobId);
    if (jobOpt.isEmpty()) {
      return false;
    }

    Job job = jobOpt.get();

    // Verify ownership
    if (!job.getUserId().equals(userId)) {
      return false;
    }

    // Can only cancel PENDING or IN_PROGRESS jobs
    if (job.getStatus() != Job.JobStatus.PENDING && job.getStatus() != Job.JobStatus.IN_PROGRESS) {
      return false;
    }

    job.setStatus(Job.JobStatus.CANCELLED);
    job.setCompletedAt(LocalDateTime.now());
    jobRepository.save(job);

    log.info("Job {} cancelled by user {}", jobId, userId);
    return true;
  }

  /**
   * Delete a job from history (only completed/failed/cancelled jobs).
   */
  public boolean deleteJob(String jobId, String userId) {
    Optional<Job> jobOpt = jobRepository.findById(jobId);
    if (jobOpt.isEmpty()) {
      return false;
    }

    Job job = jobOpt.get();

    // Verify ownership
    if (!job.getUserId().equals(userId)) {
      return false;
    }

    // Can only delete completed/failed/cancelled jobs
    if (job.getStatus() == Job.JobStatus.PENDING || job.getStatus() == Job.JobStatus.IN_PROGRESS) {
      return false;
    }

    jobRepository.deleteById(jobId);
    log.info("Job {} deleted by user {}", jobId, userId);
    return true;
  }

  /**
   * Process bulk delete asynchronously.
   */
  @Async("bulkTaskExecutor")
  public void processBulkDelete(String jobId, List<String> memberIds) {
    log.info("Starting bulk delete job {} for {} members", jobId, memberIds.size());

    Optional<Job> jobOpt = jobRepository.findById(jobId);
    if (jobOpt.isEmpty()) {
      log.error("Job {} not found", jobId);
      return;
    }

    Job job = jobOpt.get();

    // Update status to IN_PROGRESS
    job.setStatus(Job.JobStatus.IN_PROGRESS);
    job.setStartedAt(LocalDateTime.now());
    jobRepository.save(job);

    List<JobResultItem> successfulResults = new ArrayList<>();
    List<JobResultItem> failedResults = new ArrayList<>();

    int processed = 0;

    for (String memberId : memberIds) {
      // Check if job was cancelled
      jobOpt = jobRepository.findById(jobId);
      if (jobOpt.isEmpty() || jobOpt.get().getStatus() == Job.JobStatus.CANCELLED) {
        log.info("Job {} was cancelled, stopping processing", jobId);
        return;
      }

      try {
        // Find member
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
          failedResults.add(JobResultItem.builder()
            .itemId(memberId)
            .itemDescription("Member ID: " + memberId)
            .errorMessage("Member not found")
            .build());
        } else {
          Member member = memberOpt.get();
          String memberEmail = member.getEmail();

          // Delete member
          memberRepository.deleteById(memberId);

          successfulResults.add(JobResultItem.builder()
            .itemId(memberId)
            .itemDescription(memberEmail)
            .build());

          log.debug("Deleted member {} ({})", memberId, memberEmail);
        }

        // TODO
        // Simulate some processing time (remove in production or adjust)
        Thread.sleep(100); // 100ms per member

      } catch (Exception e) {
        log.error("Error deleting member {}: {}", memberId, e.getMessage());
        failedResults.add(JobResultItem.builder()
          .itemId(memberId)
          .itemDescription("Member ID: " + memberId)
          .errorMessage(e.getMessage())
          .build());
      }

      processed++;

      // Update progress every 5 items or at the end
      if (processed % 5 == 0 || processed == memberIds.size()) {
        job = jobRepository.findById(jobId).orElse(job);
        job.setProcessedItems(processed);
        job.setSuccessfulItems(successfulResults.size());
        job.setFailedItems(failedResults.size());
        job.setProgress((processed * 100) / memberIds.size());
        job.setSuccessfulResults(new ArrayList<>(successfulResults));
        job.setFailedResults(new ArrayList<>(failedResults));
        jobRepository.save(job);
        log.debug("Job {} progress: {}/{}", jobId, processed, memberIds.size());
      }
    }

    // Mark job as completed
    job = jobRepository.findById(jobId).orElse(job);
    job.setStatus(failedResults.isEmpty() ? Job.JobStatus.COMPLETED : Job.JobStatus.COMPLETED);
    job.setProcessedItems(processed);
    job.setSuccessfulItems(successfulResults.size());
    job.setFailedItems(failedResults.size());
    job.setProgress(100);
    job.setCompletedAt(LocalDateTime.now());
    job.setSuccessfulResults(successfulResults);
    job.setFailedResults(failedResults);
    jobRepository.save(job);

    log.info("Bulk delete job {} completed: {} successful, {} failed",
      jobId, successfulResults.size(), failedResults.size());
  }

  /**
   * Process Excel upload asynchronously - ONE job for entire file.
   * 
   * Runs on dedicated BULK thread pool (bulkTaskExecutor).
   * Excel uploads are resource-intensive (parsing, validation, DB writes).
   * 
   * THREAD POOL: bulkTaskExecutor (2 core, 4 max, 20 queue)
   * - Shared with bulk delete (both are heavy operations)
   * - Max 4 concurrent Excel uploads to prevent memory issues
   * 
   * Expected Excel format:
   * Row 1: Header (Name, Email, Phone)
   * Row 2+: Data rows
   * 
   * @param jobId The job ID
   * @param fileBytes The Excel file content as byte array (to avoid temp file deletion)
   * @param fileName Original filename for logging
   */
  @Async("bulkTaskExecutor")
  public void processExcelUpload(String jobId, byte[] fileBytes, String fileName) {
    log.info("Starting Excel upload job {} for file: {}", jobId, fileName);

    Optional<Job> jobOpt = jobRepository.findById(jobId);
    if (jobOpt.isEmpty()) {
      log.error("Job {} not found", jobId);
      return;
    }

    Job job = jobOpt.get();

    // Update status to IN_PROGRESS
    job.setStatus(Job.JobStatus.IN_PROGRESS);
    job.setStartedAt(LocalDateTime.now());
    jobRepository.save(job);

    List<JobResultItem> successfulResults = new ArrayList<>();
    List<JobResultItem> failedResults = new ArrayList<>();

    int processed = 0;
    int rowNumber = 0;

    try (InputStream inputStream = new java.io.ByteArrayInputStream(fileBytes);
         Workbook workbook = new XSSFWorkbook(inputStream)) {

      Sheet sheet = workbook.getSheetAt(0);
      int totalRows = sheet.getPhysicalNumberOfRows() - 1; // Exclude header

      // Update total items (now that we know the count)
      job.setTotalItems(totalRows);
      jobRepository.save(job);

      for (Row row : sheet) {
        rowNumber++;

        // Skip header row
        if (rowNumber == 1) {
          continue;
        }

        // Check if job was cancelled
        jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty() || jobOpt.get().getStatus() == Job.JobStatus.CANCELLED) {
          log.info("Job {} was cancelled, stopping processing", jobId);
          return;
        }

        try {
          // Parse row data
          String name = getCellValueAsString(row.getCell(0));
          String email = getCellValueAsString(row.getCell(1));
          String phoneNumber = getCellValueAsString(row.getCell(2));

          // Validate basic fields
          // Create member DTO
          Member member = new Member();
          member.setName(name != null ? name.trim() : null);
          member.setEmail(email != null ? email.trim().toLowerCase() : null);
          member.setPhoneNumber(phoneNumber != null ? phoneNumber.trim() : null);

          // Use MemberService.register() which has all validations:
          // - Email uniqueness check
          // - Email format validation (@Email)
          // - Name validation (@NotBlank, @Size)
          // - Phone number validation (@Pattern)
          // - All other business rules
          memberService.register(member);

          successfulResults.add(JobResultItem.builder()
            .itemId(String.valueOf(rowNumber))
            .itemDescription("Row " + rowNumber + ": " + email)
            .build());

          log.debug("Imported member from row {} ({})", rowNumber, email);

          // TODO
          // Simulate some processing time (remove in production or adjust)
          Thread.sleep(100); // 100ms per row

        } catch (Exception e) {
          log.error("Error processing row {}: {}", rowNumber, e.getMessage());
          failedResults.add(JobResultItem.builder()
            .itemId(String.valueOf(rowNumber))
            .itemDescription("Row " + rowNumber)
            .errorMessage(e.getMessage())
            .build());
        }

        processed++;

        // Update progress every 5 rows or at the end
        if (processed % 5 == 0 || processed == totalRows) {
          job = jobRepository.findById(jobId).orElse(job);
          job.setProcessedItems(processed);
          job.setSuccessfulItems(successfulResults.size());
          job.setFailedItems(failedResults.size());
          job.setProgress((processed * 100) / totalRows);
          job.setSuccessfulResults(new ArrayList<>(successfulResults));
          job.setFailedResults(new ArrayList<>(failedResults));
          jobRepository.save(job);
          log.debug("Job {} progress: {}/{}", jobId, processed, totalRows);
        }
      }

      // Mark job as completed
      job = jobRepository.findById(jobId).orElse(job);
      job.setStatus(Job.JobStatus.COMPLETED);
      job.setProcessedItems(processed);
      job.setSuccessfulItems(successfulResults.size());
      job.setFailedItems(failedResults.size());
      job.setProgress(100);
      job.setCompletedAt(LocalDateTime.now());
      job.setSuccessfulResults(successfulResults);
      job.setFailedResults(failedResults);
      jobRepository.save(job);

      log.info("Excel upload job {} completed: {} successful, {} failed",
        jobId, successfulResults.size(), failedResults.size());

    } catch (Exception e) {
      log.error("Fatal error processing Excel upload job {}: {}", jobId, e.getMessage(), e);
      job = jobRepository.findById(jobId).orElse(job);
      job.setStatus(Job.JobStatus.FAILED);
      job.setErrorMessage(e.getMessage());
      job.setCompletedAt(LocalDateTime.now());
      jobRepository.save(job);
    }
  }

  /**
   * Helper method to get cell value as string.
   */
  private String getCellValueAsString(Cell cell) {
    if (cell == null) {
      return null;
    }

    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue();
      case NUMERIC:
        // Check if it's a date or number
        if (DateUtil.isCellDateFormatted(cell)) {
          return cell.getDateCellValue().toString();
        } else {
          // Convert number to string (handle phone numbers)
          double numericValue = cell.getNumericCellValue();
          // If it's a whole number, don't show decimal
          if (numericValue == Math.floor(numericValue)) {
            return String.valueOf((long) numericValue);
          } else {
            return String.valueOf(numericValue);
          }
        }
      case BOOLEAN:
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        return cell.getCellFormula();
      case BLANK:
        return null;
      default:
        return null;
    }
  }

  /**
   * Scheduled task to clean up old completed jobs.
   * Runs daily at 2 AM.
   */
  @Scheduled(cron = "0 0 2 * * *")
  public void cleanupOldJobs() {
    log.info("Starting cleanup of old jobs");

    // Delete jobs older than 7 days
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
    List<Job> oldJobs = jobRepository.findByCreatedAtBefore(cutoffDate);

    for (Job job : oldJobs) {
      // Only delete completed/failed/cancelled jobs
      if (job.getStatus() == Job.JobStatus.COMPLETED ||
        job.getStatus() == Job.JobStatus.FAILED ||
        job.getStatus() == Job.JobStatus.CANCELLED) {
        jobRepository.deleteById(job.getId());
        log.debug("Deleted old job {}", job.getId());
      }
    }

    log.info("Cleaned up {} old jobs", oldJobs.size());
  }
}


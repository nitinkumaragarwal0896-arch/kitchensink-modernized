package com.modernizedkitechensink.kitchensinkmodernized.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Job entity for tracking background/async operations.
 * 
 * Supports:
 * - Bulk delete operations
 * - Excel upload operations
 * - Progress tracking
 * - Error tracking
 * - Detailed results (success/failure)
 * 
 * INDEXES:
 * All indexes are managed centrally in MongoIndexInitializer.java for consistency.
 * 
 * Current indexes (3 total, optimized via iterative refinement):
 *   1. _id (unique, mandatory)
 *   2. createdAt_ttl_idx (TTL auto-deletion after 7 days + cleanup queries)
 *   3. userId_createdAt_status_idx (compound - covers ALL user-specific queries!)
 * 
 * Query patterns covered:
 *   - findById(jobId) → _id index (IDHACK)
 *   - findByUserIdOrderByCreatedAtDesc(userId) → userId_createdAt_status_idx (prefix match)
 *   - findByUserIdAndStatusInOrderByCreatedAtDesc(userId, status) → userId_createdAt_status_idx (full match)
 *   - findByCreatedAtBefore(dateTime) → createdAt_ttl_idx
 * 
 * KEY INSIGHT:
 * The compound index field order { userId, createdAt, status } follows the rule:
 *   Equality → Sort → Optional/Range
 * 
 * This allows ONE index to support BOTH query patterns via prefix matching,
 * eliminating the need for separate indexes and reducing write overhead.
 * 
 * @see com.modernizedkitechensink.kitchensinkmodernized.config.MongoIndexInitializer#createJobIndexes()
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Document(collection = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

  @Id
  private String id;

  private JobType type;

  private JobStatus status;

  private String userId;

  private String username;

  private Integer totalItems;

  @Builder.Default
  private Integer processedItems = 0;

  @Builder.Default
  private Integer successfulItems = 0;

  @Builder.Default
  private Integer failedItems = 0;

  @Builder.Default
  private Integer progress = 0; // 0-100

  @CreatedDate
  private LocalDateTime createdAt; // TTL index managed in MongoIndexInitializer

  private LocalDateTime startedAt;

  private LocalDateTime completedAt;

  private String errorMessage;

  // Detailed results
  @Builder.Default
  private List<JobResultItem> successfulResults = new ArrayList<>();

  @Builder.Default
  private List<JobResultItem> failedResults = new ArrayList<>();

  /**
   * Job types supported by the system.
   */
  public enum JobType {
    BULK_DELETE,
    EXCEL_UPLOAD
  }

  /**
   * Job status lifecycle:
   * PENDING -> IN_PROGRESS -> COMPLETED/FAILED/CANCELLED
   */
  public enum JobStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
  }
}


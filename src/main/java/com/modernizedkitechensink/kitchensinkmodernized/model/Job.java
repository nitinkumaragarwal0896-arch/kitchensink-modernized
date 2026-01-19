package com.modernizedkitechensink.kitchensinkmodernized.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
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
 * Indexes:
 * - userId: for querying user's jobs
 * - status: for filtering by job status
 * - createdAt: for sorting by creation date (TTL index candidate)
 * - compound(userId, status, createdAt): for efficient user job queries
 * - compound(status, createdAt): for cleanup queries
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Document(collection = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
  @CompoundIndex(name = "userId_status_createdAt_idx", def = "{'userId': 1, 'status': 1, 'createdAt': -1}"),
  @CompoundIndex(name = "status_createdAt_idx", def = "{'status': 1, 'createdAt': -1}")
})
public class Job {

  @Id
  private String id;

  @Indexed
  private JobType type;

  @Indexed
  private JobStatus status;

  @Indexed
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
  @Indexed(direction = org.springframework.data.mongodb.core.index.IndexDirection.DESCENDING, 
           expireAfterSeconds = 604800) // TTL index: auto-delete after 7 days
  private LocalDateTime createdAt;

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


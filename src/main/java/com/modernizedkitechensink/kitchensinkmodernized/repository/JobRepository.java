package com.modernizedkitechensink.kitchensinkmodernized.repository;

import com.modernizedkitechensink.kitchensinkmodernized.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Job entity.
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Repository
public interface JobRepository extends MongoRepository<Job, String> {

  /**
   * Find all jobs for a specific user.
   */
  List<Job> findByUserIdOrderByCreatedAtDesc(String userId);

  /**
   * Find jobs by status.
   */
  List<Job> findByStatusIn(List<Job.JobStatus> statuses);

  /**
   * Find jobs by user and status.
   */
  List<Job> findByUserIdAndStatusInOrderByCreatedAtDesc(String userId, List<Job.JobStatus> statuses);

  /**
   * Find recent jobs (for cleanup).
   */
  List<Job> findByCreatedAtBefore(LocalDateTime dateTime);

  /**
   * Delete old completed jobs (for cleanup).
   */
  void deleteByStatusAndCompletedAtBefore(Job.JobStatus status, LocalDateTime dateTime);
}


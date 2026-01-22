package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.model.Job;
import com.modernizedkitechensink.kitchensinkmodernized.model.JobResultItem;
import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.repository.JobRepository;
import com.modernizedkitechensink.kitchensinkmodernized.repository.MemberRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobService.
 * 
 * Tests job management, bulk operations, and scheduled cleanup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobService Tests")
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private IMemberService memberService;

    @InjectMocks
    private JobService jobService;

    private Job testJob;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testJob = Job.builder()
                .id("job123")
                .type(Job.JobType.BULK_DELETE)
                .status(Job.JobStatus.PENDING)
                .userId("user123")
                .username("testuser")
                .totalItems(10)
                .createdAt(LocalDateTime.now())
                .build();

        testMember = new Member();
        testMember.setId("member123");
        testMember.setName("John Doe");
        testMember.setEmail("john@example.com");
        testMember.setPhoneNumber("+1234567890");
    }

    // ========== createJob() Tests ==========

    @Test
    @DisplayName("Should create a new job successfully")
    void shouldCreateNewJobSuccessfully() {
        // Arrange
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // Act
        Job createdJob = jobService.createJob(Job.JobType.BULK_DELETE, "user123", "testuser", 10);

        // Assert
        assertNotNull(createdJob);
        assertEquals("job123", createdJob.getId());
        assertEquals(Job.JobType.BULK_DELETE, createdJob.getType());
        assertEquals(Job.JobStatus.PENDING, createdJob.getStatus());
        assertEquals("user123", createdJob.getUserId());
        assertEquals("testuser", createdJob.getUsername());
        assertEquals(10, createdJob.getTotalItems());
        assertNotNull(createdJob.getCreatedAt());
        
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    @DisplayName("Should create Excel upload job")
    void shouldCreateExcelUploadJob() {
        // Arrange
        Job excelJob = Job.builder()
                .id("job456")
                .type(Job.JobType.EXCEL_UPLOAD)
                .status(Job.JobStatus.PENDING)
                .userId("user123")
                .username("testuser")
                .totalItems(50)
                .createdAt(LocalDateTime.now())
                .build();
        when(jobRepository.save(any(Job.class))).thenReturn(excelJob);

        // Act
        Job createdJob = jobService.createJob(Job.JobType.EXCEL_UPLOAD, "user123", "testuser", 50);

        // Assert
        assertNotNull(createdJob);
        assertEquals(Job.JobType.EXCEL_UPLOAD, createdJob.getType());
        assertEquals(50, createdJob.getTotalItems());
    }

    // ========== getJobById() Tests ==========

    @Test
    @DisplayName("Should get job by ID when job exists")
    void shouldGetJobByIdWhenJobExists() {
        // Arrange
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));

        // Act
        Optional<Job> result = jobService.getJobById("job123");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("job123", result.get().getId());
        verify(jobRepository).findById("job123");
    }

    @Test
    @DisplayName("Should return empty when job ID not found")
    void shouldReturnEmptyWhenJobIdNotFound() {
        // Arrange
        when(jobRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<Job> result = jobService.getJobById("nonexistent");

        // Assert
        assertTrue(result.isEmpty());
        verify(jobRepository).findById("nonexistent");
    }

    // ========== getJobsByUserId() Tests ==========

    @Test
    @DisplayName("Should get all jobs for a user")
    void shouldGetAllJobsForUser() {
        // Arrange
        Job job1 = Job.builder().id("job1").userId("user123").build();
        Job job2 = Job.builder().id("job2").userId("user123").build();
        List<Job> jobs = List.of(job1, job2);
        when(jobRepository.findByUserIdOrderByCreatedAtDesc("user123")).thenReturn(jobs);

        // Act
        List<Job> result = jobService.getJobsByUserId("user123");

        // Assert
        assertEquals(2, result.size());
        assertEquals("job1", result.get(0).getId());
        assertEquals("job2", result.get(1).getId());
        verify(jobRepository).findByUserIdOrderByCreatedAtDesc("user123");
    }

    @Test
    @DisplayName("Should return empty list when user has no jobs")
    void shouldReturnEmptyListWhenUserHasNoJobs() {
        // Arrange
        when(jobRepository.findByUserIdOrderByCreatedAtDesc("user123")).thenReturn(List.of());

        // Act
        List<Job> result = jobService.getJobsByUserId("user123");

        // Assert
        assertTrue(result.isEmpty());
    }

    // ========== getActiveJobsByUserId() Tests ==========

    @Test
    @DisplayName("Should get active jobs for a user")
    void shouldGetActiveJobsForUser() {
        // Arrange
        Job pendingJob = Job.builder().id("job1").userId("user123").status(Job.JobStatus.PENDING).build();
        Job inProgressJob = Job.builder().id("job2").userId("user123").status(Job.JobStatus.IN_PROGRESS).build();
        List<Job> activeJobs = List.of(pendingJob, inProgressJob);
        
        List<Job.JobStatus> activeStatuses = List.of(Job.JobStatus.PENDING, Job.JobStatus.IN_PROGRESS);
        when(jobRepository.findByUserIdAndStatusInOrderByCreatedAtDesc("user123", activeStatuses))
                .thenReturn(activeJobs);

        // Act
        List<Job> result = jobService.getActiveJobsByUserId("user123");

        // Assert
        assertEquals(2, result.size());
        assertEquals(Job.JobStatus.PENDING, result.get(0).getStatus());
        assertEquals(Job.JobStatus.IN_PROGRESS, result.get(1).getStatus());
    }

    @Test
    @DisplayName("Should not return completed jobs in active jobs list")
    void shouldNotReturnCompletedJobsInActiveJobsList() {
        // Arrange
        when(jobRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(anyString(), anyList()))
                .thenReturn(List.of()); // No active jobs

        // Act
        List<Job> result = jobService.getActiveJobsByUserId("user123");

        // Assert
        assertTrue(result.isEmpty());
    }

    // ========== cancelJob() Tests ==========

    @Test
    @DisplayName("Should cancel a pending job successfully")
    void shouldCancelPendingJobSuccessfully() {
        // Arrange
        testJob.setStatus(Job.JobStatus.PENDING);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // Act
        boolean result = jobService.cancelJob("job123", "user123");

        // Assert
        assertTrue(result);
        assertEquals(Job.JobStatus.CANCELLED, testJob.getStatus());
        assertNotNull(testJob.getCompletedAt());
        verify(jobRepository).save(testJob);
    }

    @Test
    @DisplayName("Should cancel an in-progress job successfully")
    void shouldCancelInProgressJobSuccessfully() {
        // Arrange
        testJob.setStatus(Job.JobStatus.IN_PROGRESS);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // Act
        boolean result = jobService.cancelJob("job123", "user123");

        // Assert
        assertTrue(result);
        assertEquals(Job.JobStatus.CANCELLED, testJob.getStatus());
    }

    @Test
    @DisplayName("Should fail to cancel when job not found")
    void shouldFailToCancelWhenJobNotFound() {
        // Arrange
        when(jobRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        boolean result = jobService.cancelJob("nonexistent", "user123");

        // Assert
        assertFalse(result);
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to cancel when user doesn't own the job")
    void shouldFailToCancelWhenUserDoesNotOwnJob() {
        // Arrange
        testJob.setUserId("otherUser");
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));

        // Act
        boolean result = jobService.cancelJob("job123", "user123");

        // Assert
        assertFalse(result);
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to cancel a completed job")
    void shouldFailToCancelCompletedJob() {
        // Arrange
        testJob.setStatus(Job.JobStatus.COMPLETED);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));

        // Act
        boolean result = jobService.cancelJob("job123", "user123");

        // Assert
        assertFalse(result);
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to cancel a failed job")
    void shouldFailToCancelFailedJob() {
        // Arrange
        testJob.setStatus(Job.JobStatus.FAILED);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));

        // Act
        boolean result = jobService.cancelJob("job123", "user123");

        // Assert
        assertFalse(result);
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to cancel an already cancelled job")
    void shouldFailToCancelAlreadyCancelledJob() {
        // Arrange
        testJob.setStatus(Job.JobStatus.CANCELLED);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));

        // Act
        boolean result = jobService.cancelJob("job123", "user123");

        // Assert
        assertFalse(result);
        verify(jobRepository, never()).save(any());
    }

    // ========== deleteJob() Tests ==========

    @Test
    @DisplayName("Should delete a completed job successfully")
    void shouldDeleteCompletedJobSuccessfully() {
        // Arrange
        testJob.setStatus(Job.JobStatus.COMPLETED);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));

        // Act
        boolean result = jobService.deleteJob("job123", "user123");

        // Assert
        assertTrue(result);
        verify(jobRepository).deleteById("job123");
    }

    @Test
    @DisplayName("Should delete a failed job successfully")
    void shouldDeleteFailedJobSuccessfully() {
        // Arrange
        testJob.setStatus(Job.JobStatus.FAILED);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));

        // Act
        boolean result = jobService.deleteJob("job123", "user123");

        // Assert
        assertTrue(result);
        verify(jobRepository).deleteById("job123");
    }

    @Test
    @DisplayName("Should delete a cancelled job successfully")
    void shouldDeleteCancelledJobSuccessfully() {
        // Arrange
        testJob.setStatus(Job.JobStatus.CANCELLED);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));

        // Act
        boolean result = jobService.deleteJob("job123", "user123");

        // Assert
        assertTrue(result);
        verify(jobRepository).deleteById("job123");
    }

    @Test
    @DisplayName("Should fail to delete when job not found")
    void shouldFailToDeleteWhenJobNotFound() {
        // Arrange
        when(jobRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        boolean result = jobService.deleteJob("nonexistent", "user123");

        // Assert
        assertFalse(result);
        verify(jobRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should fail to delete when user doesn't own the job")
    void shouldFailToDeleteWhenUserDoesNotOwnJob() {
        // Arrange
        testJob.setUserId("otherUser");
        testJob.setStatus(Job.JobStatus.COMPLETED);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));

        // Act
        boolean result = jobService.deleteJob("job123", "user123");

        // Assert
        assertFalse(result);
        verify(jobRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should fail to delete a pending job")
    void shouldFailToDeletePendingJob() {
        // Arrange
        testJob.setStatus(Job.JobStatus.PENDING);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));

        // Act
        boolean result = jobService.deleteJob("job123", "user123");

        // Assert
        assertFalse(result);
        verify(jobRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should fail to delete an in-progress job")
    void shouldFailToDeleteInProgressJob() {
        // Arrange
        testJob.setStatus(Job.JobStatus.IN_PROGRESS);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(testJob));

        // Act
        boolean result = jobService.deleteJob("job123", "user123");

        // Assert
        assertFalse(result);
        verify(jobRepository, never()).deleteById(any());
    }

    // ========== cleanupOldJobs() Tests ==========

    @Test
    @DisplayName("Should cleanup old completed jobs")
    void shouldCleanupOldCompletedJobs() {
        // Arrange
        LocalDateTime oldDate = LocalDateTime.now().minusDays(10);
        Job oldCompletedJob1 = Job.builder()
                .id("job1")
                .status(Job.JobStatus.COMPLETED)
                .createdAt(oldDate)
                .build();
        Job oldCompletedJob2 = Job.builder()
                .id("job2")
                .status(Job.JobStatus.COMPLETED)
                .createdAt(oldDate)
                .build();
        
        List<Job> oldJobs = List.of(oldCompletedJob1, oldCompletedJob2);
        when(jobRepository.findByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(oldJobs);

        // Act
        jobService.cleanupOldJobs();

        // Assert
        verify(jobRepository).deleteById("job1");
        verify(jobRepository).deleteById("job2");
    }

    @Test
    @DisplayName("Should cleanup old failed jobs")
    void shouldCleanupOldFailedJobs() {
        // Arrange
        LocalDateTime oldDate = LocalDateTime.now().minusDays(10);
        Job oldFailedJob = Job.builder()
                .id("job1")
                .status(Job.JobStatus.FAILED)
                .createdAt(oldDate)
                .build();
        
        when(jobRepository.findByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(List.of(oldFailedJob));

        // Act
        jobService.cleanupOldJobs();

        // Assert
        verify(jobRepository).deleteById("job1");
    }

    @Test
    @DisplayName("Should cleanup old cancelled jobs")
    void shouldCleanupOldCancelledJobs() {
        // Arrange
        LocalDateTime oldDate = LocalDateTime.now().minusDays(10);
        Job oldCancelledJob = Job.builder()
                .id("job1")
                .status(Job.JobStatus.CANCELLED)
                .createdAt(oldDate)
                .build();
        
        when(jobRepository.findByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(List.of(oldCancelledJob));

        // Act
        jobService.cleanupOldJobs();

        // Assert
        verify(jobRepository).deleteById("job1");
    }

    @Test
    @DisplayName("Should not cleanup old pending jobs")
    void shouldNotCleanupOldPendingJobs() {
        // Arrange
        LocalDateTime oldDate = LocalDateTime.now().minusDays(10);
        Job oldPendingJob = Job.builder()
                .id("job1")
                .status(Job.JobStatus.PENDING)
                .createdAt(oldDate)
                .build();
        
        when(jobRepository.findByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(List.of(oldPendingJob));

        // Act
        jobService.cleanupOldJobs();

        // Assert
        verify(jobRepository, never()).deleteById("job1");
    }

    @Test
    @DisplayName("Should not cleanup old in-progress jobs")
    void shouldNotCleanupOldInProgressJobs() {
        // Arrange
        LocalDateTime oldDate = LocalDateTime.now().minusDays(10);
        Job oldInProgressJob = Job.builder()
                .id("job1")
                .status(Job.JobStatus.IN_PROGRESS)
                .createdAt(oldDate)
                .build();
        
        when(jobRepository.findByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(List.of(oldInProgressJob));

        // Act
        jobService.cleanupOldJobs();

        // Assert
        verify(jobRepository, never()).deleteById("job1");
    }

    @Test
    @DisplayName("Should handle empty list during cleanup")
    void shouldHandleEmptyListDuringCleanup() {
        // Arrange
        when(jobRepository.findByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(List.of());

        // Act
        jobService.cleanupOldJobs();

        // Assert
        verify(jobRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should cleanup mixed status old jobs correctly")
    void shouldCleanupMixedStatusOldJobsCorrectly() {
        // Arrange
        LocalDateTime oldDate = LocalDateTime.now().minusDays(10);
        Job completedJob = Job.builder().id("job1").status(Job.JobStatus.COMPLETED).createdAt(oldDate).build();
        Job pendingJob = Job.builder().id("job2").status(Job.JobStatus.PENDING).createdAt(oldDate).build();
        Job failedJob = Job.builder().id("job3").status(Job.JobStatus.FAILED).createdAt(oldDate).build();
        Job inProgressJob = Job.builder().id("job4").status(Job.JobStatus.IN_PROGRESS).createdAt(oldDate).build();
        
        List<Job> oldJobs = List.of(completedJob, pendingJob, failedJob, inProgressJob);
        when(jobRepository.findByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(oldJobs);

        // Act
        jobService.cleanupOldJobs();

        // Assert
        verify(jobRepository).deleteById("job1"); // Completed - deleted
        verify(jobRepository, never()).deleteById("job2"); // Pending - kept
        verify(jobRepository).deleteById("job3"); // Failed - deleted
        verify(jobRepository, never()).deleteById("job4"); // In-progress - kept
    }

    // ========== processBulkDelete() Tests ==========

    @Test
    @DisplayName("Should handle job not found in processBulkDelete")
    void shouldHandleJobNotFoundInProcessBulkDelete() {
        // Arrange
        when(jobRepository.findById("job123")).thenReturn(Optional.empty());

        // Act
        jobService.processBulkDelete("job123", List.of("member1", "member2"));

        // Assert - Should log error and return without throwing exception
        verify(jobRepository).findById("job123");
        verify(memberRepository, never()).findById(any());
    }

    // ========== processExcelUpload() Tests ==========

    @Test
    @DisplayName("Should handle job not found in processExcelUpload")
    void shouldHandleJobNotFoundInProcessExcelUpload() {
        // Arrange
        when(jobRepository.findById("job123")).thenReturn(Optional.empty());
        byte[] emptyBytes = new byte[0];

        // Act
        jobService.processExcelUpload("job123", emptyBytes, "test.xlsx");

        // Assert - Should log error and return without throwing exception
        verify(jobRepository).findById("job123");
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("Should handle null user ID in createJob")
    void shouldHandleNullUserIdInCreateJob() {
        // Arrange
        Job jobWithNullUserId = Job.builder()
                .id("job123")
                .type(Job.JobType.BULK_DELETE)
                .status(Job.JobStatus.PENDING)
                .userId(null) // Null userId
                .username("testuser")
                .totalItems(10)
                .createdAt(LocalDateTime.now())
                .build();
        when(jobRepository.save(any(Job.class))).thenReturn(jobWithNullUserId);

        // Act
        Job createdJob = jobService.createJob(Job.JobType.BULK_DELETE, null, "testuser", 10);

        // Assert
        assertNotNull(createdJob);
        assertNull(createdJob.getUserId());
    }

    @Test
    @DisplayName("Should handle zero total items in createJob")
    void shouldHandleZeroTotalItemsInCreateJob() {
        // Arrange
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // Act
        Job createdJob = jobService.createJob(Job.JobType.BULK_DELETE, "user123", "testuser", 0);

        // Assert
        assertNotNull(createdJob);
        // Zero items is valid (edge case: empty bulk operation)
    }

    @Test
    @DisplayName("Should handle negative total items in createJob")
    void shouldHandleNegativeTotalItemsInCreateJob() {
        // Arrange
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // Act
        Job createdJob = jobService.createJob(Job.JobType.BULK_DELETE, "user123", "testuser", -5);

        // Assert
        assertNotNull(createdJob);
        // Negative items is technically allowed by the method (validation should be at controller level)
    }

    // ========== processBulkDelete() Full Coverage Tests ==========

    @Test
    @DisplayName("Should process bulk delete successfully with valid members")
    void shouldProcessBulkDeleteSuccessfullyWithValidMembers() throws InterruptedException {
        // Arrange
        String jobId = "job123";
        String memberId1 = "member1";
        String memberId2 = "member2";
        List<String> memberIds = List.of(memberId1, memberId2);

        Member member1 = new Member();
        member1.setId(memberId1);
        member1.setEmail("member1@test.com");

        Member member2 = new Member();
        member2.setId(memberId2);
        member2.setEmail("member2@test.com");

        testJob.setStatus(Job.JobStatus.PENDING);
        
        when(jobRepository.findById(jobId))
            .thenReturn(Optional.of(testJob))
            .thenReturn(Optional.of(testJob))
            .thenReturn(Optional.of(testJob))
            .thenReturn(Optional.of(testJob));
        when(memberRepository.findById(memberId1)).thenReturn(Optional.of(member1));
        when(memberRepository.findById(memberId2)).thenReturn(Optional.of(member2));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // Act
        jobService.processBulkDelete(jobId, memberIds);

        // Assert
        verify(memberRepository).deleteById(memberId1);
        verify(memberRepository).deleteById(memberId2);
        verify(jobRepository, atLeast(2)).save(any(Job.class));
    }

    @Test
    @DisplayName("Should handle member not found during bulk delete")
    void shouldHandleMemberNotFoundDuringBulkDelete() {
        // Arrange
        String jobId = "job123";
        List<String> memberIds = List.of("nonexistent");

        testJob.setStatus(Job.JobStatus.PENDING);
        
        when(jobRepository.findById(jobId))
            .thenReturn(Optional.of(testJob))
            .thenReturn(Optional.of(testJob))
            .thenReturn(Optional.of(testJob));
        when(memberRepository.findById("nonexistent")).thenReturn(Optional.empty());
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // Act
        jobService.processBulkDelete(jobId, memberIds);

        // Assert
        verify(memberRepository, never()).deleteById(any());
        verify(jobRepository, atLeast(2)).save(any(Job.class));
    }

    @Test
    @DisplayName("Should handle exception during member deletion in bulk delete")
    void shouldHandleExceptionDuringMemberDeletionInBulkDelete() {
        // Arrange
        String jobId = "job123";
        String memberId = "member1";
        List<String> memberIds = List.of(memberId);

        Member member = new Member();
        member.setId(memberId);
        member.setEmail("member1@test.com");

        testJob.setStatus(Job.JobStatus.PENDING);
        
        when(jobRepository.findById(jobId))
            .thenReturn(Optional.of(testJob))
            .thenReturn(Optional.of(testJob))
            .thenReturn(Optional.of(testJob));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        doThrow(new RuntimeException("Database error")).when(memberRepository).deleteById(memberId);
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // Act
        jobService.processBulkDelete(jobId, memberIds);

        // Assert
        verify(memberRepository).deleteById(memberId);
        verify(jobRepository, atLeast(2)).save(any(Job.class));
    }

    @Test
    @DisplayName("Should stop bulk delete when job is cancelled")
    void shouldStopBulkDeleteWhenJobIsCancelled() {
        // Arrange
        String jobId = "job123";
        List<String> memberIds = List.of("member1", "member2", "member3");

        testJob.setStatus(Job.JobStatus.PENDING);
        Job cancelledJob = Job.builder()
            .id(jobId)
            .status(Job.JobStatus.CANCELLED)
            .build();
        
        when(jobRepository.findById(jobId))
            .thenReturn(Optional.of(testJob))  // First call - start job
            .thenReturn(Optional.of(cancelledJob));  // Second call - job cancelled
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // Act
        jobService.processBulkDelete(jobId, memberIds);

        // Assert
        verify(memberRepository, never()).deleteById(any());
    }

    // ========== processExcelUpload() Coverage Tests ==========

    @Test
    @DisplayName("Should handle invalid Excel file in processExcelUpload")
    void shouldHandleInvalidExcelFileInProcessExcelUpload() {
        // Arrange
        String jobId = "job123";
        byte[] invalidBytes = "invalid excel data".getBytes();
        
        testJob.setStatus(Job.JobStatus.PENDING);
        when(jobRepository.findById(jobId))
            .thenReturn(Optional.of(testJob))
            .thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // Act
        jobService.processExcelUpload(jobId, invalidBytes, "test.xlsx");

        // Assert
        verify(jobRepository, atLeast(2)).save(any(Job.class));
    }

    @Test
    @DisplayName("Should handle empty Excel file in processExcelUpload")
    void shouldHandleEmptyExcelFileInProcessExcelUpload() {
        // Arrange
        String jobId = "job123";
        byte[] emptyBytes = new byte[0];
        
        testJob.setStatus(Job.JobStatus.PENDING);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // Act
        jobService.processExcelUpload(jobId, emptyBytes, "test.xlsx");

        // Assert
        verify(jobRepository, atLeast(1)).save(any(Job.class));
    }
}

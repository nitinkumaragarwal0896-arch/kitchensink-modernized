package com.modernizedkitechensink.kitchensinkmodernized.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous task execution with SEPARATE thread pools.
 *
 * WHY SEPARATE THREAD POOLS?
 * ===========================
 * 
 * PROBLEM: Shared thread pool causes "thread starvation"
 * - Example: Bulk delete uses all 5 threads for 30 seconds
 * - Meanwhile: User requests password reset
 * - Result: Email rejected! User doesn't receive reset link ❌
 * 
 * SOLUTION: Dedicated thread pools per concern
 * - Email operations: Always have threads available (critical)
 * - Bulk operations: Can queue/slow down (not user-facing)
 * - Audit logging: Best-effort, can drop if overwhelmed (non-critical)
 * 
 * THREAD POOL SIZING LOGIC:
 * ==========================
 * 
 * Formula for I/O-bound tasks (network calls, DB queries):
 *   Core Pool Size = CPU Cores × (1 + Wait Time / CPU Time)
 * 
 * For CPU-bound tasks (data processing, encryption):
 *   Core Pool Size = CPU Cores + 1
 * 
 * Real-world: Start conservative, monitor, then tune.
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfig {

  /**
   * Thread pool for EMAIL operations (password reset, notifications, etc.).
   * 
   * CRITICAL: Emails are user-facing and time-sensitive.
   * If email fails, user experience breaks (can't login, can't reset password).
   * 
   * SIZING RATIONALE:
   * - Core: 2 threads (handles normal load: ~10 emails/minute)
   * - Max: 10 threads (burst capacity for peak times: ~50 emails/minute)
   * - Queue: 200 (2000 emails can wait ~40 minutes before rejection)
   * 
   * REJECTION POLICY: CallerRunsPolicy
   * - If queue full, controller thread sends email (slow but doesn't fail)
   * - Better to delay response than lose password reset email
   * 
   * MONITORING:
   * - Alert if queueSize > 150 (approaching capacity)
   * - Alert if activeThreads == maxPoolSize (all threads busy)
   */
  @Bean(name = "emailTaskExecutor")
  public Executor emailTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    
    // Thread pool sizing
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(200);
    
    // Thread naming (useful for debugging logs)
    executor.setThreadNamePrefix("Email-");
    
    // Rejection policy: Run in caller thread if queue full
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    
    // Allow core threads to timeout after 60 seconds of inactivity
    executor.setAllowCoreThreadTimeOut(true);
    executor.setKeepAliveSeconds(60);
    
    // Graceful shutdown: wait 30 seconds for tasks to complete
    executor.setAwaitTerminationSeconds(30);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    
    executor.initialize();
    
    log.info("✅ Email thread pool initialized: core={}, max={}, queue={}",
      executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
    
    return executor;
  }

  /**
   * Thread pool for BULK operations (bulk delete, Excel upload, etc.).
   * 
   * CHARACTERISTICS: Heavy, long-running, resource-intensive.
   * These tasks can take 30 seconds to several minutes.
   * They process hundreds/thousands of records.
   * 
   * SIZING RATIONALE:
   * - Core: 2 threads (2 concurrent bulk jobs is reasonable)
   * - Max: 4 threads (limit to prevent DB/CPU overload)
   * - Queue: 20 (small queue - bulk jobs are big, don't want too many queued)
   * 
   * REJECTION POLICY: AbortPolicy
   * - If queue full, reject immediately (fail fast)
   * - User gets error: "Too many bulk operations, try again later"
   * - Prevents system overload
   * 
   * TRADE-OFF:
   * - Could use CallerRunsPolicy, but that would block controller for 30+ seconds
   * - Better to reject and ask user to retry than freeze the request
   */
  @Bean(name = "bulkTaskExecutor")
  public Executor bulkTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    
    // Conservative sizing (bulk operations are resource-heavy)
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(20);
    
    executor.setThreadNamePrefix("Bulk-");
    
    // Rejection policy: Fail fast (throw exception)
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    
    // Keep threads alive longer (bulk jobs may be periodic)
    executor.setKeepAliveSeconds(120);
    
    // Graceful shutdown: wait up to 5 minutes for bulk jobs to complete
    executor.setAwaitTerminationSeconds(300);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    
    executor.initialize();
    
    log.info("✅ Bulk operations thread pool initialized: core={}, max={}, queue={}",
      executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
    
    return executor;
  }

  /**
   * Thread pool for AUDIT logging.
   * 
   * NON-CRITICAL: Audit logs are important but not critical for user experience.
   * If audit log fails, the main operation (create member, delete user) still succeeds.
   * 
   * SIZING RATIONALE:
   * - Core: 1 thread (single thread is enough for logging)
   * - Max: 2 threads (minimal burst capacity)
   * - Queue: 500 (large queue - audit is async, can afford to wait)
   * 
   * REJECTION POLICY: DiscardPolicy
   * - If queue full, silently discard audit log
   * - Controversial but pragmatic: better to lose audit than fail user operation
   * - In production, consider DiscardOldestPolicy (keep recent audits)
   * 
   * ALTERNATIVE:
   * - For compliance-heavy apps (finance, healthcare), use CallerRunsPolicy
   * - Or use a message queue (Kafka, RabbitMQ) for guaranteed delivery
   */
  @Bean(name = "auditTaskExecutor")
  public Executor auditTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    
    // Minimal threads (logging is lightweight)
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(500);
    
    executor.setThreadNamePrefix("Audit-");
    
    // Rejection policy: Silently discard (audit is best-effort)
    executor.setRejectedExecutionHandler(new LogAndDiscardPolicy());
    
    // Allow core thread to timeout (save resources during idle)
    executor.setAllowCoreThreadTimeOut(true);
    executor.setKeepAliveSeconds(60);
    
    // Quick shutdown (audit logs are not critical)
    executor.setAwaitTerminationSeconds(10);
    executor.setWaitForTasksToCompleteOnShutdown(false);
    
    executor.initialize();
    
    log.info("✅ Audit logging thread pool initialized: core={}, max={}, queue={}",
      executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
    
    return executor;
  }

  /**
   * Custom rejection handler that logs discarded audit tasks.
   * 
   * Standard DiscardPolicy silently drops tasks, which is hard to debug.
   * This variant logs a warning so we know audit logs are being lost.
   */
  private static class LogAndDiscardPolicy implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      log.warn("⚠️ Audit task DISCARDED (queue full): pool={}, active={}, queue={}", 
        executor.getPoolSize(), 
        executor.getActiveCount(), 
        executor.getQueue().size());
    }
  }
}
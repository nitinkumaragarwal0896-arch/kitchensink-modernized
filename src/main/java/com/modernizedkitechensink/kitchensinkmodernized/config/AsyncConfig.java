package com.modernizedkitechensink.kitchensinkmodernized.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution.
 *
 * Enables @Async annotation and configures the thread pool
 * used for background tasks like audit logging.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

  /**
   * Custom thread pool for async tasks.
   *
   * Without this, Spring uses SimpleAsyncTaskExecutor
   * which creates a new thread for each task (not efficient).
   */
  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    // Core threads - always kept alive
    executor.setCorePoolSize(2);

    // Max threads - created when queue is full
    executor.setMaxPoolSize(5);

    // Queue capacity - tasks wait here before new threads created
    executor.setQueueCapacity(100);

    // Thread naming - useful for debugging
    executor.setThreadNamePrefix("Async-");

    executor.initialize();
    return executor;
  }
}
package com.modernizedkitechensink.kitchensinkmodernized.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration using Caffeine.
 *
 * Caffeine is a high-performance Java caching library.
 * Used to reduce database queries for frequently accessed data.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /**
   * Configure the cache manager with named caches.
   */
  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();

    cacheManager.setCaffeine(Caffeine.newBuilder()
      // Expire entries 5 minutes after last write
      .expireAfterWrite(5, TimeUnit.MINUTES)
      // Maximum 1000 entries in cache
      .maximumSize(1000)
      // Record stats for monitoring
      .recordStats()
    );

    // Register named caches
    cacheManager.setCacheNames(java.util.List.of(
      "members",       // Cache for member queries
      "users",         // Cache for user lookups
      "roles"          // Cache for role lookups
    ));

    return cacheManager;
  }
}
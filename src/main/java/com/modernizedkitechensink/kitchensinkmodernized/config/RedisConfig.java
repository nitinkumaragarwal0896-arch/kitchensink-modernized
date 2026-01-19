package com.modernizedkitechensink.kitchensinkmodernized.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Configuration for Distributed Caching and Session Management.
 *
 * This configuration sets up:
 * 1. Redis Template for manual cache operations (token blacklist, sessions)
 * 2. Cache Manager for @Cacheable annotations (member list, etc.)
 * 3. JSON serialization for storing Java objects in Redis
 *
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Configuration
@EnableCaching  // Enables Spring's @Cacheable, @CacheEvict, @CachePut annotations
public class RedisConfig {

  /**
   * Configure RedisTemplate for manual cache operations.
   *
   * RedisTemplate is used for:
   * - Token blacklist (instant logout)
   * - Refresh token storage (session management)
   * - Rate limiting counters
   * - Any custom Redis operations
   *
   * Why We Need This:
   * - Spring Boot's auto-configured RedisTemplate uses JDK serialization (binary)
   * - We want JSON serialization (human-readable, cross-platform)
   *
   * @param connectionFactory Auto-configured by Spring Boot from application.properties
   * @return Configured RedisTemplate with JSON serialization
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Create ObjectMapper for JSON serialization
    ObjectMapper objectMapper = new ObjectMapper();

    // Register JavaTimeModule for Java 8 date/time support (LocalDateTime, etc.)
    objectMapper.registerModule(new JavaTimeModule());

    // Enable type information in JSON (so we can deserialize back to correct Java class)
    // Without this, Redis would store generic LinkedHashMap instead of our POJOs
    objectMapper.activateDefaultTyping(
      LaissezFaireSubTypeValidator.instance,
      ObjectMapper.DefaultTyping.NON_FINAL,
      JsonTypeInfo.As.PROPERTY
    );

    // Create JSON serializer using our ObjectMapper
    GenericJackson2JsonRedisSerializer jsonSerializer =
      new GenericJackson2JsonRedisSerializer(objectMapper);

    // Use String serializer for keys (simple, fast)
    StringRedisSerializer stringSerializer = new StringRedisSerializer();

    // Configure serializers for different Redis data types
    template.setKeySerializer(stringSerializer);              // Keys as plain strings
    template.setValueSerializer(jsonSerializer);              // Values as JSON
    template.setHashKeySerializer(stringSerializer);          // Hash keys as strings
    template.setHashValueSerializer(jsonSerializer);          // Hash values as JSON

    template.afterPropertiesSet();
    return template;
  }

  /**
   * Configure CacheManager for @Cacheable annotations.
   *
   * This is used by Spring's caching abstraction:
   * - @Cacheable("members") on getAllMembers()
   * - @CacheEvict("members") on createMember()
   * - @CachePut("members") on updateMember()
   *
   * Why We Need This:
   * - Spring's @Cacheable needs a CacheManager to know HOW to cache
   * - We're replacing the old CaffeineCacheManager with RedisCacheManager
   * - Enables distributed caching across multiple backend instances
   *
   * @param connectionFactory Auto-configured by Spring Boot
   * @return Configured RedisCacheManager with 5-minute TTL
   */
  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    // Create ObjectMapper (same as above)
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.activateDefaultTyping(
      LaissezFaireSubTypeValidator.instance,
      ObjectMapper.DefaultTyping.NON_FINAL,
      JsonTypeInfo.As.PROPERTY
    );

    // Create JSON serializer
    GenericJackson2JsonRedisSerializer jsonSerializer =
      new GenericJackson2JsonRedisSerializer(objectMapper);

    // Configure cache behavior
    RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofMinutes(5))  // Cache TTL = 5 minutes (same as old Caffeine config)
      .disableCachingNullValues()        // Don't cache null results (avoids memory waste)
      .serializeKeysWith(                // Keys as strings
        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
      )
      .serializeValuesWith(              // Values as JSON
        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
      );

    // Build and return CacheManager
    return RedisCacheManager.builder(connectionFactory)
      .cacheDefaults(cacheConfig)
      .build();
  }
}
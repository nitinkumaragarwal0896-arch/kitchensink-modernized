package com.modernizedkitechensink.kitchensinkmodernized.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter - Prevents API abuse.
 *
 * Uses Token Bucket algorithm (via Bucket4j):
 * - Each client gets a "bucket" of tokens
 * - Each request consumes 1 token
 * - Tokens refill at a steady rate
 * - When bucket is empty → 429 Too Many Requests
 *
 * Limits: 100 requests per minute per IP address
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

  // Store buckets per client IP
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  // Rate limit configuration
  private static final int REQUESTS_PER_MINUTE = 100;
  private static final int BUCKET_CAPACITY = 100;

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {

    // Skip rate limiting for health checks
    if (request.getRequestURI().contains("/actuator")) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = getClientIp(request);
    Bucket bucket = buckets.computeIfAbsent(clientIp, this::createNewBucket);

    // Try to consume a token
    if (bucket.tryConsume(1)) {
      // Token available - proceed with request
      filterChain.doFilter(request, response);
    } else {
      // No tokens - rate limit exceeded
      log.warn("Rate limit exceeded for IP: {}", clientIp);

      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json");
      response.getWriter().write(
        "{\"error\": \"Rate limit exceeded. Please try again later.\"}"
      );
    }
  }

  /**
   * Creates a new token bucket for a client.
   *
   * Token Bucket Algorithm:
   * - Bucket starts full (100 tokens)
   * - Each request takes 1 token
   * - Tokens refill at 100 per minute (greedy refill)
   */
  private Bucket createNewBucket(String clientIp) {
    Bandwidth limit = Bandwidth.classic(
      BUCKET_CAPACITY,                              // Max tokens
      Refill.greedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))  // Refill rate
    );

    log.debug("Created rate limit bucket for IP: {}", clientIp);
    return Bucket.builder().addLimit(limit).build();
  }

  /**
   * Get client IP, handling proxies.
   */
  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
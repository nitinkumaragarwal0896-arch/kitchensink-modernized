package com.modernizedkitechensink.kitchensinkmodernized.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for creating audit log entries.
 *
 * Uses @Async to save logs in a background thread,
 * so audit logging doesn't slow down the main request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

  private final AuditLogRepository auditLogRepository;

  /**
   * Log a successful operation.
   */
  public void logSuccess(String action, String entityType, String entityId) {
    logEvent(action, entityType, entityId, "SUCCESS", null, null);
  }

  /**
   * Log a successful operation with additional details.
   */
  public void logSuccess(String action, String entityType, String entityId,
                         Map<String, Object> details) {
    logEvent(action, entityType, entityId, "SUCCESS", null, details);
  }

  /**
   * Log a failed operation.
   */
  public void logFailure(String action, String entityType, String entityId,
                         String errorMessage) {
    logEvent(action, entityType, entityId, "FAILURE", errorMessage, null);
  }

  /**
   * Core logging method - saves audit entry asynchronously.
   *
   * Runs on dedicated AUDIT thread pool (auditTaskExecutor).
   * This ensures audit logging doesn't compete with critical operations.
   * 
   * THREAD POOL: auditTaskExecutor (1 core, 2 max, 500 queue)
   * - Single thread is sufficient (logging is fast)
   * - Large queue (audit can afford to wait)
   * - DiscardPolicy: If overwhelmed, drops logs (non-critical)
   * 
   * @Async means this runs in a separate thread pool,
   * so the main request doesn't wait for database write.
   */
  @Async("auditTaskExecutor")
  public void logEvent(String action, String entityType, String entityId,
                       String status, String errorMessage, Map<String, Object> details) {
    try {
      AuditLog auditLog = AuditLog.builder()
        .action(action)
        .entityType(entityType)
        .entityId(entityId)
        .username(getCurrentUsername())
        .ipAddress(getClientIpAddress())
        .timestamp(LocalDateTime.now())
        .status(status)
        .errorMessage(errorMessage)
        .details(details)
        .build();

      auditLogRepository.save(auditLog);
      log.debug("Audit log saved: {} {} {} by {}",
        action, entityType, entityId, auditLog.getUsername());

    } catch (Exception e) {
      // Don't let audit failures affect the main operation
      log.error("Failed to save audit log: {}", e.getMessage());
    }
  }

  /**
   * Get current authenticated username from SecurityContext.
   */
  private String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      return authentication.getName();
    }
    return "anonymous";
  }

  /**
   * Get client IP address from the current request.
   * Handles proxy headers (X-Forwarded-For) for load-balanced environments.
   */
  private String getClientIpAddress() {
    try {
      ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();

        // Check for proxy headers first
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
          return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
      }
    } catch (Exception e) {
      log.debug("Could not get client IP: {}", e.getMessage());
    }
    return "unknown";
  }
}
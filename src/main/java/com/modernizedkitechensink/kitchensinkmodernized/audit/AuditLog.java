package com.modernizedkitechensink.kitchensinkmodernized.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Audit Log entity - records all significant system events.
 *
 * Stored in MongoDB "audit_logs" collection.
 *
 * Example document:
 * {
 *   "_id": "507f1f77bcf86cd799439011",
 *   "action": "CREATE",
 *   "entityType": "Member",
 *   "entityId": "123",
 *   "username": "admin",
 *   "timestamp": "2024-01-15T10:30:00",
 *   "status": "SUCCESS",
 *   "details": { "name": "John Doe", "email": "john@example.com" }
 * }
 */
@Document(collection = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

  @Id
  private String id;

  private String action;          // CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT
  private String entityType;      // Member, User, Role
  private String entityId;        // ID of the affected entity
  private String username;        // Who performed the action
  private String ipAddress;       // Client IP address
  private LocalDateTime timestamp;
  private String status;          // SUCCESS, FAILURE
  private String errorMessage;    // If status is FAILURE
  private Map<String, Object> details;  // Additional context (changes, etc.)
}

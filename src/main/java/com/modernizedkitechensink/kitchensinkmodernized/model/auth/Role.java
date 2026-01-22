package com.modernizedkitechensink.kitchensinkmodernized.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Role entity - represents a named collection of permissions.
 *
 * Stored in MongoDB "roles" collection.
 *
 * Example document in MongoDB:
 * {
 *   "_id": "507f1f77bcf86cd799439011",
 *   "name": "ADMIN",
 *   "description": "Full system access",
 *   "permissions": ["member:create", "member:read", "member:update", "member:delete", "system:admin"]
 * }
 */
@Document(collection = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

  @Id
  private String id;

  @Indexed(unique = true)
  private String name;  // e.g., "ADMIN", "USER", "VIEWER"

  private String description;  // Human-readable description

  /**
   * Permissions stored as strings (e.g., "member:read", "system:admin").
   * This avoids MongoDB conversion issues.
   */
  @Builder.Default
  private Set<String> permissions = new HashSet<>();

  @CreatedDate
  private LocalDateTime createdAt;

  /**
   * Get permissions as Permission enums (for Java code).
   */
  public Set<Permission> getPermissionsAsEnums() {
    Set<Permission> result = new HashSet<>();
    for (String p : permissions) {
      try {
        result.add(Permission.fromString(p));
      } catch (IllegalArgumentException e) {
        // Skip invalid permissions
      }
    }
    return result;
  }

  /**
   * Set permissions from Permission enums (converts to strings for storage).
   */
  public void setPermissionsFromEnums(Set<Permission> perms) {
    this.permissions = new HashSet<>();
    for (Permission p : perms) {
      this.permissions.add(p.getPermission());
    }
  }
}

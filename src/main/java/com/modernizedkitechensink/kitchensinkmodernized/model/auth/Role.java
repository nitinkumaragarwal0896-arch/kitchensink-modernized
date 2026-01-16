package com.modernizedkitechensink.kitchensinkmodernized.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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

  @Builder.Default
  private Set<Permission> permissions = new HashSet<>();

  /**
   * Convenience method to check if role has a specific permission.
   */
  public boolean hasPermission(Permission permission) {
    return permissions.contains(permission);
  }

  /**
   * Add a permission to this role.
   */
  public void addPermission(Permission permission) {
    permissions.add(permission);
  }

  /**
   * Remove a permission from this role.
   */
  public void removePermission(Permission permission) {
    permissions.remove(permission);
  }
}

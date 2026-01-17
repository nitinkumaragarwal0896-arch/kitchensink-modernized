package com.modernizedkitechensink.kitchensinkmodernized.model.auth;

/**
 * Granular permissions for Role-Based Access Control (RBAC).
 *
 * Naming convention: "resource:action"
 * - resource: what entity (member, user, role)
 * - action: what operation (create, read, update, delete)
 *
 * This pattern is used by Auth0, Okta, and other identity providers.
 */
public enum Permission {

  // ===== Member Permissions =====
  MEMBER_CREATE("member:create"),
  MEMBER_READ("member:read"),
  MEMBER_UPDATE("member:update"),
  MEMBER_DELETE("member:delete"),

  // ===== User Management Permissions (Admin only) =====
  USER_CREATE("user:create"),
  USER_READ("user:read"),
  USER_UPDATE("user:update"),
  USER_DELETE("user:delete"),

  // ===== Role Management Permissions (Admin only) =====
  ROLE_CREATE("role:create"),
  ROLE_READ("role:read"),
  ROLE_UPDATE("role:update"),
  ROLE_DELETE("role:delete"),

  // ===== System Permissions =====
  SYSTEM_ADMIN("system:admin");  // Super admin - can do everything

  private final String permission;

  Permission(String permission) {
    this.permission = permission;
  }

  /**
   * Returns the permission string (e.g., "member:read").
   * Used by Spring Security's @PreAuthorize annotations.
   */
  public String getPermission() {
    return permission;
  }

  /**
   * Convert permission string (e.g., "member:read") back to enum.
   * Used when creating/updating roles from API requests.
   */
  public static Permission fromString(String permissionString) {
    for (Permission p : Permission.values()) {
      if (p.permission.equals(permissionString)) {
        return p;
      }
    }
    throw new IllegalArgumentException("Unknown permission: " + permissionString);
  }
}
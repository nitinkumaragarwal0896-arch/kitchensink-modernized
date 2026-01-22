package com.modernizedkitechensink.kitchensinkmodernized.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity - represents a system user for authentication.
 */
@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  private String id;

  @Indexed(unique = true)
  private String username;

  @Indexed(unique = true)
  private String email;

  private String password;  // BCrypt hashed - NEVER plain text!

  private String firstName;
  
  private String lastName;
  
  private String phoneNumber;

  /**
   * User's roles - loaded from the "roles" collection.
   * @DBRef creates a reference (like a foreign key) instead of embedding.
   */
  @DBRef
  @Builder.Default
  private Set<Role> roles = new HashSet<>();

  // ===== Account Status Fields =====

  @Builder.Default
  private boolean enabled = true;  // Can be disabled by admin

  @Builder.Default
  private boolean accountNonLocked = true;  // Locked after failed attempts

  @Builder.Default
  private boolean accountNonExpired = true;  // For time-limited accounts

  @Builder.Default
  private boolean credentialsNonExpired = true;  // Force password reset

  // ===== Security Tracking =====

  @Builder.Default
  private int failedLoginAttempts = 0;  // Track for lockout

  private LocalDateTime lastLoginDate;

  private LocalDateTime lockoutEndTime;  // When lockout expires

  // ===== Audit Fields =====

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;

  /**
   * Increment failed login attempts and lock if threshold reached.
   */
  public void recordFailedLogin() {
    failedLoginAttempts++;
    if (failedLoginAttempts >= 5) {  // Lock after 5 failed attempts
      accountNonLocked = false;
      lockoutEndTime = LocalDateTime.now().plusMinutes(30);  // 30-minute lockout
    }
  }

  /**
   * Reset failed attempts on successful login.
   */
  public void recordSuccessfulLogin() {
    failedLoginAttempts = 0;
    accountNonLocked = true;
    lockoutEndTime = null;
    lastLoginDate = LocalDateTime.now();
  }
}

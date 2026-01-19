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

/**
 * RefreshToken entity - tracks active user sessions.
 *
 * Purpose:
 * - Enable "Active Sessions" management (view/revoke devices)
 * - Limit concurrent sessions per user
 * - Provide logout from all devices functionality
 * - Audit trail of user logins
 *
 * Example document in MongoDB:
 * {
 *   "_id": "507f1f77bcf86cd799439011",
 *   "tokenHash": "sha256hash...",  // Hashed token (never store plain!)
 *   "user": DBRef to User,
 *   "deviceInfo": "Chrome 120.0 on macOS",
 *   "ipAddress": "192.168.1.100",
 *   "issuedAt": ISODate("2026-01-17T10:00:00Z"),
 *   "expiresAt": ISODate("2026-01-24T10:00:00Z"),
 *   "lastUsedAt": ISODate("2026-01-17T12:30:00Z"),
 *   "revoked": false
 * }
 */
@Document(collection = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

  @Id
  private String id;

  /**
   * SHA-256 hash of the refresh token.
   * We NEVER store the actual token - only its hash.
   * This way, even if DB is compromised, tokens can't be stolen.
   */
  @Indexed
  private String tokenHash;

  /**
   * SHA-256 hash of the access token associated with this refresh token.
   * Used for instant revocation when session is deleted from another device.
   * Without this, revoked sessions can still make API calls for 15 minutes!
   */
  private String accessTokenHash;

  /**
   * User who owns this token.
   */
  @DBRef
  private User user;

  /**
   * Device information: Browser, OS, etc.
   * Extracted from User-Agent header.
   * Example: "Chrome 120.0 on macOS", "Safari on iPhone"
   */
  private String deviceInfo;

  /**
   * IP address from which login occurred.
   * Useful for security monitoring and fraud detection.
   */
  private String ipAddress;

  /**
   * When this token was created (login time).
   */
  @CreatedDate
  private LocalDateTime issuedAt;

  /**
   * When this token expires (7 days from issuance).
   */
  @Indexed(expireAfterSeconds = 0)  // MongoDB TTL index - auto-delete expired tokens
  private LocalDateTime expiresAt;

  /**
   * Last time this token was used to refresh access token.
   * Updated on each /refresh call.
   */
  @LastModifiedDate
  private LocalDateTime lastUsedAt;

  /**
   * Whether this token has been revoked.
   * True = user logged out or session was terminated
   */
  @Builder.Default
  private boolean revoked = false;

  /**
   * Check if this token is still valid.
   */
  public boolean isValid() {
    return !revoked && expiresAt.isAfter(LocalDateTime.now());
  }

  /**
   * Revoke this token (logout).
   */
  public void revoke() {
    this.revoked = true;
  }
}


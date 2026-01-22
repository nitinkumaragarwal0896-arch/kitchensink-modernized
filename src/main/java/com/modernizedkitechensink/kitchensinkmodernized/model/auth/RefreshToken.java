package com.modernizedkitechensink.kitchensinkmodernized.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * RefreshToken entity - tracks active user sessions.
 *
 * PURPOSE:
 * - Enable "Active Sessions" management (view/revoke devices)
 * - Limit concurrent sessions per user
 * - Provide logout from all devices functionality
 * - Audit trail of user logins
 *
 * DESIGN DECISION: Why userId (String) instead of @DBRef User?
 * 
 * ✅ More Control: Fetch User only when needed (not all queries need User object)
 * ✅ MongoDB-Friendly: No expensive $lookup or multiple round-trips
 * ✅ Explicit: No Spring Data "magic" - clear when User is fetched
 * ✅ Performance: Many operations (blacklist, cleanup) only need userId
 * ✅ Microservices-Ready: userId can reference a User in another service
 * 
 * Example Operations:
 * - Blacklist all tokens: Only need userId (no User fetch!)
 * - Cleanup expired: Only need expiresAt (no User fetch!)
 * - Show sessions UI: Fetch User once, then iterate tokens
 *
 * Example document in MongoDB:
 * {
 *   "_id": "507f1f77bcf86cd799439011",
 *   "tokenHash": "UFi6XgXd...",  // SHA-256 hash (never store plain!)
 *   "accessTokenHash": "e+8rivo0...",  // For instant revocation
 *   "userId": "696b2545bf7f4dc4514ec4e8",  // ← Just the ID!
 *   "deviceInfo": "Chrome 120.0 on macOS",
 *   "ipAddress": "192.168.1.100",
 *   "issuedAt": ISODate("2026-01-17T10:00:00Z"),
 *   "expiresAt": ISODate("2026-01-24T10:00:00Z"),
 *   "lastUsedAt": ISODate("2026-01-17T12:30:00Z"),
 *   "revoked": false
 * }
 */
@Document(collection = "refresh_tokens")
@CompoundIndexes({
  @CompoundIndex(
    name = "userId_revoked_expiresAt_idx",
    def = "{'userId': 1, 'revoked': 1, 'expiresAt': -1}"
  ),
  @CompoundIndex(
    name = "userId_deviceInfo_ipAddress_idx",
    def = "{'userId': 1, 'deviceInfo': 1, 'ipAddress': 1}"
  )
})
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
  @Indexed(unique = true)
  private String tokenHash;

  /**
   * SHA-256 hash of the access token associated with this refresh token.
   * Used for instant revocation when session is deleted from another device.
   * Without this, revoked sessions can still make API calls for 1 hour!
   */
  private String accessTokenHash;

  /**
   * User ID who owns this token.
   * 
   * REFACTORED from @DBRef User to String userId:
   * - Avoids unnecessary User fetches (many operations only need the ID)
   * - More explicit control over when to load User object
   * - Better MongoDB pattern (no joins/lookups)
   * - Simpler queries and better performance
   */
  @Indexed
  private String userId;

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
   * TTL index configured via @CompoundIndex for auto-deletion of expired tokens.
   */
  @Indexed
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


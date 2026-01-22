package com.modernizedkitechensink.kitchensinkmodernized.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Password Reset Token Entity.
 * 
 * Stores temporary tokens for password reset functionality.
 * Tokens expire after a configurable time period (default: 1 hour).
 * 
 * Security Features:
 * - Tokens are hashed (SHA-256) before storage
 * - One-time use (marked as used after redemption)
 * - TTL expiration (auto-deleted from MongoDB)
 * - User-specific (tied to userId)
 * 
 * Indexes (managed by MongoIndexInitializer):
 * - tokenHash_unique_idx: Unique index for token validation
 * - userId_idx: Index for user-specific queries
 * - expiresAt_ttl_idx: TTL index for auto-deletion
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Document(collection = "password_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    private String id;

    /**
     * SHA-256 hash of the reset token.
     * The actual token is sent via email, not stored in DB.
     * Index: tokenHash_unique_idx (unique)
     */
    private String tokenHash;

    /**
     * User ID this token belongs to.
     * Index: userId_idx
     */
    private String userId;

    /**
     * Email address where reset link was sent.
     */
    private String email;

    /**
     * Token creation timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Token expiration timestamp.
     * Index: expiresAt_ttl_idx (TTL - auto-deletes expired tokens)
     */
    private LocalDateTime expiresAt;

    /**
     * Whether this token has been used (one-time use).
     */
    private boolean used;

    /**
     * Timestamp when token was used (if applicable).
     */
    private LocalDateTime usedAt;

    /**
     * IP address from which the reset was requested.
     */
    private String ipAddress;

    /**
     * User agent of the browser that requested the reset.
     */
    private String userAgent;

    /**
     * Check if token is expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if token is valid (not used and not expired).
     */
    public boolean isValid() {
        return !used && !isExpired();
    }

    /**
     * Mark token as used.
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
}


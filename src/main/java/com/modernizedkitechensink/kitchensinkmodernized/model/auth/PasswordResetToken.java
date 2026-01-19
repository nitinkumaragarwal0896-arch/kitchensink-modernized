package com.modernizedkitechensink.kitchensinkmodernized.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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
     */
    @Indexed(unique = true)
    private String tokenHash;

    /**
     * User ID this token belongs to.
     */
    @Indexed
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
     */
    @Indexed(expireAfterSeconds = 0) // TTL index (expires at this time)
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


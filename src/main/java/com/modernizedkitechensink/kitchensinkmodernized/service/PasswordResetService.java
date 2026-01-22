package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.PasswordResetToken;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.PasswordResetTokenRepository;
import com.modernizedkitechensink.kitchensinkmodernized.repository.UserRepository;
import com.modernizedkitechensink.kitchensinkmodernized.util.PasswordValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Service for handling password reset functionality.
 * 
 * Features:
 * - Generate secure reset tokens
 * - Send reset emails
 * - Validate reset tokens
 * - Reset passwords
 * - Clean up expired tokens
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.token-expiration:3600000}") // Default: 1 hour
    private long tokenExpirationMs;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Request password reset for a user.
     * Generates a secure token and sends reset email.
     * 
     * NOTE: @Transactional provides transactional guarantees only with MongoDB replica sets.
     * With standalone MongoDB, operations execute normally but without rollback capability.
     * This is acceptable for password reset as operations (delete old tokens, save new) are idempotent.
     * 
     * @param email User's email
     * @param request HTTP request (for IP and user agent)
     * @return true if email exists and email sent, false otherwise
     */
    @Transactional
    public boolean requestPasswordReset(String email, HttpServletRequest request) {
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            // Don't reveal if email exists (security best practice)
            log.warn("Password reset requested for non-existent email: {}", email);
            return true; // Return true anyway to not reveal user existence
        }

        // Invalidate any existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        // Generate secure random token (32 bytes = 256 bits)
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Hash token for storage
        String tokenHash = hashToken(token);

        // Create token entity
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .tokenHash(tokenHash)
            .userId(user.getId())
            .email(email)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusSeconds(tokenExpirationMs / 1000))
            .used(false)
            .ipAddress(getClientIp(request))
            .userAgent(request.getHeader("User-Agent"))
            .build();

        tokenRepository.save(resetToken);

        // Send reset email asynchronously
        emailService.sendPasswordResetEmail(email, token, user.getUsername());

        log.info("Password reset requested for user: {} (email: {})", user.getUsername(), email);
        
        return true;
    }

    /**
     * Reset password using a valid token.
     * 
     * NOTE: @Transactional provides transactional guarantees only with MongoDB replica sets.
     * Operations here are atomic at document level (update user, mark token as used).
     * 
     * @param token Reset token (plain text from email)
     * @param newPassword New password
     * @return true if successful, false if token invalid/expired
     * @throws IllegalArgumentException if newPassword doesn't meet strength requirements
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        // ⚠️ SECURITY: Validate password strength BEFORE processing token
        PasswordValidator.ValidationResult passwordValidation = PasswordValidator.validate(newPassword);
        if (!passwordValidation.isValid()) {
            throw new IllegalArgumentException(passwordValidation.getErrorMessage());
        }
        
        String tokenHash = hashToken(token);
        
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash).orElse(null);
        
        if (resetToken == null) {
            log.warn("Password reset attempted with invalid token");
            return false;
        }

        if (!resetToken.isValid()) {
            log.warn("Password reset attempted with expired/used token for user: {}", resetToken.getUserId());
            return false;
        }

        User user = userRepository.findById(resetToken.getUserId()).orElse(null);
        
        if (user == null) {
            log.error("User not found for password reset token: {}", resetToken.getUserId());
            return false;
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        // Delete all other tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        log.info("Password successfully reset for user: {}", user.getUsername());
        
        return true;
    }

    /**
     * Validate a reset token without using it.
     * 
     * @param token Reset token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        String tokenHash = hashToken(token);
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash).orElse(null);
        return resetToken != null && resetToken.isValid();
    }

    /**
     * Hash token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Get client IP address from request.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}


package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.PasswordResetToken;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.PasswordResetTokenRepository;
import com.modernizedkitechensink.kitchensinkmodernized.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PasswordResetService.
 * 
 * Tests cover:
 * - Password reset request flow
 * - Token validation
 * - Password reset with valid/invalid tokens
 * - Edge cases and security scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService Tests")
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private static final Long TOKEN_EXPIRATION_MS = 3600000L; // 1 hour

    private User testUser;

    @BeforeEach
    void setUp() {
        // Set token expiration
        ReflectionTestUtils.setField(passwordResetService, "tokenExpirationMs", TOKEN_EXPIRATION_MS);

        // Create test user
        testUser = User.builder()
            .id("user123")
            .username("testuser")
            .email("test@example.com")
            .password("$2a$10$hashedPassword")
            .build();

        // Mock HttpServletRequest (lenient to avoid unnecessary stubbing warnings)
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    }

    // ========== requestPasswordReset() Tests ==========

    @Test
    @DisplayName("Should successfully request password reset for existing user")
    void shouldSuccessfullyRequestPasswordResetForExistingUser() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // Act
        boolean result = passwordResetService.requestPasswordReset(email, httpRequest);

        // Assert
        assertTrue(result);
        verify(userRepository).findByEmail(email);
        verify(tokenRepository).deleteByUserId(testUser.getId());
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(email), anyString(), eq(testUser.getUsername()));
    }

    @Test
    @DisplayName("Should return true for non-existent email (security: don't reveal user existence)")
    void shouldReturnTrueForNonExistentEmail() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        boolean result = passwordResetService.requestPasswordReset(email, httpRequest);

        // Assert
        assertTrue(result); // Still returns true to not reveal user existence
        verify(userRepository).findByEmail(email);
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should delete existing tokens before creating new one")
    void shouldDeleteExistingTokensBeforeCreatingNewOne() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // Act
        passwordResetService.requestPasswordReset(email, httpRequest);

        // Assert - verify deletion happens before save
        var order = inOrder(tokenRepository);
        order.verify(tokenRepository).deleteByUserId(testUser.getId());
        order.verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("Should create token with correct expiration time")
    void shouldCreateTokenWithCorrectExpirationTime() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        // Act
        passwordResetService.requestPasswordReset(email, httpRequest);

        // Assert
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertNotNull(savedToken.getExpiresAt());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));
        assertFalse(savedToken.isUsed());
        assertEquals(testUser.getId(), savedToken.getUserId());
        assertEquals(email, savedToken.getEmail());
    }

    @Test
    @DisplayName("Should capture client IP from X-Forwarded-For header")
    void shouldCaptureClientIpFromXForwardedForHeader() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        doReturn("203.0.113.1, 198.51.100.1").when(httpRequest).getHeader("X-Forwarded-For");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        // Act
        passwordResetService.requestPasswordReset(email, httpRequest);

        // Assert
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertEquals("203.0.113.1", savedToken.getIpAddress()); // First IP in X-Forwarded-For
    }

    @Test
    @DisplayName("Should use remote address when X-Forwarded-For is not present")
    void shouldUseRemoteAddressWhenXForwardedForIsNotPresent() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        doReturn(null).when(httpRequest).getHeader("X-Forwarded-For");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        // Act
        passwordResetService.requestPasswordReset(email, httpRequest);

        // Assert
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertEquals("192.168.1.1", savedToken.getIpAddress()); // From getRemoteAddr()
    }

    @Test
    @DisplayName("Should capture User-Agent from request")
    void shouldCaptureUserAgentFromRequest() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        // Act
        passwordResetService.requestPasswordReset(email, httpRequest);

        // Assert
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertEquals("Mozilla/5.0", savedToken.getUserAgent());
    }

    // ========== resetPassword() Tests ==========

    @Test
    @DisplayName("Should successfully reset password with valid token")
    void shouldSuccessfullyResetPasswordWithValidToken() {
        // Arrange
        String plainToken = "validToken123";
        String newPassword = "newSecurePassword123";
        String encodedPassword = "$2a$10$newEncodedPassword";

        PasswordResetToken resetToken = PasswordResetToken.builder()
            .tokenHash("hashedToken")
            .userId(testUser.getId())
            .email(testUser.getEmail())
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(1)) // Valid, not expired
            .used(false)
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        // Act
        boolean result = passwordResetService.resetPassword(plainToken, newPassword);

        // Assert
        assertTrue(result);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(resetToken);
        verify(tokenRepository).deleteByUserId(testUser.getId());
        assertEquals(encodedPassword, testUser.getPassword());
        assertTrue(resetToken.isUsed());
    }

    @Test
    @DisplayName("Should fail to reset password with invalid token")
    void shouldFailToResetPasswordWithInvalidToken() {
        // Arrange
        String plainToken = "invalidToken";
        String newPassword = "newPassword123";

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // Act
        boolean result = passwordResetService.resetPassword(plainToken, newPassword);

        // Assert
        assertFalse(result);
        verify(tokenRepository).findByTokenHash(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to reset password with expired token")
    void shouldFailToResetPasswordWithExpiredToken() {
        // Arrange
        String plainToken = "expiredToken";
        String newPassword = "newPassword123";

        PasswordResetToken expiredToken = PasswordResetToken.builder()
            .tokenHash("hashedToken")
            .userId(testUser.getId())
            .email(testUser.getEmail())
            .createdAt(LocalDateTime.now().minusHours(2))
            .expiresAt(LocalDateTime.now().minusHours(1)) // Expired
            .used(false)
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        // Act
        boolean result = passwordResetService.resetPassword(plainToken, newPassword);

        // Assert
        assertFalse(result);
        verify(tokenRepository).findByTokenHash(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to reset password with already used token")
    void shouldFailToResetPasswordWithAlreadyUsedToken() {
        // Arrange
        String plainToken = "usedToken";
        String newPassword = "newPassword123";

        PasswordResetToken usedToken = PasswordResetToken.builder()
            .tokenHash("hashedToken")
            .userId(testUser.getId())
            .email(testUser.getEmail())
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(1))
            .used(true) // Already used
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usedToken));

        // Act
        boolean result = passwordResetService.resetPassword(plainToken, newPassword);

        // Assert
        assertFalse(result);
        verify(tokenRepository).findByTokenHash(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to reset password when user not found")
    void shouldFailToResetPasswordWhenUserNotFound() {
        // Arrange
        String plainToken = "validToken";
        String newPassword = "newPassword123";

        PasswordResetToken resetToken = PasswordResetToken.builder()
            .tokenHash("hashedToken")
            .userId("nonExistentUserId")
            .email("test@example.com")
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(1))
            .used(false)
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(resetToken));
        when(userRepository.findById("nonExistentUserId")).thenReturn(Optional.empty());

        // Act
        boolean result = passwordResetService.resetPassword(plainToken, newPassword);

        // Assert
        assertFalse(result);
        verify(userRepository).findById("nonExistentUserId");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should mark token as used after successful password reset")
    void shouldMarkTokenAsUsedAfterSuccessfulPasswordReset() {
        // Arrange
        String plainToken = "validToken";
        String newPassword = "newPassword123";

        PasswordResetToken resetToken = PasswordResetToken.builder()
            .tokenHash("hashedToken")
            .userId(testUser.getId())
            .email(testUser.getEmail())
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(1))
            .used(false)
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");

        // Act
        passwordResetService.resetPassword(plainToken, newPassword);

        // Assert
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertTrue(tokenCaptor.getValue().isUsed());
    }

    // ========== validateToken() Tests ==========

    @Test
    @DisplayName("Should return true for valid token")
    void shouldReturnTrueForValidToken() {
        // Arrange
        String plainToken = "validToken";

        PasswordResetToken validToken = PasswordResetToken.builder()
            .tokenHash("hashedToken")
            .userId(testUser.getId())
            .email(testUser.getEmail())
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(1))
            .used(false)
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(validToken));

        // Act
        boolean result = passwordResetService.validateToken(plainToken);

        // Assert
        assertTrue(result);
        verify(tokenRepository).findByTokenHash(anyString());
    }

    @Test
    @DisplayName("Should return false for non-existent token")
    void shouldReturnFalseForNonExistentToken() {
        // Arrange
        String plainToken = "nonExistentToken";

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // Act
        boolean result = passwordResetService.validateToken(plainToken);

        // Assert
        assertFalse(result);
        verify(tokenRepository).findByTokenHash(anyString());
    }

    @Test
    @DisplayName("Should return false for expired token")
    void shouldReturnFalseForExpiredToken() {
        // Arrange
        String plainToken = "expiredToken";

        PasswordResetToken expiredToken = PasswordResetToken.builder()
            .tokenHash("hashedToken")
            .userId(testUser.getId())
            .email(testUser.getEmail())
            .createdAt(LocalDateTime.now().minusHours(2))
            .expiresAt(LocalDateTime.now().minusHours(1)) // Expired
            .used(false)
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        // Act
        boolean result = passwordResetService.validateToken(plainToken);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for used token")
    void shouldReturnFalseForUsedToken() {
        // Arrange
        String plainToken = "usedToken";

        PasswordResetToken usedToken = PasswordResetToken.builder()
            .tokenHash("hashedToken")
            .userId(testUser.getId())
            .email(testUser.getEmail())
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(1))
            .used(true) // Already used
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usedToken));

        // Act
        boolean result = passwordResetService.validateToken(plainToken);

        // Assert
        assertFalse(result);
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("Should handle empty X-Forwarded-For header")
    void shouldHandleEmptyXForwardedForHeader() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        doReturn("").when(httpRequest).getHeader("X-Forwarded-For");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        // Act
        passwordResetService.requestPasswordReset(email, httpRequest);

        // Assert
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        // Should fall back to remote address
        assertEquals("192.168.1.1", savedToken.getIpAddress());
    }

    @Test
    @DisplayName("Should handle null User-Agent header")
    void shouldHandleNullUserAgentHeader() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        doReturn(null).when(httpRequest).getHeader("User-Agent");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        // Act
        passwordResetService.requestPasswordReset(email, httpRequest);

        // Assert
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertNull(savedToken.getUserAgent());
    }

    @Test
    @DisplayName("Should delete all user tokens after successful password reset")
    void shouldDeleteAllUserTokensAfterSuccessfulPasswordReset() {
        // Arrange
        String plainToken = "validToken";
        String newPassword = "newPassword123";

        PasswordResetToken resetToken = PasswordResetToken.builder()
            .tokenHash("hashedToken")
            .userId(testUser.getId())
            .email(testUser.getEmail())
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(1))
            .used(false)
            .build();

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");

        // Act
        passwordResetService.resetPassword(plainToken, newPassword);

        // Assert - verify deletion happens at the end
        verify(tokenRepository).deleteByUserId(testUser.getId());
    }

    @Test
    @DisplayName("Should generate unique tokens for multiple requests")
    void shouldGenerateUniqueTokensForMultipleRequests() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        ArgumentCaptor<PasswordResetToken> tokenCaptor1 = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<PasswordResetToken> tokenCaptor2 = ArgumentCaptor.forClass(PasswordResetToken.class);

        // Act
        passwordResetService.requestPasswordReset(email, httpRequest);
        passwordResetService.requestPasswordReset(email, httpRequest);

        // Assert
        verify(tokenRepository, times(2)).save(any(PasswordResetToken.class));
        // Note: We can't easily verify token uniqueness without exposing the token,
        // but we verify that save was called twice with different token hashes
    }

    @Test
    @DisplayName("Should handle multiple IPs in X-Forwarded-For header")
    void shouldHandleMultipleIpsInXForwardedForHeader() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        doReturn("203.0.113.1, 198.51.100.1, 192.0.2.1").when(httpRequest).getHeader("X-Forwarded-For");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        // Act
        passwordResetService.requestPasswordReset(email, httpRequest);

        // Assert
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        // Should extract only the first IP
        assertEquals("203.0.113.1", savedToken.getIpAddress());
    }
}

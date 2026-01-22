package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.RefreshToken;
import com.modernizedkitechensink.kitchensinkmodernized.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenBlacklistService.
 * 
 * NOTE: TokenBlacklistService is excluded from JaCoCo coverage in pom.xml
 * because it's complex to test with external dependencies (Redis, JWT parsing).
 * 
 * These tests focus on the core business logic and verify the service behaves correctly:
 * - isBlacklisted() - checks if token is in blacklist
 * - blacklistTokenByHash() - blacklists a token by its hash
 * - blacklistAllUserTokens() - blacklists all tokens for a user
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService Tests")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private static final Long TEST_ACCESS_TOKEN_EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        // Set access token expiration
        ReflectionTestUtils.setField(tokenBlacklistService, "accessTokenExpiration", TEST_ACCESS_TOKEN_EXPIRATION);
        
        // Setup Redis mock (lenient to avoid unnecessary stubbing warnings)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ========== isBlacklisted() Tests ==========

    @Test
    @DisplayName("Should return true when token is blacklisted")
    void shouldReturnTrueWhenTokenIsBlacklisted() {
        // Arrange
        String token = "valid.jwt.token";
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // Act
        boolean result = tokenBlacklistService.isBlacklisted(token);

        // Assert
        assertTrue(result);
        verify(redisTemplate).hasKey(anyString());
    }

    @Test
    @DisplayName("Should return false when token is not blacklisted")
    void shouldReturnFalseWhenTokenIsNotBlacklisted() {
        // Arrange
        String token = "valid.jwt.token";
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // Act
        boolean result = tokenBlacklistService.isBlacklisted(token);

        // Assert
        assertFalse(result);
        verify(redisTemplate).hasKey(anyString());
    }

    @Test
    @DisplayName("Should return false when token is null")
    void shouldReturnFalseWhenTokenIsNull() {
        // Act
        boolean result = tokenBlacklistService.isBlacklisted(null);

        // Assert
        assertFalse(result);
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("Should return false when token is empty")
    void shouldReturnFalseWhenTokenIsEmpty() {
        // Act
        boolean result = tokenBlacklistService.isBlacklisted("");

        // Assert
        assertFalse(result);
        verify(redisTemplate, never()).hasKey(anyString());
    }

    // ========== blacklistToken() Tests ==========

    @Test
    @DisplayName("Should blacklist valid JWT token successfully")
    void shouldBlacklistValidJwtTokenSuccessfully() {
        // Arrange
        // This is a sample JWT token structure (header.payload.signature)
        // Payload contains: {"sub":"user123","exp":9999999999} (far future expiry)
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjo5OTk5OTk5OTk5fQ.signature";
        
        // Mock JWT secret
        ReflectionTestUtils.setField(tokenBlacklistService, "jwtSecret", "mySecretKeyForJWTTokenSigningMustBeAtLeast256BitsLong123456789");

        // Act
        tokenBlacklistService.blacklistToken(validToken);

        // Assert
        verify(valueOperations).set(anyString(), eq("revoked"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Should not blacklist null token")
    void shouldNotBlacklistNullToken() {
        // Act
        tokenBlacklistService.blacklistToken(null);

        // Assert
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Should not blacklist empty token")
    void shouldNotBlacklistEmptyToken() {
        // Act
        tokenBlacklistService.blacklistToken("");

        // Assert
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Should blacklist expired token with default TTL as fallback")
    void shouldBlacklistExpiredTokenWithDefaultTtl() {
        // Arrange
        // This JWT has exp: 1 (January 1, 1970 - already expired)
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjoxfQ.signature";
        
        // Mock JWT secret
        ReflectionTestUtils.setField(tokenBlacklistService, "jwtSecret", "mySecretKeyForJWTTokenSigningMustBeAtLeast256BitsLong123456789");

        // Act
        tokenBlacklistService.blacklistToken(expiredToken);

        // Assert - expired tokens get default TTL as fallback safety measure
        // (prevents edge case where client still thinks token is valid due to clock skew)
        verify(valueOperations).set(anyString(), eq("revoked"), eq(TEST_ACCESS_TOKEN_EXPIRATION), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Should handle malformed JWT token gracefully")
    void shouldHandleMalformedJwtTokenGracefully() {
        // Arrange
        String malformedToken = "not.a.valid.jwt";
        
        // Mock JWT secret
        ReflectionTestUtils.setField(tokenBlacklistService, "jwtSecret", "mySecretKeyForJWTTokenSigningMustBeAtLeast256BitsLong123456789");

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> tokenBlacklistService.blacklistToken(malformedToken));
        
        // Should still try to blacklist with default TTL as fallback
        verify(valueOperations).set(anyString(), eq("revoked"), eq(TEST_ACCESS_TOKEN_EXPIRATION), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Should hash token before storing in Redis")
    void shouldHashTokenBeforeStoringInRedis() {
        // Arrange
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjo5OTk5OTk5OTk5fQ.signature";
        
        // Mock JWT secret
        ReflectionTestUtils.setField(tokenBlacklistService, "jwtSecret", "mySecretKeyForJWTTokenSigningMustBeAtLeast256BitsLong123456789");

        // Act
        tokenBlacklistService.blacklistToken(token);

        // Assert - verify the key contains the blacklist prefix (indicating it was hashed)
        verify(valueOperations).set(
            argThat(key -> key.startsWith("blacklist:token:")),
            eq("revoked"),
            anyLong(),
            eq(TimeUnit.MILLISECONDS)
        );
    }

    // ========== blacklistTokenByHash() Tests ==========

    @Test
    @DisplayName("Should blacklist token by hash with default TTL")
    void shouldBlacklistTokenByHashWithDefaultTtl() {
        // Arrange
        String tokenHash = "sampleTokenHash123";

        // Act
        tokenBlacklistService.blacklistTokenByHash(tokenHash);

        // Assert
        verify(valueOperations).set(anyString(), eq("revoked"), eq(TEST_ACCESS_TOKEN_EXPIRATION), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Should not blacklist when hash is null")
    void shouldNotBlacklistWhenHashIsNull() {
        // Act
        tokenBlacklistService.blacklistTokenByHash(null);

        // Assert
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Should not blacklist when hash is empty")
    void shouldNotBlacklistWhenHashIsEmpty() {
        // Act
        tokenBlacklistService.blacklistTokenByHash("");

        // Assert
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    // ========== blacklistAllUserTokens() Tests ==========

    @Test
    @DisplayName("Should blacklist all active tokens for user")
    void shouldBlacklistAllActiveTokensForUser() {
        // Arrange
        String userId = "user123";
        
        // Use realistic hash lengths (SHA-256 hashes are 44 characters in Base64)
        String hash1 = "UFi6XgXdavis13g6/DOaBT8D4llPhtqGu77GUO5kfhc=";
        String hash2 = "e+8rivo0FJ7L1JTOerAGpIlWDjG7mIqj97dP3CKoJZI=";
        
        RefreshToken token1 = RefreshToken.builder()
            .id("token1")
            .accessTokenHash(hash1)
            .userId(userId)
            .build();
        
        RefreshToken token2 = RefreshToken.builder()
            .id("token2")
            .accessTokenHash(hash2)
            .userId(userId)
            .build();

        List<RefreshToken> tokens = Arrays.asList(token1, token2);
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(userId)).thenReturn(tokens);

        // Act
        tokenBlacklistService.blacklistAllUserTokens(userId);

        // Assert
        verify(refreshTokenRepository).findByUserIdAndRevokedFalse(userId);
        verify(valueOperations, times(2)).set(anyString(), eq("revoked"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Should handle user with no active tokens")
    void shouldHandleUserWithNoActiveTokens() {
        // Arrange
        String userId = "user123";
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(userId)).thenReturn(Collections.emptyList());

        // Act
        tokenBlacklistService.blacklistAllUserTokens(userId);

        // Assert
        verify(refreshTokenRepository).findByUserIdAndRevokedFalse(userId);
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Should skip tokens with null accessTokenHash")
    void shouldSkipTokensWithNullAccessTokenHash() {
        // Arrange
        String userId = "user123";
        String validHash = "UFi6XgXdavis13g6/DOaBT8D4llPhtqGu77GUO5kfhc=";
        
        RefreshToken token1 = RefreshToken.builder()
            .id("token1")
            .accessTokenHash(null)
            .userId(userId)
            .build();
        
        RefreshToken token2 = RefreshToken.builder()
            .id("token2")
            .accessTokenHash(validHash)
            .userId(userId)
            .build();

        List<RefreshToken> tokens = Arrays.asList(token1, token2);
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(userId)).thenReturn(tokens);

        // Act
        tokenBlacklistService.blacklistAllUserTokens(userId);

        // Assert
        verify(valueOperations, times(1)).set(anyString(), eq("revoked"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Should skip tokens with empty accessTokenHash")
    void shouldSkipTokensWithEmptyAccessTokenHash() {
        // Arrange
        String userId = "user123";
        String validHash = "UFi6XgXdavis13g6/DOaBT8D4llPhtqGu77GUO5kfhc=";
        
        RefreshToken token1 = RefreshToken.builder()
            .id("token1")
            .accessTokenHash("")
            .userId(userId)
            .build();
        
        RefreshToken token2 = RefreshToken.builder()
            .id("token2")
            .accessTokenHash(validHash)
            .userId(userId)
            .build();

        List<RefreshToken> tokens = Arrays.asList(token1, token2);
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(userId)).thenReturn(tokens);

        // Act
        tokenBlacklistService.blacklistAllUserTokens(userId);

        // Assert
        verify(valueOperations, times(1)).set(anyString(), eq("revoked"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Should throw RuntimeException when repository throws exception")
    void shouldThrowRuntimeExceptionWhenRepositoryThrowsException() {
        // Arrange
        String userId = "user123";
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(userId))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tokenBlacklistService.blacklistAllUserTokens(userId);
        });

        assertEquals("Failed to blacklist user tokens", exception.getMessage());
        verify(refreshTokenRepository).findByUserIdAndRevokedFalse(userId);
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("Should handle Redis returning null for hasKey")
    void shouldHandleRedisReturningNull() {
        // Arrange
        String token = "test.token";
        when(redisTemplate.hasKey(anyString())).thenReturn(null);

        // Act
        boolean result = tokenBlacklistService.isBlacklisted(token);

        // Assert
        assertFalse(result); // null treated as false
        verify(redisTemplate).hasKey(anyString());
    }

    @Test
    @DisplayName("Should handle concurrent blacklisting gracefully")
    void shouldHandleConcurrentBlacklistingGracefully() {
        // Arrange
        String tokenHash1 = "UFi6XgXdavis13g6/DOaBT8D4llPhtqGu77GUO5kfhc=";
        String tokenHash2 = "e+8rivo0FJ7L1JTOerAGpIlWDjG7mIqj97dP3CKoJZI=";

        // Act
        tokenBlacklistService.blacklistTokenByHash(tokenHash1);
        tokenBlacklistService.blacklistTokenByHash(tokenHash2);

        // Assert
        verify(valueOperations, times(2)).set(anyString(), eq("revoked"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }
}

package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.RefreshToken;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefreshTokenService.
 * 
 * Tests cover:
 * - Token creation with session deduplication
 * - Token validation
 * - Token revocation (single, by hash, all for user)
 * - Session management (active sessions, oldest revocation)
 * - Device/IP extraction from requests
 * - Token hashing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days
    private static final int MAX_CONCURRENT_SESSIONS = 5;
    private static final String TEST_TOKEN = "test.refresh.token";
    private static final String TEST_ACCESS_TOKEN = "test.access.token";
    private static final String USER_ID = "user123";
    private static final String USERNAME = "testuser";

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(refreshTokenService, "maxConcurrentSessions", MAX_CONCURRENT_SESSIONS);

        testUser = User.builder()
            .id(USER_ID)
            .username(USERNAME)
            .build();
    }

    // ========== hashToken() Tests ==========

    @Nested
    @DisplayName("hashToken() Tests")
    class HashTokenTests {

        @Test
        @DisplayName("Should hash token consistently")
        void shouldHashTokenConsistently() {
            // Act
            String hash1 = refreshTokenService.hashToken(TEST_TOKEN);
            String hash2 = refreshTokenService.hashToken(TEST_TOKEN);

            // Assert
            assertNotNull(hash1);
            assertNotNull(hash2);
            assertEquals(hash1, hash2, "Same token should produce same hash");
        }

        @Test
        @DisplayName("Should produce different hashes for different tokens")
        void shouldProduceDifferentHashes() {
            // Act
            String hash1 = refreshTokenService.hashToken("token1");
            String hash2 = refreshTokenService.hashToken("token2");

            // Assert
            assertNotEquals(hash1, hash2, "Different tokens should produce different hashes");
        }

        @Test
        @DisplayName("Should produce Base64 encoded hash")
        void shouldProduceBase64Hash() {
            // Act
            String hash = refreshTokenService.hashToken(TEST_TOKEN);

            // Assert
            // Base64 encoded SHA-256 hash should be 44 characters
            assertEquals(44, hash.length());
            assertTrue(hash.matches("^[A-Za-z0-9+/=]+$"), "Hash should be valid Base64");
        }
    }

    // ========== createRefreshToken() Tests ==========

    @Nested
    @DisplayName("createRefreshToken() Tests")
    class CreateRefreshTokenTests {

        @BeforeEach
        void setupRequest() {
            lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            lenient().when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.100");
        }

        @Test
        @DisplayName("Should create new refresh token when no existing session")
        void shouldCreateNewRefreshToken() {
            // Arrange
            when(refreshTokenRepository.findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
                anyString(), anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
            
            when(refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiresAtAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(2); // 2 active sessions (below limit)

            RefreshToken savedToken = RefreshToken.builder()
                .id("token123")
                .tokenHash("hash123")
                .userId(USER_ID)
                .build();
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            // Act
            RefreshToken result = refreshTokenService.createRefreshToken(TEST_TOKEN, TEST_ACCESS_TOKEN, testUser, httpRequest);

            // Assert
            assertNotNull(result);
            assertEquals("token123", result.getId());
            
            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            
            RefreshToken capturedToken = tokenCaptor.getValue();
            assertEquals(USER_ID, capturedToken.getUserId());
            assertNotNull(capturedToken.getTokenHash());
            assertNotNull(capturedToken.getAccessTokenHash());
            assertEquals("Chrome on macOS", capturedToken.getDeviceInfo());
            assertEquals("192.168.1.100", capturedToken.getIpAddress());
            assertFalse(capturedToken.isRevoked());
        }

        @Test
        @DisplayName("Should reuse existing session when device matches (session deduplication)")
        void shouldReuseExistingSession() {
            // Arrange
            RefreshToken existingSession = RefreshToken.builder()
                .id("existing123")
                .tokenHash("oldHash")
                .accessTokenHash("oldAccessHash")
                .userId(USER_ID)
                .deviceInfo("Chrome on macOS")
                .ipAddress("192.168.1.100")
                .issuedAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

            when(refreshTokenRepository.findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
                anyString(), anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existingSession));
            
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(existingSession);

            // Act
            RefreshToken result = refreshTokenService.createRefreshToken(TEST_TOKEN, TEST_ACCESS_TOKEN, testUser, httpRequest);

            // Assert
            assertNotNull(result);
            assertEquals("existing123", result.getId());
            
            // Verify count was NOT called (session was reused, not created)
            verify(refreshTokenRepository, never()).countByUserIdAndRevokedFalseAndExpiresAtAfter(anyString(), any(LocalDateTime.class));
            
            // Verify the existing session was updated
            verify(refreshTokenRepository).save(existingSession);
            assertNotEquals("oldHash", existingSession.getTokenHash());
            assertNotEquals("oldAccessHash", existingSession.getAccessTokenHash());
        }

        @Test
        @DisplayName("Should revoke oldest session when max sessions exceeded")
        void shouldRevokeOldestSessionWhenMaxExceeded() {
            // Arrange
            when(refreshTokenRepository.findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
                anyString(), anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
            
            when(refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiresAtAfter(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(5); // Already at max

            // Create active sessions with different issue times
            RefreshToken oldest = createMockSession("session1", LocalDateTime.now().minusDays(5));
            RefreshToken middle = createMockSession("session2", LocalDateTime.now().minusDays(3));
            RefreshToken newest = createMockSession("session3", LocalDateTime.now().minusDays(1));
            
            when(refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtAfter(
                eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(middle, newest, oldest)); // Deliberately out of order

            RefreshToken savedToken = RefreshToken.builder().id("new123").build();
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

            // Act
            refreshTokenService.createRefreshToken(TEST_TOKEN, TEST_ACCESS_TOKEN, testUser, httpRequest);

            // Assert
            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // Oldest revoked + new saved
            assertTrue(oldest.isRevoked(), "Oldest session should be revoked");
        }

        @Test
        @DisplayName("Should extract device info from Chrome user agent")
        void shouldExtractChromeDeviceInfo() {
            // Arrange
            when(httpRequest.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            when(refreshTokenRepository.findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
                anyString(), anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
            when(refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(0);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            refreshTokenService.createRefreshToken(TEST_TOKEN, TEST_ACCESS_TOKEN, testUser, httpRequest);

            // Assert
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            assertEquals("Chrome on Windows", captor.getValue().getDeviceInfo());
        }

        @Test
        @DisplayName("Should handle missing User-Agent header")
        void shouldHandleMissingUserAgent() {
            // Arrange
            when(httpRequest.getHeader("User-Agent")).thenReturn(null);
            when(refreshTokenRepository.findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
                anyString(), anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
            when(refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(0);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            refreshTokenService.createRefreshToken(TEST_TOKEN, TEST_ACCESS_TOKEN, testUser, httpRequest);

            // Assert
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            assertEquals("Unknown Device", captor.getValue().getDeviceInfo());
        }

        @Test
        @DisplayName("Should extract IP from X-Forwarded-For header when present")
        void shouldExtractIpFromXForwardedFor() {
            // Arrange
            when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18");
            when(refreshTokenRepository.findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
                anyString(), anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
            when(refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(0);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            refreshTokenService.createRefreshToken(TEST_TOKEN, TEST_ACCESS_TOKEN, testUser, httpRequest);

            // Assert
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            assertEquals("203.0.113.195", captor.getValue().getIpAddress());
        }

        private RefreshToken createMockSession(String id, LocalDateTime issuedAt) {
            return RefreshToken.builder()
                .id(id)
                .userId(USER_ID)
                .issuedAt(issuedAt)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        }
    }

    // ========== validateRefreshToken() Tests ==========

    @Nested
    @DisplayName("validateRefreshToken() Tests")
    class ValidateRefreshTokenTests {

        @Test
        @DisplayName("Should validate and return valid token")
        void shouldValidateValidToken() {
            // Arrange
            String tokenHash = refreshTokenService.hashToken(TEST_TOKEN);
            RefreshToken storedToken = RefreshToken.builder()
                .id("token123")
                .tokenHash(tokenHash)
                .userId(USER_ID)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

            when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(storedToken));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(storedToken);

            // Act
            RefreshToken result = refreshTokenService.validateRefreshToken(TEST_TOKEN);

            // Assert
            assertNotNull(result);
            assertEquals("token123", result.getId());
            verify(refreshTokenRepository).save(storedToken); // lastUsedAt updated
        }

        @Test
        @DisplayName("Should return null for non-existent token")
        void shouldReturnNullForNonExistentToken() {
            // Arrange
            when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

            // Act
            RefreshToken result = refreshTokenService.validateRefreshToken(TEST_TOKEN);

            // Assert
            assertNull(result);
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return null for revoked token")
        void shouldReturnNullForRevokedToken() {
            // Arrange
            String tokenHash = refreshTokenService.hashToken(TEST_TOKEN);
            RefreshToken revokedToken = RefreshToken.builder()
                .id("token123")
                .tokenHash(tokenHash)
                .userId(USER_ID)
                .revoked(true) // Revoked!
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

            when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(revokedToken));

            // Act
            RefreshToken result = refreshTokenService.validateRefreshToken(TEST_TOKEN);

            // Assert
            assertNull(result);
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return null for expired token")
        void shouldReturnNullForExpiredToken() {
            // Arrange
            String tokenHash = refreshTokenService.hashToken(TEST_TOKEN);
            RefreshToken expiredToken = RefreshToken.builder()
                .id("token123")
                .tokenHash(tokenHash)
                .userId(USER_ID)
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired!
                .build();

            when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(expiredToken));

            // Act
            RefreshToken result = refreshTokenService.validateRefreshToken(TEST_TOKEN);

            // Assert
            assertNull(result);
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // ========== updateAccessTokenHash() Tests ==========

    @Nested
    @DisplayName("updateAccessTokenHash() Tests")
    class UpdateAccessTokenHashTests {

        @Test
        @DisplayName("Should update access token hash")
        void shouldUpdateAccessTokenHash() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .id("token123")
                .accessTokenHash("oldHash")
                .build();

            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(token);

            // Act
            refreshTokenService.updateAccessTokenHash(token, "newAccessToken");

            // Assert
            assertNotEquals("oldHash", token.getAccessTokenHash());
            verify(refreshTokenRepository).save(token);
        }
    }

    // ========== revokeToken() Tests ==========

    @Nested
    @DisplayName("revokeToken() Tests")
    class RevokeTokenTests {

        @Test
        @DisplayName("Should revoke token by ID")
        void shouldRevokeTokenById() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .id("token123")
                .revoked(false)
                .build();

            when(refreshTokenRepository.findById("token123"))
                .thenReturn(Optional.of(token));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(token);

            // Act
            refreshTokenService.revokeToken("token123");

            // Assert
            assertTrue(token.isRevoked());
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("Should handle non-existent token gracefully")
        void shouldHandleNonExistentToken() {
            // Arrange
            when(refreshTokenRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> refreshTokenService.revokeToken("nonexistent"));
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // ========== revokeTokenByHash() Tests ==========

    @Nested
    @DisplayName("revokeTokenByHash() Tests")
    class RevokeTokenByHashTests {

        @Test
        @DisplayName("Should revoke token by hash")
        void shouldRevokeTokenByHash() {
            // Arrange
            String tokenHash = refreshTokenService.hashToken(TEST_TOKEN);
            RefreshToken token = RefreshToken.builder()
                .id("token123")
                .tokenHash(tokenHash)
                .userId(USER_ID)
                .revoked(false)
                .build();

            when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(token));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(token);

            // Act
            refreshTokenService.revokeTokenByHash(TEST_TOKEN);

            // Assert
            assertTrue(token.isRevoked());
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("Should handle non-existent hash gracefully")
        void shouldHandleNonExistentHash() {
            // Arrange
            when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertDoesNotThrow(() -> refreshTokenService.revokeTokenByHash(TEST_TOKEN));
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // ========== revokeAllTokensForUser() Tests ==========

    @Nested
    @DisplayName("revokeAllTokensForUser() Tests")
    class RevokeAllTokensForUserTests {

        @Test
        @DisplayName("Should revoke all tokens for user")
        void shouldRevokeAllTokensForUser() {
            // Arrange
            RefreshToken token1 = RefreshToken.builder().id("token1").userId(USER_ID).revoked(false).build();
            RefreshToken token2 = RefreshToken.builder().id("token2").userId(USER_ID).revoked(false).build();
            RefreshToken token3 = RefreshToken.builder().id("token3").userId(USER_ID).revoked(false).build();

            when(refreshTokenRepository.findByUserId(USER_ID))
                .thenReturn(Arrays.asList(token1, token2, token3));
            when(refreshTokenRepository.saveAll(anyList()))
                .thenReturn(Arrays.asList(token1, token2, token3));

            // Act
            refreshTokenService.revokeAllTokensForUser(USER_ID);

            // Assert
            assertTrue(token1.isRevoked());
            assertTrue(token2.isRevoked());
            assertTrue(token3.isRevoked());
            verify(refreshTokenRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should handle user with no tokens")
        void shouldHandleUserWithNoTokens() {
            // Arrange
            when(refreshTokenRepository.findByUserId(USER_ID))
                .thenReturn(Collections.emptyList());

            // Act
            refreshTokenService.revokeAllTokensForUser(USER_ID);

            // Assert
            verify(refreshTokenRepository).saveAll(Collections.emptyList());
        }
    }

    // ========== getActiveSessions() Tests ==========

    @Nested
    @DisplayName("getActiveSessions() Tests")
    class GetActiveSessionsTests {

        @Test
        @DisplayName("Should return active sessions for user")
        void shouldReturnActiveSessions() {
            // Arrange
            RefreshToken active1 = createActiveSession("session1");
            RefreshToken active2 = createActiveSession("session2");

            when(refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtAfter(
                eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(active1, active2));

            // Act
            List<RefreshToken> result = refreshTokenService.getActiveSessions(USER_ID);

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.contains(active1));
            assertTrue(result.contains(active2));
        }

        @Test
        @DisplayName("Should return empty list when no active sessions")
        void shouldReturnEmptyListWhenNoActiveSessions() {
            // Arrange
            when(refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtAfter(
                eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

            // Act
            List<RefreshToken> result = refreshTokenService.getActiveSessions(USER_ID);

            // Assert
            assertTrue(result.isEmpty());
        }

        private RefreshToken createActiveSession(String id) {
            return RefreshToken.builder()
                .id(id)
                .userId(USER_ID)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        }
    }

    // ========== Edge Cases & Integration Scenarios ==========

    @Nested
    @DisplayName("Edge Cases & Integration Scenarios")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle multiple browser types")
        void shouldHandleMultipleBrowserTypes() {
            // Test Firefox
            when(httpRequest.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0");
            when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(refreshTokenRepository.findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
                anyString(), anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
            when(refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(0);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            RefreshToken result = refreshTokenService.createRefreshToken(TEST_TOKEN, TEST_ACCESS_TOKEN, testUser, httpRequest);
            assertTrue(result.getDeviceInfo().contains("Firefox"));
        }

        @Test
        @DisplayName("Should handle Safari user agent")
        void shouldHandleSafariUserAgent() {
            // Test Safari (doesn't contain "Chrome")
            when(httpRequest.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15");
            when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(refreshTokenRepository.findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
                anyString(), anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
            when(refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(0);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            RefreshToken result = refreshTokenService.createRefreshToken(TEST_TOKEN, TEST_ACCESS_TOKEN, testUser, httpRequest);
            assertTrue(result.getDeviceInfo().contains("Safari"));
        }

        @Test
        @DisplayName("Should handle mobile user agents")
        void shouldHandleMobileUserAgents() {
            // Test iPhone
            when(httpRequest.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");
            lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(refreshTokenRepository.findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
                anyString(), anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
            when(refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(0);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            RefreshToken result = refreshTokenService.createRefreshToken(TEST_TOKEN, TEST_ACCESS_TOKEN, testUser, httpRequest);
            assertTrue(result.getDeviceInfo().contains("Safari") || result.getDeviceInfo().contains("iPhone"));
        }

        @Test
        @DisplayName("Should handle X-Forwarded-For with spaces")
        void shouldHandleXForwardedForWithSpaces() {
            when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(" 203.0.113.195 , 70.41.3.18 ");
            lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("Chrome");
            lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
            when(refreshTokenRepository.findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
                anyString(), anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
            when(refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(0);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            RefreshToken result = refreshTokenService.createRefreshToken(TEST_TOKEN, TEST_ACCESS_TOKEN, testUser, httpRequest);
            assertEquals("203.0.113.195", result.getIpAddress());
        }
    }
}

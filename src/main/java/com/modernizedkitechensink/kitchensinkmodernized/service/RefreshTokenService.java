package com.modernizedkitechensink.kitchensinkmodernized.service;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.RefreshToken;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Service for managing refresh tokens and user sessions.
 *
 * Responsibilities:
 * - Create refresh tokens on login
 * - Validate refresh tokens
 * - Enforce session limits (max 5 active sessions)
 * - Provide session management (view/revoke)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;

  @Value("${jwt.refresh.expiration:604800000}")  // 7 days in milliseconds
  private long refreshTokenExpiration;

  @Value("${session.max-concurrent:5}")  // Max 5 concurrent sessions
  private int maxConcurrentSessions;

  /**
   * Create and store a new refresh token.
   * 
   * SESSION DEDUPLICATION:
   * - Checks if user already has an active session from the same device/browser
   * - If yes: Updates existing session instead of creating new one (prevents duplicate sessions)
   * - If no: Creates new session
   * 
   * This ensures: 1 Browser = 1 Session (not 1 Tab = 1 Session)
   *
   * @param token The JWT refresh token string
   * @param accessToken The JWT access token string (for instant revocation)
   * @param user The user
   * @param request HTTP request for device/IP info
   * @return The saved RefreshToken entity
   */
  @Transactional
  public RefreshToken createRefreshToken(String token, String accessToken, User user, HttpServletRequest request) {
    log.info("Creating refresh token for user: {}", user.getUsername());

    // Extract device and IP info
    String deviceInfo = extractDeviceInfo(request);
    String ipAddress = extractIpAddress(request);

    // Hash the tokens (NEVER store plain tokens!)
    String tokenHash = hashToken(token);
    String accessTokenHash = hashToken(accessToken);

    // SESSION DEDUPLICATION: Check if user already has an active session from this device
    RefreshToken existingSession = findExistingSession(user.getId(), deviceInfo, ipAddress);
    
    if (existingSession != null) {
      // REUSE existing session - just update it with new tokens
      log.info("♻️ Reusing existing session for user: {} (device: {})", user.getUsername(), deviceInfo);
      
      existingSession.setTokenHash(tokenHash);
      existingSession.setAccessTokenHash(accessTokenHash);
      existingSession.setLastUsedAt(LocalDateTime.now());
      existingSession.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));
      existingSession.setRevoked(false); // In case it was revoked, un-revoke it
      
      existingSession = refreshTokenRepository.save(existingSession);
      log.info("✅ Session updated (deduplicated): {}", existingSession.getId());
      
      return existingSession;
    }

    // No existing session - check session limits before creating new one
    int activeSessions = refreshTokenRepository.countByUserIdAndRevokedFalseAndExpiresAtAfter(
      user.getId(), LocalDateTime.now()
    );

    if (activeSessions >= maxConcurrentSessions) {
      log.warn("User {} has {} active sessions (max: {}). Revoking oldest session.",
        user.getUsername(), activeSessions, maxConcurrentSessions);
      revokeOldestSession(user.getId());
    }

    // Create NEW session
    RefreshToken refreshToken = RefreshToken.builder()
      .tokenHash(tokenHash)
      .accessTokenHash(accessTokenHash)
      .userId(user.getId())  // ← Store just the ID!
      .deviceInfo(deviceInfo)
      .ipAddress(ipAddress)
      .issuedAt(LocalDateTime.now())
      .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
      .lastUsedAt(LocalDateTime.now())
      .revoked(false)
      .build();

    refreshToken = refreshTokenRepository.save(refreshToken);
    log.info("✅ New session created: {}", refreshToken.getId());

    return refreshToken;
  }

  /**
   * Find an existing active session for the same device/browser.
   * 
   * Matches sessions by:
   * 1. Same userId
   * 2. Same deviceInfo (browser/OS)
   * 3. Same IP address (or close enough)
   * 4. Not revoked
   * 5. Not expired
   * 
   * @param userId The user ID
   * @param deviceInfo Device fingerprint string
   * @param ipAddress Client IP address
   * @return Existing session if found, null otherwise
   */
  private RefreshToken findExistingSession(String userId, String deviceInfo, String ipAddress) {
    // Use repository query for efficiency
    return refreshTokenRepository
      .findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
        userId, deviceInfo, ipAddress, LocalDateTime.now()
      )
      .orElse(null);
  }

  /**
   * Validate a refresh token.
   *
   * @param token The JWT refresh token string
   * @return RefreshToken entity if valid, empty otherwise
   */
  public RefreshToken validateRefreshToken(String token) {
    String tokenHash = hashToken(token);
    
    return refreshTokenRepository.findByTokenHash(tokenHash)
      .filter(RefreshToken::isValid)
      .map(rt -> {
        // Update last used timestamp
        rt.setLastUsedAt(LocalDateTime.now());
        return refreshTokenRepository.save(rt);
      })
      .orElse(null);
  }

  /**
   * Update the access token hash for a refresh token.
   * Called when access token is refreshed - allows instant revocation of the new token.
   *
   * @param refreshToken The RefreshToken entity
   * @param newAccessToken The new JWT access token string
   */
  @Transactional
  public void updateAccessTokenHash(RefreshToken refreshToken, String newAccessToken) {
    String newAccessTokenHash = hashToken(newAccessToken);
    refreshToken.setAccessTokenHash(newAccessTokenHash);
    refreshTokenRepository.save(refreshToken);
    log.debug("Updated access token hash for session: {}", refreshToken.getId());
  }

  /**
   * Revoke a specific refresh token (logout from one device).
   */
  @Transactional
  public void revokeToken(String tokenId) {
    refreshTokenRepository.findById(tokenId).ifPresent(token -> {
      token.revoke();
      refreshTokenRepository.save(token);
      log.info("Revoked refresh token: {}", tokenId);
    });
  }

  /**
   * Revoke a token by its hash (used during logout).
   */
  @Transactional
  public void revokeTokenByHash(String token) {
    String tokenHash = hashToken(token);
    refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
      rt.revoke();
      refreshTokenRepository.save(rt);
      log.info("Revoked refresh token for userId: {}", rt.getUserId());
    });
  }

  /**
   * Revoke all tokens for a user by userId (logout from all devices).
   */
  @Transactional
  public void revokeAllTokensForUser(String userId) {
    List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
    tokens.forEach(RefreshToken::revoke);
    refreshTokenRepository.saveAll(tokens);
    log.info("Revoked {} tokens for userId: {}", tokens.size(), userId);
  }

  /**
   * Get all active sessions for a user by userId.
   * Caller must fetch User separately if needed.
   */
  public List<RefreshToken> getActiveSessions(String userId) {
    return refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtAfter(
      userId, LocalDateTime.now()
    );
  }

  /**
   * Revoke the oldest active session for a user by userId.
   * Called when user exceeds max concurrent sessions.
   */
  private void revokeOldestSession(String userId) {
    List<RefreshToken> activeSessions = refreshTokenRepository
      .findByUserIdAndRevokedFalseAndExpiresAtAfter(userId, LocalDateTime.now());

    if (!activeSessions.isEmpty()) {
      // Sort by issuedAt and revoke the oldest
      activeSessions.stream()
        .min((t1, t2) -> t1.getIssuedAt().compareTo(t2.getIssuedAt()))
        .ifPresent(oldest -> {
          oldest.revoke();
          refreshTokenRepository.save(oldest);
          log.info("Revoked oldest session (issued: {}) for userId: {}",
            oldest.getIssuedAt(), userId);
        });
    }
  }

  /**
   * Hash a token using SHA-256.
   * We never store plain tokens - only their hashes.
   */
  /**
   * Hash a token using SHA-256.
   * This is used for secure token storage - never store plain tokens in database!
   */
  public String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }

  /**
   * Extract device info from User-Agent header.
   */
  private String extractDeviceInfo(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    if (userAgent == null || userAgent.isEmpty()) {
      return "Unknown Device";
    }

    // Simple parsing - you could use a library like user-agent-utils for better parsing
    if (userAgent.contains("Chrome")) {
      return "Chrome on " + extractOS(userAgent);
    } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
      return "Safari on " + extractOS(userAgent);
    } else if (userAgent.contains("Firefox")) {
      return "Firefox on " + extractOS(userAgent);
    } else if (userAgent.contains("Edge")) {
      return "Edge on " + extractOS(userAgent);
    }
    return "Browser on " + extractOS(userAgent);
  }

  /**
   * Extract OS from User-Agent.
   */
  private String extractOS(String userAgent) {
    if (userAgent.contains("Windows")) return "Windows";
    if (userAgent.contains("Macintosh") || userAgent.contains("Mac OS")) return "macOS";
    if (userAgent.contains("Linux")) return "Linux";
    if (userAgent.contains("iPhone")) return "iPhone";
    if (userAgent.contains("iPad")) return "iPad";
    if (userAgent.contains("Android")) return "Android";
    return "Unknown OS";
  }

  /**
   * Extract IP address from request.
   * Handles X-Forwarded-For header for proxies/load balancers.
   */
  private String extractIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      // X-Forwarded-For can contain multiple IPs, take the first (client IP)
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}


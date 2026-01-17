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
   * If user has too many active sessions, revoke the oldest one.
   *
   * @param token The JWT refresh token string
   * @param user The user
   * @param request HTTP request for device/IP info
   * @return The saved RefreshToken entity
   */
  @Transactional
  public RefreshToken createRefreshToken(String token, User user, HttpServletRequest request) {
    log.info("Creating refresh token for user: {}", user.getUsername());

    // Check if user has too many active sessions
    int activeSessions = refreshTokenRepository.countByUserAndRevokedFalseAndExpiresAtAfter(
      user, LocalDateTime.now()
    );

    if (activeSessions >= maxConcurrentSessions) {
      log.warn("User {} has {} active sessions (max: {}). Revoking oldest session.",
        user.getUsername(), activeSessions, maxConcurrentSessions);
      revokeOldestSession(user);
    }

    // Extract device and IP info
    String deviceInfo = extractDeviceInfo(request);
    String ipAddress = extractIpAddress(request);

    // Hash the token (NEVER store plain tokens!)
    String tokenHash = hashToken(token);

    // Create and save the refresh token
    RefreshToken refreshToken = RefreshToken.builder()
      .tokenHash(tokenHash)
      .user(user)
      .deviceInfo(deviceInfo)
      .ipAddress(ipAddress)
      .issuedAt(LocalDateTime.now())
      .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
      .lastUsedAt(LocalDateTime.now())
      .revoked(false)
      .build();

    refreshToken = refreshTokenRepository.save(refreshToken);
    log.info("Refresh token created with id: {}", refreshToken.getId());

    return refreshToken;
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
      log.info("Revoked refresh token for user: {}", rt.getUser().getUsername());
    });
  }

  /**
   * Revoke all tokens for a user (logout from all devices).
   */
  @Transactional
  public void revokeAllTokensForUser(User user) {
    List<RefreshToken> tokens = refreshTokenRepository.findByUser(user);
    tokens.forEach(RefreshToken::revoke);
    refreshTokenRepository.saveAll(tokens);
    log.info("Revoked {} tokens for user: {}", tokens.size(), user.getUsername());
  }

  /**
   * Get all active sessions for a user.
   */
  public List<RefreshToken> getActiveSessions(User user) {
    return refreshTokenRepository.findByUserAndRevokedFalseAndExpiresAtAfter(
      user, LocalDateTime.now()
    );
  }

  /**
   * Revoke the oldest active session for a user.
   * Called when user exceeds max concurrent sessions.
   */
  private void revokeOldestSession(User user) {
    List<RefreshToken> activeSessions = refreshTokenRepository
      .findByUserAndRevokedFalseAndExpiresAtAfter(user, LocalDateTime.now());

    if (!activeSessions.isEmpty()) {
      // Sort by issuedAt and revoke the oldest
      activeSessions.stream()
        .min((t1, t2) -> t1.getIssuedAt().compareTo(t2.getIssuedAt()))
        .ifPresent(oldest -> {
          oldest.revoke();
          refreshTokenRepository.save(oldest);
          log.info("Revoked oldest session (issued: {}) for user: {}",
            oldest.getIssuedAt(), user.getUsername());
        });
    }
  }

  /**
   * Hash a token using SHA-256.
   * We never store plain tokens - only their hashes.
   */
  private String hashToken(String token) {
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


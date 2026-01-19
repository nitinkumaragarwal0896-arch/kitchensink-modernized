package com.modernizedkitechensink.kitchensinkmodernized.controller.auth;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.RefreshToken;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.RefreshTokenRepository;
import com.modernizedkitechensink.kitchensinkmodernized.repository.UserRepository;
import com.modernizedkitechensink.kitchensinkmodernized.service.RefreshTokenService;
import com.modernizedkitechensink.kitchensinkmodernized.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Session Controller - Manages active user sessions.
 *
 * Provides endpoints to:
 * - View all active sessions for current user
 * - Revoke specific sessions (logout from specific device)
 * - View session details (device, IP, last activity)
 */
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

  private final RefreshTokenService refreshTokenService;
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final TokenBlacklistService blacklistService;

  /**
   * Get all active sessions for current user.
   *
   * GET /api/v1/sessions?currentRefreshToken=xxx
   * Returns: List of active sessions with device info, IP, last activity
   *          and marks which one is the "current" session
   */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> getActiveSessions(
    Authentication authentication,
    @RequestParam(required = false) String currentRefreshToken
  ) {
    String username = authentication.getName();
    User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new RuntimeException("User not found"));
    
    List<RefreshToken> sessions = refreshTokenService.getActiveSessions(user);
    
    // Identify current session by refresh token hash
    String currentSessionId = null;
    if (currentRefreshToken != null && !currentRefreshToken.isEmpty()) {
      String tokenHash = refreshTokenService.hashToken(currentRefreshToken);
      Optional<RefreshToken> currentToken = refreshTokenRepository.findByTokenHash(tokenHash);
      if (currentToken.isPresent() && !currentToken.get().isRevoked()) {
        currentSessionId = currentToken.get().getId();
      }
    }
    
    String finalCurrentSessionId = currentSessionId;
    List<Map<String, Object>> response = sessions.stream()
      .map(session -> {
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("id", session.getId());
        sessionMap.put("deviceInfo", session.getDeviceInfo());
        sessionMap.put("ipAddress", session.getIpAddress());
        sessionMap.put("lastUsedAt", session.getLastUsedAt().toString());
        sessionMap.put("issuedAt", session.getIssuedAt().toString());
        sessionMap.put("expiresAt", session.getExpiresAt().toString());
        sessionMap.put("isCurrent", session.getId().equals(finalCurrentSessionId));
        return sessionMap;
      })
      .collect(Collectors.toList());
    
    log.info("Retrieved {} active sessions for user: {} (current: {})", 
             response.size(), username, currentSessionId);
    return ResponseEntity.ok(response);
  }

  /**
   * Revoke a specific session (logout from one device).
   *
   * DELETE /api/v1/sessions/{sessionId}?currentRefreshToken=xxx
   * 
   * NOW WITH INSTANT LOGOUT FOR ALL DEVICES!
   * - Retrieves the access token hash from the RefreshToken entity
   * - Blacklists the access token in Redis (INSTANT LOGOUT!)
   * - Revokes the refresh token in MongoDB
   * - User is logged out immediately on next request (even from other devices)
   */
  @DeleteMapping("/{sessionId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> revokeSession(
    @PathVariable String sessionId,
    @RequestParam(required = false) String currentRefreshToken,
    Authentication authentication,
    HttpServletRequest request
  ) {
    String username = authentication.getName();
    
    // Check if this is the current session
    boolean isCurrentSession = false;
    if (currentRefreshToken != null && !currentRefreshToken.isEmpty()) {
      String tokenHash = refreshTokenService.hashToken(currentRefreshToken);
      Optional<RefreshToken> currentToken = refreshTokenRepository.findByTokenHash(tokenHash);
      if (currentToken.isPresent() && currentToken.get().getId().equals(sessionId)) {
        isCurrentSession = true;
      }
    }
    
    // ✅ NEW: Get the session to revoke and blacklist its access token
    Optional<RefreshToken> sessionToRevoke = refreshTokenRepository.findById(sessionId);
    if (sessionToRevoke.isPresent()) {
      RefreshToken session = sessionToRevoke.get();
      
      // Blacklist the access token for INSTANT logout (even from other devices!)
      String accessTokenHash = session.getAccessTokenHash();
      if (accessTokenHash != null && !accessTokenHash.isEmpty()) {
        blacklistService.blacklistTokenByHash(accessTokenHash);
        log.info("🚫 Blacklisted access token (hash) for INSTANT logout: user={}, session={}, device={}", 
                 username, sessionId, session.getDeviceInfo());
      }
      
      // Revoke the refresh token (prevent new access tokens)
      refreshTokenService.revokeToken(sessionId);
    }
    
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Session revoked successfully");
    response.put("isCurrentSession", isCurrentSession);
    response.put("instantLogout", true); // NOW ALWAYS TRUE - instant logout for all devices!
    
    if (isCurrentSession) {
      log.warn("User {} revoked their CURRENT session {} - Logged out INSTANTLY", 
               username, sessionId);
    } else {
      log.info("User {} revoked session {} from another device - INSTANT logout!", username, sessionId);
    }
    
    return ResponseEntity.ok(response);
  }
  
  /**
   * Extract access token from Authorization header.
   */
  private String extractAccessToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}


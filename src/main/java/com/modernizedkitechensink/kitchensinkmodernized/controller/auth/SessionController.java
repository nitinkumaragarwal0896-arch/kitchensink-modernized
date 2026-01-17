package com.modernizedkitechensink.kitchensinkmodernized.controller.auth;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.RefreshToken;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.UserRepository;
import com.modernizedkitechensink.kitchensinkmodernized.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  /**
   * Get all active sessions for current user.
   *
   * GET /api/v1/sessions
   * Returns: List of active sessions with device info, IP, last activity
   */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> getActiveSessions(Authentication authentication) {
    String username = authentication.getName();
    User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new RuntimeException("User not found"));
    
    List<RefreshToken> sessions = refreshTokenService.getActiveSessions(user);
    
    List<Map<String, Object>> response = sessions.stream()
      .map(session -> {
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("id", session.getId());
        sessionMap.put("deviceInfo", session.getDeviceInfo());
        sessionMap.put("ipAddress", session.getIpAddress());
        sessionMap.put("lastUsedAt", session.getLastUsedAt().toString());
        sessionMap.put("issuedAt", session.getIssuedAt().toString());
        sessionMap.put("expiresAt", session.getExpiresAt().toString());
        return sessionMap;
      })
      .collect(Collectors.toList());
    
    log.info("Retrieved {} active sessions for user: {}", response.size(), username);
    return ResponseEntity.ok(response);
  }

  /**
   * Revoke a specific session (logout from one device).
   *
   * DELETE /api/v1/sessions/{sessionId}
   */
  @DeleteMapping("/{sessionId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> revokeSession(
    @PathVariable String sessionId,
    Authentication authentication
  ) {
    String username = authentication.getName();
    refreshTokenService.revokeToken(sessionId);
    log.info("Session {} revoked by user: {}", sessionId, username);
    
    return ResponseEntity.ok(Map.of("message", "Session revoked successfully"));
  }
}


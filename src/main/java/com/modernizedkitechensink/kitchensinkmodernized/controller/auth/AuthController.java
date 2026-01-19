package com.modernizedkitechensink.kitchensinkmodernized.controller.auth;

import com.modernizedkitechensink.kitchensinkmodernized.dto.AuthRequest;
import com.modernizedkitechensink.kitchensinkmodernized.dto.AuthResponse;
import com.modernizedkitechensink.kitchensinkmodernized.dto.RegisterRequest;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.RefreshToken;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.Role;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.RoleRepository;
import com.modernizedkitechensink.kitchensinkmodernized.repository.UserRepository;
import com.modernizedkitechensink.kitchensinkmodernized.security.JwtTokenProvider;
import com.modernizedkitechensink.kitchensinkmodernized.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication Controller - Handles login, registration, token refresh, and session management.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenService refreshTokenService;

  /**
   * Login endpoint - validates credentials and returns JWT tokens.
   * Also creates a refresh token entry to track the session.
   */
  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
    log.info("Login attempt for user: {}", request.getUsername());

    try {
      // Step 1: Authenticate using Spring Security
      Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
          request.getUsername(),
          request.getPassword()
        )
      );

      // Step 2: Generate tokens
      String accessToken = jwtTokenProvider.generateAccessToken(authentication);
      String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

      // Step 3: Store refresh token in database (tracks session)
      User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
      refreshTokenService.createRefreshToken(refreshToken, accessToken, user, httpRequest);

      // Step 4: Update last login time
      user.recordSuccessfulLogin();
      userRepository.save(user);

      log.info("Login successful for user: {}", request.getUsername());

      // Step 5: Return tokens
      return ResponseEntity.ok(AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(900)  // 15 minutes in seconds
        .username(request.getUsername())
        .build());

    } catch (BadCredentialsException e) {
      // Record failed login attempt
      userRepository.findByUsername(request.getUsername())
        .ifPresent(user -> {
          user.recordFailedLogin();
          userRepository.save(user);
        });

      log.warn("Login failed for user: {}", request.getUsername());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "Invalid username or password"));
    }
  }

  /**
   * Registration endpoint - creates new user account.
   */
  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    log.info("Registration attempt for user: {}", request.getUsername());

    // Check if username already exists
    if (userRepository.existsByUsername(request.getUsername())) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "Username already taken"));
    }

    // Check if email already exists
    if (userRepository.existsByEmail(request.getEmail())) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "Email already registered"));
    }

    // Get default USER role
    Role userRole = roleRepository.findByName("USER")
      .orElseThrow(() -> new RuntimeException("Default role USER not found"));

    // Create new user
    User user = User.builder()
      .username(request.getUsername())
      .email(request.getEmail())
      .password(passwordEncoder.encode(request.getPassword()))
      .roles(Set.of(userRole))
      .enabled(true)
      .accountNonLocked(true)
      .accountNonExpired(true)
      .credentialsNonExpired(true)
      .build();

    userRepository.save(user);
    log.info("User registered successfully: {}", request.getUsername());

    return ResponseEntity.status(HttpStatus.CREATED)
      .body(Map.of("message", "User registered successfully"));
  }

  /**
   * Refresh token endpoint - get new access token using refresh token.
   * Validates that the refresh token exists in database and is not revoked.
   */
  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
    String refreshToken = request.get("refreshToken");

    if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "Invalid or expired refresh token"));
    }

    // Validate refresh token is stored and not revoked
    RefreshToken storedToken = refreshTokenService.validateRefreshToken(refreshToken);
    if (storedToken == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "Refresh token has been revoked or doesn't exist"));
    }

    // Get username from refresh token
    String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

    // Generate new access token
    Authentication authentication = new UsernamePasswordAuthenticationToken(
      username, null, java.util.Collections.emptyList()
    );

    String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);

    // Update the access token hash in the refresh token (for instant revocation)
    refreshTokenService.updateAccessTokenHash(storedToken, newAccessToken);

    return ResponseEntity.ok(Map.of(
      "accessToken", newAccessToken,
      "tokenType", "Bearer",
      "expiresIn", 900
    ));
  }

  /**
   * Logout endpoint - revokes the refresh token for current session.
   */
  @PostMapping("/logout")
  public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
    String refreshToken = request.get("refreshToken");
    
    if (refreshToken != null) {
      refreshTokenService.revokeTokenByHash(refreshToken);
      log.info("User logged out successfully");
    }
    
    return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
  }

  /**
   * Logout from all devices - revokes all refresh tokens for the user.
   */
  @PostMapping("/logout-all")
  public ResponseEntity<?> logoutAll(Authentication authentication) {
    String username = authentication.getName();
    User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new RuntimeException("User not found"));
    
    refreshTokenService.revokeAllTokensForUser(user);
    log.info("User {} logged out from all devices", username);
    
    return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
  }

  /**
   * Get current user endpoint - returns details of authenticated user.
   */
  @GetMapping("/me")
  public ResponseEntity<?> getCurrentUser() {
    // Get the authenticated user from SecurityContext
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    // Load user details from database
    User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new RuntimeException("User not found"));

    // Extract role names
    Set<String> roleNames = user.getRoles().stream()
      .map(Role::getName)
      .collect(Collectors.toSet());

    // Extract all permissions from all roles
    Set<String> permissions = user.getRoles().stream()
      .flatMap(role -> role.getPermissions().stream())
      .collect(Collectors.toSet());

    // Return user details
    return ResponseEntity.ok(Map.of(
      "id", user.getId(),
      "username", user.getUsername(),
      "email", user.getEmail(),
      "roles", roleNames,
      "permissions", permissions,
      "enabled", user.isEnabled(),
      "accountNonLocked", user.isAccountNonLocked()
    ));
  }
}


package com.modernizedkitechensink.kitchensinkmodernized.controller;

import com.modernizedkitechensink.kitchensinkmodernized.dto.AuthRequest;
import com.modernizedkitechensink.kitchensinkmodernized.dto.AuthResponse;
import com.modernizedkitechensink.kitchensinkmodernized.dto.RegisterRequest;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.Role;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.RoleRepository;
import com.modernizedkitechensink.kitchensinkmodernized.repository.UserRepository;
import com.modernizedkitechensink.kitchensinkmodernized.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Authentication Controller - Handles login, registration, and token refresh.
 *
 * All endpoints here are PUBLIC (no JWT required).
 * See SecurityConfig where "/api/v1/auth/**" is permitted.
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

  /**
   * Login endpoint - validates credentials and returns JWT tokens.
   *
   * POST /api/v1/auth/login
   * Body: { "username": "admin", "password": "admin123" }
   */
  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
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

      // Step 3: Update last login time
      userRepository.findByUsername(request.getUsername())
        .ifPresent(user -> {
          user.recordSuccessfulLogin();
          userRepository.save(user);
        });

      log.info("Login successful for user: {}", request.getUsername());

      // Step 4: Return tokens
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
   *
   * POST /api/v1/auth/register
   * Body: { "username": "john", "email": "john@example.com", "password": "secret123" }
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
      .password(passwordEncoder.encode(request.getPassword()))  // Hash password!
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
   *
   * POST /api/v1/auth/refresh
   * Body: { "refreshToken": "eyJhbGciOiJIUzI1NiIs..." }
   */
  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
    String refreshToken = request.get("refreshToken");

    if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "Invalid or expired refresh token"));
    }

    // Get username from refresh token
    String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

    // Load user and create new authentication
    User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new RuntimeException("User not found"));

    // Generate new access token (we need to create authentication manually)
    Authentication authentication = new UsernamePasswordAuthenticationToken(
      username, null, java.util.Collections.emptyList()
    );

    String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);

    return ResponseEntity.ok(Map.of(
      "accessToken", newAccessToken,
      "tokenType", "Bearer",
      "expiresIn", 900
    ));
  }
}

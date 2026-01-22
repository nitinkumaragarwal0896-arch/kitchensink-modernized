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
import com.modernizedkitechensink.kitchensinkmodernized.util.PasswordValidator;
import com.modernizedkitechensink.kitchensinkmodernized.validation.EmailValidationService;
import com.modernizedkitechensink.kitchensinkmodernized.validation.PhoneValidationService;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
  private final EmailValidationService emailValidationService;
  private final PhoneValidationService phoneValidationService;
  private final UserDetailsService userDetailsService;
  private final com.modernizedkitechensink.kitchensinkmodernized.service.PasswordResetService passwordResetService;
  private final com.modernizedkitechensink.kitchensinkmodernized.service.TokenBlacklistService tokenBlacklistService;

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
        .expiresIn(3600)  // 1 hour in seconds (matches jwt.access-token-expiration)
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

    // Validate email format using our custom validation service
    EmailValidationService.ValidationResult emailValidation = emailValidationService.validate(request.getEmail());
    if (!emailValidation.isValid()) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", emailValidation.getErrorMessage()));
    }

    // Validate phone format using our custom validation service
    PhoneValidationService.ValidationResult phoneValidation = phoneValidationService.validate(request.getPhoneNumber());
    if (!phoneValidation.isValid()) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", phoneValidation.getErrorMessage()));
    }

    // Get default USER role
    Role userRole = roleRepository.findByName("USER")
      .orElseThrow(() -> new RuntimeException("Default role USER not found"));

    // Create new user
    User user = User.builder()
      .username(request.getUsername())
      .email(request.getEmail())
      .password(passwordEncoder.encode(request.getPassword()))
      .firstName(request.getFirstName())
      .lastName(request.getLastName())
      .phoneNumber(request.getPhoneNumber())
      .roles(Set.of(userRole))
      .enabled(true)
      .accountNonLocked(true)
      .accountNonExpired(true)
      .credentialsNonExpired(true)
      .build();

    userRepository.save(user);
    log.info("User registered successfully: {} {} ({})", request.getFirstName(), request.getLastName(), request.getUsername());

    return ResponseEntity.status(HttpStatus.CREATED)
      .body(Map.of("message", "User registered successfully"));
  }

  /**
   * Refresh token endpoint - get new access token using refresh token.
   * Validates that the refresh token exists in database and is not revoked.
   * 
   * CRITICAL FIX: Load user from database to get ACTUAL authorities.
   * Without this, refreshed tokens have ZERO permissions, causing 403 errors.
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

    // ✅ FIX: Load user with ACTUAL authorities from database
    // This ensures the new access token contains all user permissions
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

    // ✅ FIX: Create Authentication with REAL authorities
    Authentication authentication = new UsernamePasswordAuthenticationToken(
      userDetails, 
      null, 
      userDetails.getAuthorities() // ← Real permissions (member:read, member:write, etc.)
    );

    // Generate new access token with correct permissions
    String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);

    // Update the access token hash in the refresh token (for instant revocation)
    refreshTokenService.updateAccessTokenHash(storedToken, newAccessToken);

    log.debug("Access token refreshed for user: {} with {} permissions", 
      username, userDetails.getAuthorities().size());

    return ResponseEntity.ok(Map.of(
      "accessToken", newAccessToken,
      "tokenType", "Bearer",
      "expiresIn", 3600  // ✅ FIX: 1 hour (3600s), not 15 min (900s)
    ));
  }

  /**
   * Logout endpoint - revokes refresh token AND blacklists access token.
   * 
   * This ensures:
   * 1. Instant logout (access token blacklisted in Redis)
   * 2. Can't get new tokens (refresh token revoked in MongoDB)
   * 3. Other tabs on same device log out immediately
   */
  @PostMapping("/logout")
  public ResponseEntity<?> logout(
      @RequestBody Map<String, String> request,
      @RequestHeader(value = "Authorization", required = false) String authHeader
  ) {
    String refreshToken = request.get("refreshToken");
    
    // Step 1: Revoke refresh token (prevent new access tokens)
    if (refreshToken != null) {
      refreshTokenService.revokeTokenByHash(refreshToken);
      log.info("Refresh token revoked");
    }
    
    // Step 2: Blacklist current access token (instant logout) ✅ NEW!
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String accessToken = authHeader.substring(7);
      tokenBlacklistService.blacklistToken(accessToken);
      log.info("Access token blacklisted for instant logout");
    }
    
    return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
  }

  /**
   * Logout from all devices - revokes all refresh tokens and blacklists all access tokens.
   * CRITICAL: Blacklist access tokens FIRST, then revoke refresh tokens.
   */
  @PostMapping("/logout-all")
  public ResponseEntity<?> logoutAll(Authentication authentication) {
    String username = authentication.getName();
    User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new RuntimeException("User not found"));
    
    // Step 1: Blacklist all access tokens in Redis (instant logout)
    tokenBlacklistService.blacklistAllUserTokens(user.getId());
    
    // Step 2: Revoke all refresh tokens in MongoDB (prevent new access tokens)
    refreshTokenService.revokeAllTokensForUser(user.getId());  // ← Changed to userId
    
    log.info("User {} logged out from all devices (access tokens blacklisted + refresh tokens revoked)", username);
    
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

  /**
   * Request password reset.
   * Sends reset email with token if email exists.
   * Always returns success (security: don't reveal if email exists).
   * 
   * @param request Map containing "email"
   * @return Success message
   */
  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(
    @RequestBody Map<String, String> request,
    HttpServletRequest httpRequest
  ) {
    String email = request.get("email");
    
    if (email == null || email.trim().isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
    }

    log.info("Password reset requested for email: {}", email);
    
    passwordResetService.requestPasswordReset(email.trim(), httpRequest);
    
    // Always return success (don't reveal if email exists)
    return ResponseEntity.ok(Map.of(
      "message", "If an account exists with this email, you will receive a password reset link shortly."
    ));
  }

  /**
   * Validate password reset token.
   * 
   * @param request Map containing "token"
   * @return Validation result
   */
  @PostMapping("/validate-reset-token")
  public ResponseEntity<?> validateResetToken(@RequestBody Map<String, String> request) {
    String token = request.get("token");
    
    if (token == null || token.trim().isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
    }

    boolean isValid = passwordResetService.validateToken(token.trim());
    
    if (isValid) {
      return ResponseEntity.ok(Map.of("message", "Token is valid"));
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "Invalid or expired reset token"));
    }
  }

  /**
   * Reset password using token.
   * 
   * @param request Map containing "token" and "newPassword"
   * @return Success or error message
   */
  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
    String token = request.get("token");
    String newPassword = request.get("newPassword");
    
    if (token == null || token.trim().isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
    }
    
    // ⚠️ SECURITY: Validate password strength using centralized validator
    if (newPassword == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
    }
    
    PasswordValidator.ValidationResult passwordValidation = PasswordValidator.validate(newPassword);
    if (!passwordValidation.isValid()) {
      return ResponseEntity.badRequest().body(Map.of("error", passwordValidation.getErrorMessage()));
    }

    boolean success;
    try {
      success = passwordResetService.resetPassword(token.trim(), newPassword);
    } catch (IllegalArgumentException e) {
      // Password validation failed in service layer (defense in depth)
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
    
    if (success) {
      log.info("Password reset successful");
      return ResponseEntity.ok(Map.of("message", "Password reset successful. You can now login with your new password."));
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "Invalid or expired reset token"));
    }
  }
}


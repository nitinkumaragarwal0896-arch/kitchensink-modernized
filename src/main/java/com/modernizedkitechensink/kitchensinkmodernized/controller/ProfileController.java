package com.modernizedkitechensink.kitchensinkmodernized.controller;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.UserRepository;
import com.modernizedkitechensink.kitchensinkmodernized.service.RefreshTokenService;
import com.modernizedkitechensink.kitchensinkmodernized.service.TokenBlacklistService;
import com.modernizedkitechensink.kitchensinkmodernized.util.PasswordValidator;
import com.modernizedkitechensink.kitchensinkmodernized.validation.EmailValidationService;
import com.modernizedkitechensink.kitchensinkmodernized.validation.PhoneValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Profile Controller - Manage logged-in user's own profile.
 * 
 * Endpoints:
 * - GET /api/v1/profile - Get current user's profile
 * - PUT /api/v1/profile - Update current user's profile
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

  private final UserRepository userRepository;
  private final EmailValidationService emailValidationService;
  private final PhoneValidationService phoneValidationService;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenService refreshTokenService;
  private final TokenBlacklistService tokenBlacklistService;

  /**
   * Get current user's profile.
   * 
   * @return User profile information
   */
  @GetMapping
  public ResponseEntity<?> getProfile() {
    String username = getCurrentUsername();
    log.debug("Fetching profile for user: {}", username);

    User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new RuntimeException("User not found"));

    // Return sanitized user data (no password!)
    // Use HashMap instead of Map.of() to allow null values
    Map<String, Object> profileData = new java.util.HashMap<>();
    profileData.put("id", user.getId());
    profileData.put("username", user.getUsername());
    profileData.put("email", user.getEmail());
    profileData.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
    profileData.put("lastName", user.getLastName() != null ? user.getLastName() : "");
    profileData.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
    profileData.put("enabled", user.isEnabled());
    profileData.put("accountNonLocked", user.isAccountNonLocked());
    profileData.put("createdAt", user.getCreatedAt());
    profileData.put("lastLoginDate", user.getLastLoginDate());
    
    // Add roles and permissions
    profileData.put("roles", user.getRoles().stream().map(role -> role.getName()).toList());
    // Extract permissions from all roles
    List<String> permissions = user.getRoles().stream()
      .flatMap(role -> role.getPermissions().stream())
      .distinct()
      .sorted()
      .toList();
    profileData.put("permissions", permissions);
    
    return ResponseEntity.ok(profileData);
  }

  /**
   * Update current user's profile.
   * Only allows updating: firstName, lastName, email, phoneNumber.
   * Cannot change username or password through this endpoint.
   * 
   * @param updates Map of fields to update
   * @return Updated profile
   */
  @PutMapping
  public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates) {
    String username = getCurrentUsername();
    log.info("Updating profile for user: {}", username);

    User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new RuntimeException("User not found"));

    // Update firstName
    if (updates.containsKey("firstName")) {
      String firstName = updates.get("firstName");
      if (firstName != null && !firstName.trim().isEmpty()) {
        user.setFirstName(firstName.trim());
      }
    }

    // Update lastName
    if (updates.containsKey("lastName")) {
      String lastName = updates.get("lastName");
      if (lastName != null && !lastName.trim().isEmpty()) {
        user.setLastName(lastName.trim());
      }
    }

    // Update email (with validation)
    if (updates.containsKey("email")) {
      String newEmail = updates.get("email");
      
      // Validate email format
      EmailValidationService.ValidationResult emailValidation = emailValidationService.validate(newEmail);
      if (!emailValidation.isValid()) {
        return ResponseEntity.badRequest()
          .body(Map.of("error", emailValidation.getErrorMessage()));
      }

      // Check if email is already taken by another user
      if (!newEmail.equals(user.getEmail())) {
        if (userRepository.existsByEmail(newEmail)) {
          return ResponseEntity.badRequest()
            .body(Map.of("error", "Email already in use by another account"));
        }
        user.setEmail(newEmail);
      }
    }

    // Update phoneNumber (with validation)
    if (updates.containsKey("phoneNumber")) {
      String newPhone = updates.get("phoneNumber");
      
      // Validate phone format
      PhoneValidationService.ValidationResult phoneValidation = phoneValidationService.validate(newPhone);
      if (!phoneValidation.isValid()) {
        return ResponseEntity.badRequest()
          .body(Map.of("error", phoneValidation.getErrorMessage()));
      }

      user.setPhoneNumber(newPhone);
    }

    // Save updated user
    userRepository.save(user);
    log.info("Profile updated successfully for user: {}", username);

    // Use HashMap to allow null values
    Map<String, Object> userData = new java.util.HashMap<>();
    userData.put("id", user.getId());
    userData.put("username", user.getUsername());
    userData.put("email", user.getEmail());
    userData.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
    userData.put("lastName", user.getLastName() != null ? user.getLastName() : "");
    userData.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
    
    Map<String, Object> response = new java.util.HashMap<>();
    response.put("message", "Profile updated successfully");
    response.put("user", userData);
    
    return ResponseEntity.ok(response);
  }

  /**
   * Change password for logged-in user.
   * Requires current password for verification.
   * Logs out all sessions after password change.
   * 
   * @param request Map containing currentPassword and newPassword
   * @return Success message
   */
  @PostMapping("/change-password")
  public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
    String username = getCurrentUsername();
    log.info("Password change requested for user: {}", username);

    // Validate request
    String currentPassword = request.get("currentPassword");
    String newPassword = request.get("newPassword");

    if (currentPassword == null || currentPassword.trim().isEmpty()) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "Current password is required"));
    }

    if (newPassword == null || newPassword.trim().isEmpty()) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "New password is required"));
    }

    // Validate new password strength
    PasswordValidator.ValidationResult passwordValidation = PasswordValidator.validate(newPassword);
    if (!passwordValidation.isValid()) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", passwordValidation.getErrorMessage()));
    }

    // Get user
    User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new RuntimeException("User not found"));

    // Verify current password
    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      log.warn("Failed password change attempt for user: {} - Invalid current password", username);
      return ResponseEntity.badRequest()
        .body(Map.of("error", "Current password is incorrect"));
    }

    // Check if new password is same as current
    if (passwordEncoder.matches(newPassword, user.getPassword())) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "New password must be different from current password"));
    }

    // Update password
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    
    log.info("Password changed successfully for user: {}", username);

    // Revoke all sessions and blacklist all tokens for security
    // CRITICAL: Blacklist FIRST (while tokens are still revoked=false), then revoke
    try {
      // Step 1: Blacklist all access tokens in Redis (instant logout)
      tokenBlacklistService.blacklistAllUserTokens(user.getId());
      
      // Step 2: Revoke all refresh tokens in MongoDB (prevent new access tokens)
      refreshTokenService.revokeAllTokensForUser(user.getId());  // ← Changed to userId
      
      log.info("All sessions revoked for user after password change: {}", username);
    } catch (Exception e) {
      log.error("Failed to revoke sessions after password change for user: {}", username, e);
    }

    return ResponseEntity.ok(Map.of(
      "message", "Password changed successfully. Please log in again with your new password.",
      "logoutRequired", true
    ));
  }

  // Password validation moved to centralized PasswordValidator utility class

  /**
   * Get current authenticated username from SecurityContext.
   */
  private String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }
}


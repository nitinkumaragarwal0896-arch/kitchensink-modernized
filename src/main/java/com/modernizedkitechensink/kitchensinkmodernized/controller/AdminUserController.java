package com.modernizedkitechensink.kitchensinkmodernized.controller;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.Role;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.RoleRepository;
import com.modernizedkitechensink.kitchensinkmodernized.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin User Management Controller.
 * Requires system:admin permission.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('system:admin')")
public class AdminUserController {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  /**
   * Get current authenticated username.
   */
  private String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }

  /**
   * Check if user is trying to modify their own account.
   */
  private boolean isSelfModification(String targetUserId) {
    String currentUsername = getCurrentUsername();
    User targetUser = userRepository.findById(targetUserId).orElse(null);
    return targetUser != null && targetUser.getUsername().equals(currentUsername);
  }

  /**
   * Get all users with pagination.
   */
  @GetMapping
  public ResponseEntity<?> getAllUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {

    Page<User> users = userRepository.findAll(PageRequest.of(page, size));

    Map<String, Object> response = Map.of(
      "content", users.getContent().stream()
        .map(this::mapUserToResponse)
        .collect(Collectors.toList()),
      "totalElements", users.getTotalElements(),
      "totalPages", users.getTotalPages(),
      "currentPage", users.getNumber(),
      "size", users.getSize()
    );

    return ResponseEntity.ok(response);
  }

  /**
   * Get user by ID.
   */
  @GetMapping("/{id}")
  public ResponseEntity<?> getUserById(@PathVariable String id) {
    return userRepository.findById(id)
      .map(user -> ResponseEntity.ok(mapUserToResponse(user)))
      .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Update user.
   */
  @PutMapping("/{id}")
  public ResponseEntity<?> updateUser(
    @PathVariable String id,
    @Valid @RequestBody Map<String, Object> request) {

    // Prevent self-modification of critical fields
    if (isSelfModification(id)) {
      if (request.containsKey("enabled") || request.containsKey("accountNonLocked")) {
        return ResponseEntity.badRequest()
          .body(Map.of("error", "Cannot modify your own account status"));
      }
    }

    return userRepository.findById(id)
      .map(user -> {
        if (request.containsKey("email")) {
          user.setEmail((String) request.get("email"));
        }
        if (request.containsKey("enabled")) {
          user.setEnabled((Boolean) request.get("enabled"));
        }
        if (request.containsKey("accountNonLocked")) {
          user.setAccountNonLocked((Boolean) request.get("accountNonLocked"));
        }

        userRepository.save(user);
        log.info("User updated: {}", user.getUsername());

        return ResponseEntity.ok(Map.of("message", "User updated successfully"));
      })
      .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Delete user.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteUser(@PathVariable String id) {
    // Prevent self-deletion
    if (isSelfModification(id)) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "Cannot delete your own account"));
    }

    return userRepository.findById(id)
      .map(user -> {
        // Prevent deletion of the last admin
        boolean isAdmin = user.getRoles().stream()
          .anyMatch(role -> role.getName().equals("ADMIN"));
        
        if (isAdmin) {
          long adminCount = userRepository.findAll().stream()
            .filter(u -> u.getRoles().stream()
              .anyMatch(role -> role.getName().equals("ADMIN")))
            .count();
          
          if (adminCount <= 1) {
            return ResponseEntity.badRequest()
              .body(Map.of("error", "Cannot delete the last admin user"));
          }
        }

        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());

        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
      })
      .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Assign roles to user.
   */
  @PostMapping("/{id}/roles")
  public ResponseEntity<?> assignRoles(
    @PathVariable String id,
    @RequestBody Map<String, List<String>> request) {

    return userRepository.findById(id)
      .map(user -> {
        List<String> roleNames = request.get("roles");
        Set<Role> roles = roleNames.stream()
          .map(roleName -> roleRepository.findByName(roleName)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
          .collect(Collectors.toSet());

        user.getRoles().addAll(roles);
        userRepository.save(user);
        log.info("Roles assigned to user {}: {}", user.getUsername(), roleNames);

        return ResponseEntity.ok(Map.of("message", "Roles assigned successfully"));
      })
      .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Remove roles from user.
   */
  @DeleteMapping("/{id}/roles")
  public ResponseEntity<?> removeRoles(
    @PathVariable String id,
    @RequestBody Map<String, List<String>> request) {

    // Prevent removing roles from own account
    if (isSelfModification(id)) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "Cannot remove roles from your own account"));
    }

    return userRepository.findById(id)
      .map(user -> {
        List<String> roleNames = request.get("roles");
        
        // Prevent removing ADMIN role from the last admin
        if (roleNames.contains("ADMIN")) {
          long adminCount = userRepository.findAll().stream()
            .filter(u -> u.getRoles().stream()
              .anyMatch(role -> role.getName().equals("ADMIN")))
            .count();
          
          if (adminCount <= 1) {
            return ResponseEntity.badRequest()
              .body(Map.of("error", "Cannot remove ADMIN role from the last admin"));
          }
        }
        
        user.getRoles().removeIf(role -> roleNames.contains(role.getName()));
        userRepository.save(user);
        log.info("Roles removed from user {}: {}", user.getUsername(), roleNames);

        return ResponseEntity.ok(Map.of("message", "Roles removed successfully"));
      })
      .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Enable user account.
   */
  @PostMapping("/{id}/enable")
  public ResponseEntity<?> enableUser(@PathVariable String id) {
    return userRepository.findById(id)
      .map(user -> {
        user.setEnabled(true);
        userRepository.save(user);
        log.info("User enabled: {}", user.getUsername());

        return ResponseEntity.ok(Map.of("message", "User enabled successfully"));
      })
      .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Disable user account.
   */
  @PostMapping("/{id}/disable")
  public ResponseEntity<?> disableUser(@PathVariable String id) {
    // Prevent self-disabling
    if (isSelfModification(id)) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "Cannot disable your own account"));
    }

    return userRepository.findById(id)
      .map(user -> {
        user.setEnabled(false);
        userRepository.save(user);
        log.info("User disabled: {}", user.getUsername());

        return ResponseEntity.ok(Map.of("message", "User disabled successfully"));
      })
      .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Unlock user account.
   */
  @PostMapping("/{id}/unlock")
  public ResponseEntity<?> unlockUser(@PathVariable String id) {
    return userRepository.findById(id)
      .map(user -> {
        user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        log.info("User unlocked: {}", user.getUsername());

        return ResponseEntity.ok(Map.of("message", "User unlocked successfully"));
      })
      .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Helper method to map User entity to response DTO.
   */
  private Map<String, Object> mapUserToResponse(User user) {
    return Map.of(
      "id", user.getId(),
      "username", user.getUsername(),
      "email", user.getEmail(),
      "enabled", user.isEnabled(),
      "accountNonLocked", user.isAccountNonLocked(),
      "roles", user.getRoles().stream()
        .map(Role::getName)
        .collect(Collectors.toList()),
      "lastLoginDate", user.getLastLoginDate() != null ? user.getLastLoginDate().toString() : "",
      "failedLoginAttempts", user.getFailedLoginAttempts()
    );
  }
}


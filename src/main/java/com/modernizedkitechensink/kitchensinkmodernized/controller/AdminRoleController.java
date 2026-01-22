package com.modernizedkitechensink.kitchensinkmodernized.controller;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.Permission;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.Role;
import com.modernizedkitechensink.kitchensinkmodernized.repository.RoleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Role Management Controller.
 * Requires system:admin permission.
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('system:admin')")
public class AdminRoleController {

  private final RoleRepository roleRepository;

  /**
   * Get all roles.
   */
  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
    List<Role> roles = roleRepository.findAll();
    List<Map<String, Object>> response = roles.stream()
      .map(role -> {
        Map<String, Object> map = new HashMap<>();
        map.put("id", role.getId());
        map.put("name", role.getName());
        map.put("description", role.getDescription() != null ? role.getDescription() : "");
        map.put("permissions", new ArrayList<>(role.getPermissions()));
        map.put("createdAt", role.getCreatedAt() != null ? role.getCreatedAt().toString() : "");
        return map;
      })
      .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  /**
   * Get all available permissions.
   */
  @GetMapping("/permissions")
  public ResponseEntity<List<String>> getAllPermissions() {
    List<String> permissions = Arrays.stream(Permission.values())
      .map(Permission::getPermission)  // Returns "member:read" format
      .collect(Collectors.toList());
    return ResponseEntity.ok(permissions);
  }

  /**
   * Get role by ID.
   */
  @GetMapping("/{id}")
  public ResponseEntity<?> getRoleById(@PathVariable String id) {
    return roleRepository.findById(id)
      .map(role -> {
        Map<String, Object> map = new HashMap<>();
        map.put("id", role.getId());
        map.put("name", role.getName());
        map.put("description", role.getDescription() != null ? role.getDescription() : "");
        map.put("permissions", new ArrayList<>(role.getPermissionsAsEnums()));
        return ResponseEntity.ok(map);
      })
      .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Create new role.
   */
  @PostMapping
  public ResponseEntity<?> createRole(@Valid @RequestBody Map<String, Object> request) {
    String name = (String) request.get("name");
    String description = (String) request.get("description");
    @SuppressWarnings("unchecked")
    List<String> permissionNames = (List<String>) request.get("permissions");

    // Check if role already exists
    if (roleRepository.existsByName(name)) {
      return ResponseEntity.badRequest()
        .body(Map.of("error", "Role already exists"));
    }

    // Permission names are already in "member:read" format
    Role role = Role.builder()
      .name(name)
      .description(description)
      .permissions(new HashSet<>(permissionNames))  // Store as strings
      .build();

    roleRepository.save(role);
    log.info("Role created: {}", name);

    return ResponseEntity.status(HttpStatus.CREATED)
      .body(Map.of("message", "Role created successfully", "id", role.getId()));
  }

  /**
   * Update role.
   */
  @PutMapping("/{id}")
  public ResponseEntity<?> updateRole(
    @PathVariable String id,
    @Valid @RequestBody Map<String, Object> request) {

    return roleRepository.findById(id)
      .map(role -> {
        if (request.containsKey("name")) {
          role.setName((String) request.get("name"));
        }
        if (request.containsKey("description")) {
          role.setDescription((String) request.get("description"));
        }
        if (request.containsKey("permissions")) {
          @SuppressWarnings("unchecked")
          List<String> permissionNames = (List<String>) request.get("permissions");
          role.setPermissions(new HashSet<>(permissionNames));  // Store as strings
        }

        roleRepository.save(role);
        log.info("Role updated: {}", role.getName());

        return ResponseEntity.ok(Map.of("message", "Role updated successfully"));
      })
      .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Delete role.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteRole(@PathVariable String id) {
    return roleRepository.findById(id)
      .map(role -> {
        // Prevent deletion of system roles
        if (role.getName().equals("ADMIN") || role.getName().equals("USER")) {
          return ResponseEntity.badRequest()
            .body(Map.of("error", "Cannot delete system role"));
        }

        roleRepository.delete(role);
        log.info("Role deleted: {}", role.getName());

        return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
      })
      .orElse(ResponseEntity.notFound().build());
  }
}


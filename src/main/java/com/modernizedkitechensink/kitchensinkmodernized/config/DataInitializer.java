package com.modernizedkitechensink.kitchensinkmodernized.config;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.Permission;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.Role;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.MemberRepository;
import com.modernizedkitechensink.kitchensinkmodernized.repository.RoleRepository;
import com.modernizedkitechensink.kitchensinkmodernized.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Initializes the database with default data on application startup.
 *
 * Creates:
 * - Default roles (ADMIN, USER, VIEWER)
 * - Default admin user (admin/admin123)
 * - Default member (John Smith - matching legacy app)
 *
 * Only runs if data doesn't already exist (idempotent).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(String... args) {
    initializeRoles();
    initializeUsers();
    initializeMembers();
  }

  /**
   * Create default roles with their permissions.
   */
  private void initializeRoles() {
    // ADMIN role - full access
    if (!roleRepository.existsByName("ADMIN")) {
      Role adminRole = Role.builder()
        .name("ADMIN")
        .description("Full system access")
        .build();
      adminRole.setPermissionsFromEnums(Set.of(
        Permission.MEMBER_CREATE,
        Permission.MEMBER_READ,
        Permission.MEMBER_UPDATE,
        Permission.MEMBER_DELETE,
        Permission.USER_CREATE,
        Permission.USER_READ,
        Permission.USER_UPDATE,
        Permission.USER_DELETE,
        Permission.ROLE_CREATE,
        Permission.ROLE_READ,
        Permission.ROLE_UPDATE,
        Permission.ROLE_DELETE,
        Permission.SYSTEM_ADMIN
      ));
      roleRepository.save(adminRole);
      log.info("✓ Created ADMIN role");
    }

    // USER role - standard access
    if (!roleRepository.existsByName("USER")) {
      Role userRole = Role.builder()
        .name("USER")
        .description("Standard user access")
        .build();
      userRole.setPermissionsFromEnums(Set.of(
        Permission.MEMBER_CREATE,
        Permission.MEMBER_READ,
        Permission.MEMBER_UPDATE
      ));
      roleRepository.save(userRole);
      log.info("✓ Created USER role");
    }

    // VIEWER role - read-only access
    if (!roleRepository.existsByName("VIEWER")) {
      Role viewerRole = Role.builder()
        .name("VIEWER")
        .description("Read-only access")
        .build();
      viewerRole.setPermissionsFromEnums(Set.of(
        Permission.MEMBER_READ
      ));
      roleRepository.save(viewerRole);
      log.info("✓ Created VIEWER role");
    }
  }

  /**
   * Create default admin user.
   */
  private void initializeUsers() {
    if (!userRepository.existsByUsername("admin")) {
      Role adminRole = roleRepository.findByName("ADMIN")
        .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

      User admin = User.builder()
        .username("admin")
        .email("admin@kitchensink.com")
        .password(passwordEncoder.encode("Admin@2024"))
        .roles(Set.of(adminRole))
        .enabled(true)
        .accountNonLocked(true)
        .accountNonExpired(true)
        .credentialsNonExpired(true)
        .build();

      userRepository.save(admin);
      log.info("✓ Created admin user (username: admin, password: admin123)");
    }

    // Create a regular user for testing
    if (!userRepository.existsByUsername("user")) {
      Role userRole = roleRepository.findByName("USER")
        .orElseThrow(() -> new RuntimeException("USER role not found"));

      User regularUser = User.builder()
        .username("user")
        .email("user@kitchensink.com")
        .password(passwordEncoder.encode("User@2024"))
        .roles(Set.of(userRole))
        .enabled(true)
        .accountNonLocked(true)
        .accountNonExpired(true)
        .credentialsNonExpired(true)
        .build();

      userRepository.save(regularUser);
      log.info("✓ Created regular user (username: user, password: user123)");
    }
  }

  /**
   * Create default member (matching legacy app's import.sql).
   */
  private void initializeMembers() {
    if (memberRepository.count() == 0) {
      Member johnSmith = new Member();
      johnSmith.setId("0");  // Same ID as legacy for contract test compatibility
      johnSmith.setName("John Smith");
      johnSmith.setEmail("john.smith@mailinator.com");
      johnSmith.setPhoneNumber("2125551212");

      memberRepository.save(johnSmith);
      log.info("✓ Created default member: John Smith");
    }
  }
}
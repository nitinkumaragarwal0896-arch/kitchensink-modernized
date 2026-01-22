package com.modernizedkitechensink.kitchensinkmodernized.security;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.Role;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomUserDetailsService.
 * 
 * Tests the Spring Security integration for loading users from MongoDB.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Create test roles with permissions
        userRole = Role.builder()
                .id("role1")
                .name("USER")
                .description("Standard user")
                .permissions(Set.of("member:read", "member:create"))
                .build();

        adminRole = Role.builder()
                .id("role2")
                .name("ADMIN")
                .description("Administrator")
                .permissions(Set.of("member:read", "member:create", "member:update", "member:delete", "user:manage"))
                .build();

        // Create test user with roles
        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$hashedPassword")
                .roles(Set.of(userRole))
                .enabled(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .accountNonLocked(true)
                .build();
    }

    // ========== loadUserByUsername() Tests ==========

    @Test
    @DisplayName("Should successfully load user by username")
    void shouldSuccessfullyLoadUserByUsername() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("$2a$10$hashedPassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        
        // Verify permissions are flattened from roles
        assertEquals(2, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("member:read")));
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("member:create")));
        
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("nonexistent")
        );

        assertEquals("User not found with username: nonexistent", exception.getMessage());
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should load user with multiple roles and flatten all permissions")
    void shouldLoadUserWithMultipleRolesAndFlattenPermissions() {
        // Arrange - User with both USER and ADMIN roles
        testUser.setRoles(Set.of(userRole, adminRole));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert - Should have combined permissions from both roles
        assertNotNull(userDetails);
        assertEquals(5, userDetails.getAuthorities().size()); // 2 from USER + 3 unique from ADMIN
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("member:read")));
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("member:create")));
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("member:update")));
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("member:delete")));
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("user:manage")));
    }

    @Test
    @DisplayName("Should load user with no roles (empty authorities)")
    void shouldLoadUserWithNoRoles() {
        // Arrange - User with no roles
        testUser.setRoles(Set.of());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert - Should have zero authorities
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().isEmpty());
    }

    @Test
    @DisplayName("Should load disabled user correctly")
    void shouldLoadDisabledUserCorrectly() {
        // Arrange - Disabled user
        testUser.setEnabled(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
        assertEquals("testuser", userDetails.getUsername());
    }

    @Test
    @DisplayName("Should load locked user correctly")
    void shouldLoadLockedUserCorrectly() {
        // Arrange - Locked user
        testUser.setAccountNonLocked(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertFalse(userDetails.isAccountNonLocked());
        assertEquals("testuser", userDetails.getUsername());
    }

    @Test
    @DisplayName("Should load user with expired account correctly")
    void shouldLoadUserWithExpiredAccountCorrectly() {
        // Arrange - Account expired
        testUser.setAccountNonExpired(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertFalse(userDetails.isAccountNonExpired());
        assertEquals("testuser", userDetails.getUsername());
    }

    @Test
    @DisplayName("Should load user with expired credentials correctly")
    void shouldLoadUserWithExpiredCredentialsCorrectly() {
        // Arrange - Credentials expired
        testUser.setCredentialsNonExpired(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertFalse(userDetails.isCredentialsNonExpired());
        assertEquals("testuser", userDetails.getUsername());
    }

    @Test
    @DisplayName("Should load user with role containing no permissions")
    void shouldLoadUserWithRoleContainingNoPermissions() {
        // Arrange - Role with no permissions
        Role emptyRole = Role.builder()
                .id("role3")
                .name("GUEST")
                .description("Guest user")
                .permissions(Set.of()) // Empty permissions
                .build();
        testUser.setRoles(Set.of(emptyRole));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert - Should have zero authorities
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().isEmpty());
    }

    @Test
    @DisplayName("Should handle username with special characters")
    void shouldHandleUsernameWithSpecialCharacters() {
        // Arrange
        testUser.setUsername("test.user@example");
        when(userRepository.findByUsername("test.user@example")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test.user@example");

        // Assert
        assertNotNull(userDetails);
        assertEquals("test.user@example", userDetails.getUsername());
    }

    @Test
    @DisplayName("Should handle username with case sensitivity")
    void shouldHandleUsernameWithCaseSensitivity() {
        // Arrange - Note: Spring Security username is case-sensitive by default
        when(userRepository.findByUsername("TestUser")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("TestUser")
        );
        
        verify(userRepository).findByUsername("TestUser");
        verify(userRepository, never()).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should deduplicate permissions when user has overlapping roles")
    void shouldDeduplicatePermissionsWhenUserHasOverlappingRoles() {
        // Arrange - Two roles with overlapping permissions
        Role role1 = Role.builder()
                .id("role1")
                .name("ROLE1")
                .permissions(Set.of("member:read", "member:create"))
                .build();

        Role role2 = Role.builder()
                .id("role2")
                .name("ROLE2")
                .permissions(Set.of("member:read", "member:update")) // "member:read" overlaps
                .build();

        testUser.setRoles(Set.of(role1, role2));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert - Should have 3 unique permissions (member:read counted once)
        assertNotNull(userDetails);
        assertEquals(3, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("member:read")));
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("member:create")));
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("member:update")));
    }

    @Test
    @DisplayName("Should verify UserDetails is instance of Spring Security User")
    void shouldVerifyUserDetailsIsSpringSecurityUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertInstanceOf(org.springframework.security.core.userdetails.User.class, userDetails);
    }
}

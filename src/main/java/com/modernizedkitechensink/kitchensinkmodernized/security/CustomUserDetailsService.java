package com.modernizedkitechensink.kitchensinkmodernized.security;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import com.modernizedkitechensink.kitchensinkmodernized.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService - Bridges our User entity with Spring Security.
 *
 * Spring Security calls loadUserByUsername() during authentication.
 * We load our User from MongoDB and convert it to Spring's UserDetails.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  /**
   * Load user by username - called by Spring Security during login.
   *
   * @param username The username from login form
   * @return UserDetails object that Spring Security understands
   * @throws UsernameNotFoundException if user not found
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    //Load our User entity from MongoDB
    User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new UsernameNotFoundException(
        "User not found with username: " + username));

    //Convert our Permissions to Spring Security's GrantedAuthority
    // This flattens: User → Roles → Permissions into a single list
    List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
      .flatMap(role -> role.getPermissions().stream())  // Get all permission strings from all roles
      .map(SimpleGrantedAuthority::new)  // Permission strings are already in "member:read" format
      .collect(Collectors.toList());

    //Build and return Spring Security's UserDetails
    return new org.springframework.security.core.userdetails.User(
      user.getUsername(),
      user.getPassword(),
      user.isEnabled(),
      user.isAccountNonExpired(),
      user.isCredentialsNonExpired(),
      user.isAccountNonLocked(),
      authorities
    );
  }
}

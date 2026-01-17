package com.modernizedkitechensink.kitchensinkmodernized.config;

import com.modernizedkitechensink.kitchensinkmodernized.security.CustomUserDetailsService;
import com.modernizedkitechensink.kitchensinkmodernized.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration - Currently permits all requests.
 * Will add JWT authentication in Phase 3.
 */
/**
 * Security Configuration - JWT-based stateless authentication.
 *
 * Key differences from legacy JBoss:
 * - Stateless (no server-side sessions)
 * - JWT tokens instead of JSESSIONID cookies
 * - Method-level security with @PreAuthorize
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize annotations
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CustomUserDetailsService userDetailsService;

  /**
   * Main security filter chain configuration.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      // Disable CSRF - not needed for stateless JWT auth
      .csrf(AbstractHttpConfigurer::disable)

      // Enable CORS for frontend
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))

      // Configure endpoint access rules
      .authorizeHttpRequests(auth -> auth
        // Public endpoints - no authentication required
        .requestMatchers(
          "/api/v1/auth/login",    // Login
          "/api/v1/auth/register", // Register
          "/api/v1/auth/refresh",  // Refresh token
          "/swagger-ui/**",        // Swagger UI
          "/swagger-ui.html",
          "/v3/api-docs/**",       // OpenAPI docs
          "/actuator/health"       // Health check
        ).permitAll()

        // Admin endpoints - require system:admin permission
        .requestMatchers("/api/v1/admin/**").hasAuthority("system:admin")

        // All other endpoints require authentication
        .anyRequest().authenticated()
      )

      // Stateless session - no JSESSIONID cookie
      .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )

      // Use our custom authentication provider
      .authenticationProvider(authenticationProvider())

      // Add JWT filter before Spring's default auth filter
      .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Authentication Provider - validates username/password.
   * Uses our CustomUserDetailsService to load users.
   */
  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  /**
   * Password Encoder - BCrypt with strength 10.
   * BCrypt automatically handles salting.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Authentication Manager - needed for login endpoint.
   */
  @Bean
  public AuthenticationManager authenticationManager(
    AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * CORS Configuration - allows frontend to call API.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
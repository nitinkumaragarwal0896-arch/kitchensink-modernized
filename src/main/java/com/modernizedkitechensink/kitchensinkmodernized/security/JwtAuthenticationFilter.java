package com.modernizedkitechensink.kitchensinkmodernized.security;

import com.modernizedkitechensink.kitchensinkmodernized.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter - Intercepts every request to validate JWT tokens.
 *
 * Extends OncePerRequestFilter to guarantee single execution per request
 * (important because filters can be invoked multiple times in complex chains).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsService userDetailsService;
  private final TokenBlacklistService blacklistService;

  /**
   * Core filter logic - executed for EVERY HTTP request.
   */
  @Override
  protected void doFilterInternal(
    @NonNull HttpServletRequest request,
    @NonNull HttpServletResponse response,
    @NonNull FilterChain filterChain
  ) throws ServletException, IOException {

    try {
      //Extract JWT from the Authorization header
      String jwt = extractJwtFromRequest(request);

      if (StringUtils.hasText(jwt)) {
        
        // ✅ INSTANT LOGOUT: Check Redis blacklist FIRST (before expensive validation)
        if (blacklistService.isBlacklisted(jwt)) {
          log.warn("🚫 Blacklisted token detected: {}", 
            jwt.substring(0, Math.min(20, jwt.length())) + "...");
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.setContentType("application/json");
          response.getWriter().write(
            "{\"error\":\"Token has been revoked\",\"message\":\"Please log in again\"}"
          );
          return; // Stop processing immediately - reject the request!
        }
        
        //Validate token signature and expiration
        if (jwtTokenProvider.validateToken(jwt)) {

        //Get username from token
        String username = jwtTokenProvider.getUsernameFromToken(jwt);

        //Load user details from database
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        //Create authentication object
        UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
            userDetails,           // Principal (the user)
            null,                  // Credentials (not needed, already authenticated)
            userDetails.getAuthorities()  // User's roles/permissions
          );

        //Add request details (IP, session ID, etc.)
        authentication.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(request)
        );

        //Set authentication in Security Context
        //This makes the user available throughout the request
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Authenticated user: {}", username);
        }
      }
    } catch (Exception ex) {
      log.error("Could not set user authentication: {}", ex.getMessage());
    }

    // Continue to the next filter in the chain
    filterChain.doFilter(request, response);
  }

  /**
   * Extracts JWT token from the Authorization header.
   * Expected format: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   */
  private String extractJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");

    // Check if header exists and starts with "Bearer "
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);  // Remove "Bearer " prefix
    }
    return null;
  }
}

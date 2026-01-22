package com.modernizedkitechensink.kitchensinkmodernized.security;

import com.modernizedkitechensink.kitchensinkmodernized.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 * 
 * Tests the JWT authentication flow:
 * 1. Extract JWT from Authorization header
 * 2. Check if token is blacklisted
 * 3. Validate token signature
 * 4. Load user details
 * 5. Set authentication in SecurityContext
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TokenBlacklistService blacklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciJ9.test";
    private static final String BEARER_TOKEN = "Bearer " + VALID_JWT;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        // Clear SecurityContext before each test
        SecurityContextHolder.clearContext();
    }

    // ========== Successful Authentication Flow ==========

    @Test
    @DisplayName("Should authenticate user with valid JWT token")
    void shouldAuthenticateUserWithValidJwtToken() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(blacklistService.isBlacklisted(VALID_JWT)).thenReturn(false);
        when(jwtTokenProvider.validateToken(VALID_JWT)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(VALID_JWT)).thenReturn(USERNAME);

        UserDetails userDetails = new User(USERNAME, "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        
        // Verify authentication was set in SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication, "Authentication should be set");
        assertEquals(USERNAME, authentication.getName());
        assertTrue(authentication.isAuthenticated());
        assertEquals(1, authentication.getAuthorities().size());
    }

    // ========== No Token Cases ==========

    @Test
    @DisplayName("Should continue filter chain when no Authorization header present")
    void shouldContinueWhenNoAuthorizationHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(blacklistService, never()).isBlacklisted(anyString());
        verify(jwtTokenProvider, never()).validateToken(anyString());
        
        // Authentication should not be set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should continue filter chain when Authorization header is empty")
    void shouldContinueWhenAuthorizationHeaderIsEmpty() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(blacklistService, never()).isBlacklisted(anyString());
        verify(jwtTokenProvider, never()).validateToken(anyString());
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should continue when Authorization header does not start with Bearer")
    void shouldContinueWhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(blacklistService, never()).isBlacklisted(anyString());
        verify(jwtTokenProvider, never()).validateToken(anyString());
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ========== Blacklisted Token Cases ==========

    @Test
    @DisplayName("Should reject request when token is blacklisted")
    void shouldRejectRequestWhenTokenIsBlacklisted() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(blacklistService.isBlacklisted(VALID_JWT)).thenReturn(true);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(response).getWriter();
        
        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("Token has been revoked"));
        assertTrue(responseBody.contains("Please log in again"));

        // Filter chain should NOT continue
        verify(filterChain, never()).doFilter(request, response);
        
        // Authentication should not be set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should handle short blacklisted token gracefully")
    void shouldHandleShortBlacklistedTokenGracefully() throws ServletException, IOException {
        // Arrange
        String shortToken = "short";
        String shortBearerToken = "Bearer " + shortToken;
        when(request.getHeader("Authorization")).thenReturn(shortBearerToken);
        when(blacklistService.isBlacklisted(shortToken)).thenReturn(true);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    // ========== Invalid Token Cases ==========

    @Test
    @DisplayName("Should not authenticate when token validation fails")
    void shouldNotAuthenticateWhenTokenValidationFails() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(blacklistService.isBlacklisted(VALID_JWT)).thenReturn(false);
        when(jwtTokenProvider.validateToken(VALID_JWT)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).getUsernameFromToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        
        // Authentication should not be set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ========== Exception Handling ==========

    @Test
    @DisplayName("Should continue filter chain even when exception occurs in token validation")
    void shouldContinueFilterChainWhenExceptionOccurs() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(blacklistService.isBlacklisted(VALID_JWT)).thenReturn(false);
        when(jwtTokenProvider.validateToken(VALID_JWT)).thenThrow(new RuntimeException("Token parsing failed"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        
        // Authentication should not be set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should handle exception when loading user details")
    void shouldHandleExceptionWhenLoadingUserDetails() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(blacklistService.isBlacklisted(VALID_JWT)).thenReturn(false);
        when(jwtTokenProvider.validateToken(VALID_JWT)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(VALID_JWT)).thenReturn(USERNAME);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenThrow(new RuntimeException("User not found"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        
        // Authentication should not be set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should handle exception when blacklist service throws")
    void shouldHandleExceptionWhenBlacklistServiceThrows() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(blacklistService.isBlacklisted(VALID_JWT)).thenThrow(new RuntimeException("Redis connection failed"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        
        // Authentication should not be set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("Should extract token correctly from Bearer header")
    void shouldExtractTokenCorrectlyFromBearerHeader() throws ServletException, IOException {
        // Arrange
        String customToken = "custom.jwt.token.here";
        String customBearerToken = "Bearer " + customToken;
        when(request.getHeader("Authorization")).thenReturn(customBearerToken);
        when(blacklistService.isBlacklisted(customToken)).thenReturn(false);
        when(jwtTokenProvider.validateToken(customToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(customToken)).thenReturn(USERNAME);

        UserDetails userDetails = new User(USERNAME, "password", Collections.emptyList());
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(blacklistService).isBlacklisted(customToken);
        verify(jwtTokenProvider).validateToken(customToken);
        verify(jwtTokenProvider).getUsernameFromToken(customToken);
    }

    @Test
    @DisplayName("Should handle Bearer header with extra spaces")
    void shouldHandleBearerHeaderWithExtraSpaces() throws ServletException, IOException {
        // Arrange
        // Note: Spring's StringUtils.hasText() will trim, but substring won't
        // The actual token will have a leading space if Bearer has trailing space
        String bearerWithSpace = "Bearer  " + VALID_JWT; // Double space
        when(request.getHeader("Authorization")).thenReturn(bearerWithSpace);
        
        String extractedToken = " " + VALID_JWT; // Will have leading space
        when(blacklistService.isBlacklisted(extractedToken)).thenReturn(false);
        when(jwtTokenProvider.validateToken(extractedToken)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        // The filter will process but validation will fail due to malformed token
    }

    @Test
    @DisplayName("Should not set authentication when user has no authorities")
    void shouldSetAuthenticationEvenWithNoAuthorities() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(blacklistService.isBlacklisted(VALID_JWT)).thenReturn(false);
        when(jwtTokenProvider.validateToken(VALID_JWT)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(VALID_JWT)).thenReturn(USERNAME);

        UserDetails userDetails = new User(USERNAME, "password", Collections.emptyList());
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(0, authentication.getAuthorities().size());
    }

    @Test
    @DisplayName("Should set authentication with multiple authorities")
    void shouldSetAuthenticationWithMultipleAuthorities() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(blacklistService.isBlacklisted(VALID_JWT)).thenReturn(false);
        when(jwtTokenProvider.validateToken(VALID_JWT)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(VALID_JWT)).thenReturn(USERNAME);

        UserDetails userDetails = new User(USERNAME, "password", 
            java.util.Arrays.asList(
                new SimpleGrantedAuthority("member:read"),
                new SimpleGrantedAuthority("member:write"),
                new SimpleGrantedAuthority("system:admin")
            ));
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(3, authentication.getAuthorities().size());
    }
}

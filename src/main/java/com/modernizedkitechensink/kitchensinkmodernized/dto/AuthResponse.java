package com.modernizedkitechensink.kitchensinkmodernized.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication responses.
 *
 * Returned after successful login.
 *
 * Example JSON:
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiIs...",
 *   "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 900,
 *   "username": "admin"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

  private String accessToken;     // Short-lived token for API calls
  private String refreshToken;    // Long-lived token to get new access tokens
  private String tokenType;       // Always "Bearer"
  private long expiresIn;         // Access token expiry in seconds
  private String username;        // Logged in user's username
}

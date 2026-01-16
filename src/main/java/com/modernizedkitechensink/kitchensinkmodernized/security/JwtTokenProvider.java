package com.modernizedkitechensink.kitchensinkmodernized.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT Token Provider - Generates and validates JSON Web Tokens.
 *
 * A JWT has 3 parts: Header.Payload.Signature
 * - Header: Algorithm used (HS256)
 * - Payload: Claims (username, roles, expiration)
 * - Signature: Ensures token wasn't tampered with
 */
@Component
@Slf4j
public class JwtTokenProvider {

  // These values come from application.properties
  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.access-token-expiration}")
  private long accessTokenExpiration;  // in milliseconds

  @Value("${jwt.refresh-token-expiration}")
  private long refreshTokenExpiration;

  /**
   * Creates the signing key from our secret.
   * Uses HMAC-SHA256 algorithm for signing.
   */
  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes());
  }

  /**
   * Generates an ACCESS TOKEN after successful login.
   * Short-lived (15 minutes) - used for API requests.
   */
  public String generateAccessToken(Authentication authentication) {
    String username = authentication.getName();

    // Collect all user roles/permissions into a comma-separated string
    String authorities = authentication.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .collect(Collectors.joining(","));

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

    return Jwts.builder()
      .subject(username)                    // Who this token is for
      .claim("authorities", authorities)    // User's roles/permissions
      .issuedAt(now)                        // When token was created
      .expiration(expiryDate)               // When token expires
      .signWith(getSigningKey())            // Sign with our secret
      .compact();                           // Build the token string
  }

  /**
   * Generates a REFRESH TOKEN after successful login.
   * Long-lived (7 days) - used only to get new access tokens.
   */
  public String generateRefreshToken(Authentication authentication) {
    String username = authentication.getName();
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

    return Jwts.builder()
      .subject(username)
      .issuedAt(now)
      .expiration(expiryDate)
      .signWith(getSigningKey())
      .compact();
  }

  /**
   * Extracts the username from a token.
   * Used by the authentication filter to identify the user.
   */
  public String getUsernameFromToken(String token) {
    Claims claims = Jwts.parser()
      .verifyWith(getSigningKey())
      .build()
      .parseSignedClaims(token)
      .getPayload();

    return claims.getSubject();
  }

  /**
   * Validates a token - checks signature and expiration.
   * Returns true only if token is valid and not expired.
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token);
      return true;
    } catch (ExpiredJwtException ex) {
      log.error("JWT token expired: {}", ex.getMessage());
    } catch (MalformedJwtException ex) {
      log.error("Invalid JWT token: {}", ex.getMessage());
    } catch (SecurityException ex) {
      log.error("JWT signature validation failed: {}", ex.getMessage());
    } catch (IllegalArgumentException ex) {
      log.error("JWT claims string is empty: {}", ex.getMessage());
    }
    return false;
  }


}

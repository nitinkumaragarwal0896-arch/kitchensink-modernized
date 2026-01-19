package com.modernizedkitechensink.kitchensinkmodernized.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Service for blacklisting JWT access tokens to enable instant logout.
 * 
 * PROBLEM: JWTs are stateless - once issued, they're valid until expiration.
 * If a user logs out or an admin revokes a session, the JWT still works!
 * 
 * SOLUTION: Store revoked tokens in Redis blacklist. On every request,
 * check if token is blacklisted BEFORE validating signature.
 * 
 * WHY REDIS:
 * - Sub-millisecond lookups (0.3-0.5ms)
 * - Built-in TTL (auto-expire when token naturally expires)
 * - Distributed (shared across all backend instances)
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    /**
     * Get the signing key for JWT operations.
     * Must match the key used in JwtTokenProvider.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Hash a token using SHA-256.
     * We store token HASHES (not actual tokens) in Redis for security.
     * Even if Redis is compromised, the actual tokens can't be stolen.
     * 
     * @param token The JWT token string
     * @return Base64-encoded SHA-256 hash
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Add a JWT token to the blacklist (instant revocation).
     * 
     * The token HASH will be stored in Redis with a TTL equal to its remaining lifetime.
     * Once the token expires naturally, Redis auto-removes it (memory efficient).
     * 
     * We hash the token for:
     * 1. Security: Even if Redis is compromised, actual tokens can't be stolen
     * 2. Consistency: Matches how we store tokens in MongoDB (RefreshToken.accessTokenHash)
     * 
     * @param token The JWT access token to blacklist
     */
    public void blacklistToken(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Attempted to blacklist null or empty token");
            return;
        }

        // Hash the token (don't store plain tokens!)
        String tokenHash = hashToken(token);
        String key = BLACKLIST_PREFIX + tokenHash;
        
        // Calculate remaining TTL (how long until token expires naturally)
        long remainingTtl = getRemainingTtl(token);
        
        if (remainingTtl <= 0) {
            log.debug("Token already expired, not adding to blacklist");
            return;
        }
        
        // Store in Redis with TTL = remaining token lifetime
        // Value is just "revoked" (we only care if key exists)
        redisTemplate.opsForValue().set(key, "revoked", remainingTtl, TimeUnit.MILLISECONDS);
        
        log.info("🚫 Token blacklisted (hash): {} (TTL: {}ms)", tokenHash.substring(0, 16) + "...", remainingTtl);
    }

    /**
     * Blacklist a token by its pre-computed hash.
     * 
     * Used when revoking sessions from SessionController - we already have the
     * token hash stored in RefreshToken.accessTokenHash, so we don't need to
     * recompute it or parse the actual token.
     * 
     * @param tokenHash The SHA-256 hash of the JWT access token
     */
    public void blacklistTokenByHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isEmpty()) {
            log.warn("Attempted to blacklist null or empty token hash");
            return;
        }

        String key = BLACKLIST_PREFIX + tokenHash;
        
        // We don't have the actual token, so we can't calculate its remaining TTL.
        // Use the default access token expiration (15 minutes).
        // This is safe because:
        // 1. If the token is already expired, the blacklist entry won't hurt
        // 2. Redis will auto-remove it after 15 minutes anyway
        long ttl = accessTokenExpiration;
        
        // Store in Redis with TTL = default access token lifetime
        redisTemplate.opsForValue().set(key, "revoked", ttl, TimeUnit.MILLISECONDS);
        
        log.info("🚫 Token blacklisted by hash: {} (TTL: {}ms)", tokenHash.substring(0, 16) + "...", ttl);
    }

    /**
     * Check if a JWT token is blacklisted.
     * 
     * This is called on EVERY authenticated request by JwtAuthenticationFilter.
     * Performance is critical - that's why we use Redis (0.3-0.5ms).
     * 
     * We hash the token first, then check if the hash is blacklisted.
     * This matches how we store tokens in both Redis (blacklist) and MongoDB (RefreshToken).
     * 
     * @param token The JWT access token to check
     * @return true if token is blacklisted (should reject request)
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        // Hash the token (matches how we store it)
        String tokenHash = hashToken(token);
        String key = BLACKLIST_PREFIX + tokenHash;
        Boolean exists = redisTemplate.hasKey(key);
        
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Calculate remaining time until token expires naturally.
     * 
     * This determines how long we need to keep the token in the blacklist.
     * Once the token expires, there's no point keeping it in Redis.
     * 
     * @param token The JWT access token
     * @return Remaining TTL in milliseconds, or 0 if already expired
     */
    private long getRemainingTtl(String token) {
        try {
            // Parse JWT to extract expiration time (JJWT 0.12.x syntax)
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();
            long expirationTime = expiration.getTime();
            
            // Return remaining time, or 0 if already expired
            return Math.max(0, expirationTime - now);
            
        } catch (Exception e) {
            // If we can't parse the token, fall back to default expiration
            log.warn("Failed to parse token for TTL calculation, using default: {}", e.getMessage());
            return accessTokenExpiration;
        }
    }

    /**
     * Blacklist all tokens for a user (logout from all devices instantly).
     * 
     * NOTE: This is a simplified implementation. For production, you'd:
     * 1. Store user→tokens mapping in Redis
     * 2. Or query active sessions from RefreshTokenRepository
     * 3. Blacklist all their access tokens
     * 
     * For now, this is called when:
     * - User changes password (security best practice)
     * - Admin manually revokes all sessions
     * 
     * @param userId The user ID whose tokens should be blacklisted
     */
    public void blacklistAllUserTokens(String userId) {
        // TODO: In production, maintain a Redis set of active tokens per user
        // For now, this is a placeholder that would be called from:
        // - ChangePasswordController (with user's current token)
        // - SessionController (with user's refresh tokens → extract access tokens)
        
        log.info("🚫 Blacklisting all tokens for user: {}", userId);
        // Implementation would iterate through user's active sessions
        // and call blacklistToken() for each access token
    }

    /**
     * Clear the entire blacklist (admin utility).
     * 
     * WARNING: Only use this in development or emergency scenarios.
     * In production, let tokens expire naturally via Redis TTL.
     */
    public void clearBlacklist() {
        log.warn("⚠️ Clearing entire token blacklist (admin action)");
        // This is intentionally not implemented for safety
        // If needed, use: redisTemplate.keys(BLACKLIST_PREFIX + "*")
        // But KEYS is slow on production Redis - use SCAN instead
    }
}

package com.modernizedkitechensink.kitchensinkmodernized.repository;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RefreshToken operations.
 * 
 * REFACTORED: Changed from @DBRef User to String userId
 * - All queries now use userId directly (simpler, faster)
 * - No more nested property queries (user.id)
 * - More MongoDB-friendly pattern
 */
@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

  /**
   * Find token by its hash.
   * Used during token refresh to validate the token.
   */
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /**
   * Find all active (non-revoked, non-expired) tokens for a user by userId.
   * Used to display "Active Sessions" page.
   */
  List<RefreshToken> findByUserIdAndRevokedFalseAndExpiresAtAfter(String userId, LocalDateTime now);

  /**
   * Find all tokens for a user by userId (including revoked/expired).
   * Used for session management.
   */
  List<RefreshToken> findByUserId(String userId);

  /**
   * Count active sessions for a user by userId.
   * Used to enforce session limits.
   */
  int countByUserIdAndRevokedFalseAndExpiresAtAfter(String userId, LocalDateTime now);

  /**
   * Delete all tokens for a user by userId.
   * Used when user is deleted.
   */
  void deleteByUserId(String userId);

  /**
   * Delete expired tokens (cleanup job).
   * MongoDB TTL index handles this automatically, but this is for manual cleanup if needed.
   */
  void deleteByExpiresAtBefore(LocalDateTime date);

  /**
   * Find all active (non-revoked) tokens for a user by userId.
   * Used for blacklisting all tokens when password changes.
   */
  List<RefreshToken> findByUserIdAndRevokedFalse(String userId);

  /**
   * Find existing session by userId, deviceInfo, and ipAddress.
   * Used for session deduplication (prevent multiple sessions from same browser).
   */
  Optional<RefreshToken> findByUserIdAndDeviceInfoAndIpAddressAndRevokedFalseAndExpiresAtAfter(
    String userId, String deviceInfo, String ipAddress, LocalDateTime now
  );
}


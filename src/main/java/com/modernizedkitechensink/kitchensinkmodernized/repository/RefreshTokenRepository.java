package com.modernizedkitechensink.kitchensinkmodernized.repository;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.RefreshToken;
import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RefreshToken operations.
 */
@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

  /**
   * Find token by its hash.
   * Used during token refresh to validate the token.
   */
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /**
   * Find all active (non-revoked, non-expired) tokens for a user.
   * Used to display "Active Sessions" page.
   */
  List<RefreshToken> findByUserAndRevokedFalseAndExpiresAtAfter(User user, LocalDateTime now);

  /**
   * Find all tokens for a user (including revoked/expired).
   * Used for session management.
   */
  List<RefreshToken> findByUser(User user);

  /**
   * Count active sessions for a user.
   * Used to enforce session limits.
   */
  int countByUserAndRevokedFalseAndExpiresAtAfter(User user, LocalDateTime now);

  /**
   * Delete all tokens for a user.
   * Used when user is deleted.
   */
  void deleteByUser(User user);

  /**
   * Delete expired tokens (cleanup job).
   * MongoDB TTL index handles this automatically, but this is for manual cleanup if needed.
   */
  void deleteByExpiresAtBefore(LocalDateTime date);

  /**
   * Find all active (non-revoked) tokens for a user by user ID.
   * Used for blacklisting all tokens when password changes.
   * Note: Uses nested property query (user.id) since RefreshToken has @DBRef User.
   */
  List<RefreshToken> findByUser_IdAndRevokedFalse(String userId);
}


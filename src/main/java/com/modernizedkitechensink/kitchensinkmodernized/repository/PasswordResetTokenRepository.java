package com.modernizedkitechensink.kitchensinkmodernized.repository;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Password Reset Tokens.
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Repository
public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    /**
     * Find token by its hash.
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Delete all tokens for a specific user.
     * Useful when password is successfully reset.
     */
    void deleteByUserId(String userId);
}


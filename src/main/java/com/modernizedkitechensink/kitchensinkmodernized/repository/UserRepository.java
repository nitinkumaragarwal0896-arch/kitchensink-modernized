package com.modernizedkitechensink.kitchensinkmodernized.repository;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entities.
 *
 * Spring Data MongoDB automatically implements these methods.
 * Method names follow a convention: findBy + FieldName
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

  /**
   * Find user by username (for login).
   * Generated query: db.users.findOne({username: ?})
   */
  Optional<User> findByUsername(String username);

  /**
   * Find user by email (for registration duplicate check).
   * Generated query: db.users.findOne({email: ?})
   */
  Optional<User> findByEmail(String email);

  /**
   * Check if username already exists.
   * Generated query: db.users.count({username: ?}) > 0
   */
  boolean existsByUsername(String username);

  /**
   * Check if email already exists.
   * Generated query: db.users.count({email: ?}) > 0
   */
  boolean existsByEmail(String email);
}

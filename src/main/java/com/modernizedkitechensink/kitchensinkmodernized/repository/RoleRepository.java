package com.modernizedkitechensink.kitchensinkmodernized.repository;

import com.modernizedkitechensink.kitchensinkmodernized.model.auth.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends MongoRepository<Role, String> {

  /**
   * Find role by name (e.g., "ADMIN", "USER").
   * Used when assigning roles to users.
   */
  Optional<Role> findByName(String name);

  /**
   * Check if role name already exists.
   * Used when creating new roles.
   */
  boolean existsByName(String name);
}

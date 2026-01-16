package com.modernizedkitechensink.kitchensinkmodernized.audit;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for audit log queries.
 *
 * Useful for:
 * - Security investigations ("What did user X do?")
 * - Compliance reports ("Show all DELETE operations")
 * - Debugging ("What happened to entity Y?")
 */
@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

  /**
   * Find all actions by a specific user.
   * Use case: "Show me everything user 'john' has done"
   */
  List<AuditLog> findByUsernameOrderByTimestampDesc(String username);

  /**
   * Find all actions on a specific entity.
   * Use case: "Show me the history of Member #123"
   */
  List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);

  /**
   * Find all actions of a specific type.
   * Use case: "Show me all DELETE operations"
   */
  List<AuditLog> findByActionOrderByTimestampDesc(String action);

  /**
   * Find all actions within a time range.
   * Use case: "What happened between 9am and 10am?"
   */
  List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
    LocalDateTime start, LocalDateTime end);

  /**
   * Find failed operations.
   * Use case: "Show me all failures for investigation"
   */
  List<AuditLog> findByStatusOrderByTimestampDesc(String status);
}
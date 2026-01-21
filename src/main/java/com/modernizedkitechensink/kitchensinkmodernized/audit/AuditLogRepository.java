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
}
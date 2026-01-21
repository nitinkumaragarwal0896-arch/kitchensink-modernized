package com.modernizedkitechensink.kitchensinkmodernized.config;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB Index Initializer - Creates indexes on application startup.
 * 
 * This component:
 * 1. Runs AFTER the application is fully started (ApplicationReadyEvent)
 * 2. Creates indexes in a safe, idempotent way
 * 3. Handles conflicts by dropping and recreating indexes
 * 4. Doesn't block application startup on failure
 * 
 * WHY NOT USE @Indexed ANNOTATIONS WITH AUTO-INDEX-CREATION?
 * - Auto-index creation can fail if indexes exist with different options
 * - Failures during auto-index creation CRASH the application at startup
 * - No way to handle conflicts gracefully
 * 
 * WHY THIS APPROACH IS BETTER:
 * - Application starts successfully even if indexes fail
 * - Can drop conflicting indexes and recreate
 * - Idempotent: safe to run multiple times
 * - Detailed logging for troubleshooting
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexInitializer {

    private final MongoTemplate mongoTemplate;

    /**
     * Initialize all MongoDB indexes after application startup.
     * 
     * Runs asynchronously after application is ready, so it doesn't block startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initIndexes() {
        log.info("🔍 Starting MongoDB index initialization...");
        
        try {
            createMemberIndexes();
            createUserIndexes();
            createRoleIndexes();
            createRefreshTokenIndexes();
            createAuditLogIndexes();
            createJobIndexes();
            createPasswordResetTokenIndexes();
            
            log.info("✅ MongoDB indexes initialized successfully");
        } catch (Exception e) {
            log.error("❌ Failed to initialize MongoDB indexes (application will continue): {}", e.getMessage(), e);
        }
    }

    /**
     * Create indexes for members collection.
     */
    private void createMemberIndexes() {
        log.info("Creating indexes for 'members' collection...");
        MongoCollection<Document> collection = mongoTemplate.getCollection("members");
        
        try {
            // 1. Unique index on email
            createIndexSafely(collection, "email", 
                Indexes.ascending("email"), 
                new IndexOptions().unique(true).name("email_unique_idx"));
            
            // 2. Index on name (for search/sort)
            createIndexSafely(collection, "name",
                Indexes.ascending("name"),
                new IndexOptions().name("name_idx"));
            
            // 3. Index on createdAt (descending, for recent members query)
            createIndexSafely(collection, "createdAt",
                Indexes.descending("createdAt"),
                new IndexOptions().name("createdAt_desc_idx"));
            
            // 4. Index on updatedAt (descending)
            createIndexSafely(collection, "updatedAt",
                Indexes.descending("updatedAt"),
                new IndexOptions().name("updatedAt_desc_idx"));
            
            // 5. Compound index: createdBy + createdAt (for filtering by creator)
            createIndexSafely(collection, "createdBy_createdAt",
                Indexes.compoundIndex(
                    Indexes.ascending("createdBy"),
                    Indexes.descending("createdAt")
                ),
                new IndexOptions().name("createdBy_createdAt_idx"));
            
            // 6. Compound index: name + createdAt (for sorted search)
            createIndexSafely(collection, "name_createdAt",
                Indexes.compoundIndex(
                    Indexes.ascending("name"),
                    Indexes.descending("createdAt")
                ),
                new IndexOptions().name("name_createdAt_idx"));
            
            log.info("✓ Members indexes created");
        } catch (Exception e) {
            log.error("Failed to create members indexes: {}", e.getMessage());
        }
    }

    /**
     * Create indexes for users collection.
     */
    private void createUserIndexes() {
        log.info("Creating indexes for 'users' collection...");
        MongoCollection<Document> collection = mongoTemplate.getCollection("users");
        
        try {
            createIndexSafely(collection, "username",
                Indexes.ascending("username"),
                new IndexOptions().unique(true).name("username_unique_idx"));
            
            createIndexSafely(collection, "email",
                Indexes.ascending("email"),
                new IndexOptions().unique(true).name("email_unique_idx"));
            
            log.info("✓ Users indexes created");
        } catch (Exception e) {
            log.error("Failed to create users indexes: {}", e.getMessage());
        }
    }

    /**
     * Create indexes for roles collection.
     */
    private void createRoleIndexes() {
        log.info("Creating indexes for 'roles' collection...");
        MongoCollection<Document> collection = mongoTemplate.getCollection("roles");
        
        try {
            createIndexSafely(collection, "name",
                Indexes.ascending("name"),
                new IndexOptions().unique(true).name("name_unique_idx"));
            
            log.info("✓ Roles indexes created");
        } catch (Exception e) {
            log.error("Failed to create roles indexes: {}", e.getMessage());
        }
    }

    /**
     * Create indexes for refresh_tokens collection.
     */
    private void createRefreshTokenIndexes() {
        log.info("Creating indexes for 'refresh_tokens' collection...");
        MongoCollection<Document> collection = mongoTemplate.getCollection("refresh_tokens");
        
        try {
            // Unique index on token hash
            createIndexSafely(collection, "tokenHash",
                Indexes.ascending("tokenHash"),
                new IndexOptions().unique(true).name("tokenHash_unique_idx"));
            
            // Index on userId for finding user's sessions
            createIndexSafely(collection, "userId",
                Indexes.ascending("userId"),
                new IndexOptions().name("userId_idx"));
            
            // TTL index on expiryDate (auto-delete expired tokens)
            createIndexSafely(collection, "expiryDate",
                Indexes.ascending("expiryDate"),
                new IndexOptions().expireAfter(0L, TimeUnit.SECONDS).name("expiryDate_ttl_idx"));
            
            log.info("✓ Refresh tokens indexes created");
        } catch (Exception e) {
            log.error("Failed to create refresh_tokens indexes: {}", e.getMessage());
        }
    }

    /**
     * Create indexes for audit_logs collection.
     */
    private void createAuditLogIndexes() {
        log.info("Creating indexes for 'audit_logs' collection...");
        MongoCollection<Document> collection = mongoTemplate.getCollection("audit_logs");
        
        try {
            // Index on timestamp (descending, for recent activity)
            createIndexSafely(collection, "timestamp",
                Indexes.descending("timestamp"),
                new IndexOptions().name("timestamp_desc_idx"));
            
            // Compound index: entityType + entityId (for entity history)
            createIndexSafely(collection, "entityType_entityId",
                Indexes.compoundIndex(
                    Indexes.ascending("entityType"),
                    Indexes.ascending("entityId")
                ),
                new IndexOptions().name("entityType_entityId_idx"));
            
            // Compound index: userId + timestamp (for user activity)
            createIndexSafely(collection, "userId_timestamp",
                Indexes.compoundIndex(
                    Indexes.ascending("userId"),
                    Indexes.descending("timestamp")
                ),
                new IndexOptions().name("userId_timestamp_idx"));
            
            log.info("✓ Audit logs indexes created");
        } catch (Exception e) {
            log.error("Failed to create audit_logs indexes: {}", e.getMessage());
        }
    }

    /**
     * Create indexes for jobs collection.
     */
    private void createJobIndexes() {
        log.info("Creating indexes for 'jobs' collection...");
        MongoCollection<Document> collection = mongoTemplate.getCollection("jobs");
        
        try {
            // Index on type
            createIndexSafely(collection, "type",
                Indexes.ascending("type"),
                new IndexOptions().name("type_idx"));
            
            // Index on status
            createIndexSafely(collection, "status",
                Indexes.ascending("status"),
                new IndexOptions().name("status_idx"));
            
            // Index on userId
            createIndexSafely(collection, "userId",
                Indexes.ascending("userId"),
                new IndexOptions().name("userId_idx"));
            
            // TTL index on createdAt (auto-delete old jobs after 7 days)
            createIndexSafely(collection, "createdAt",
                Indexes.descending("createdAt"),
                new IndexOptions().expireAfter(604800L, TimeUnit.SECONDS).name("createdAt_ttl_idx"));
            
            // Compound index: userId + status + createdAt
            createIndexSafely(collection, "userId_status_createdAt",
                Indexes.compoundIndex(
                    Indexes.ascending("userId"),
                    Indexes.ascending("status"),
                    Indexes.descending("createdAt")
                ),
                new IndexOptions().name("userId_status_createdAt_idx"));
            
            // Compound index: status + createdAt
            createIndexSafely(collection, "status_createdAt",
                Indexes.compoundIndex(
                    Indexes.ascending("status"),
                    Indexes.descending("createdAt")
                ),
                new IndexOptions().name("status_createdAt_idx"));
            
            log.info("✓ Jobs indexes created");
        } catch (Exception e) {
            log.error("Failed to create jobs indexes: {}", e.getMessage());
        }
    }

    /**
     * Create indexes for password_reset_tokens collection.
     */
    private void createPasswordResetTokenIndexes() {
        log.info("Creating indexes for 'password_reset_tokens' collection...");
        MongoCollection<Document> collection = mongoTemplate.getCollection("password_reset_tokens");
        
        try {
            // Unique index on token
            createIndexSafely(collection, "token",
                Indexes.ascending("token"),
                new IndexOptions().unique(true).name("token_unique_idx"));
            
            // TTL index on expiryDate (auto-delete expired tokens)
            createIndexSafely(collection, "expiryDate",
                Indexes.ascending("expiryDate"),
                new IndexOptions().expireAfter(0L, TimeUnit.SECONDS).name("expiryDate_ttl_idx"));
            
            log.info("✓ Password reset tokens indexes created");
        } catch (Exception e) {
            log.error("Failed to create password_reset_tokens indexes: {}", e.getMessage());
        }
    }

    /**
     * Create an index safely - handles conflicts by dropping and recreating.
     * 
     * @param collection MongoDB collection
     * @param indexKey Unique key for this index (for logging)
     * @param index Index definition
     * @param options Index options (name, unique, ttl, etc.)
     */
    private void createIndexSafely(MongoCollection<Document> collection, String indexKey, 
                                   org.bson.conversions.Bson index, IndexOptions options) {
        try {
            // Try to create the index
            collection.createIndex(index, options);
            log.debug("  ✓ Created index: {} ({})", options.getName(), indexKey);
        } catch (com.mongodb.MongoCommandException e) {
            // If index already exists with different options, drop and recreate
            if (e.getErrorCode() == 85 || e.getErrorCode() == 86) { // IndexOptionsConflict or IndexKeySpecsConflict
                log.warn("  ⚠ Index conflict detected for '{}', dropping and recreating...", options.getName());
                try {
                    collection.dropIndex(options.getName());
                    collection.createIndex(index, options);
                    log.info("  ✓ Recreated index: {} ({})", options.getName(), indexKey);
                } catch (Exception ex) {
                    log.error("  ✗ Failed to recreate index '{}': {}", options.getName(), ex.getMessage());
                }
            } else {
                log.error("  ✗ Failed to create index '{}': {}", options.getName(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("  ✗ Unexpected error creating index '{}': {}", options.getName(), e.getMessage());
        }
    }
}


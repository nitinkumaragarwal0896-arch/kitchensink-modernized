#!/bin/bash
# Script to create MongoDB indexes for Kitchensink Modernized

echo "Creating MongoDB indexes..."

mongosh kitchensink --quiet --eval '
// Members Collection Indexes
print("Creating indexes for members collection...");
db.members.createIndex({ "email": 1 }, { unique: true, name: "email_1" });
db.members.createIndex({ "name": 1 }, { name: "name_1" });
db.members.createIndex({ "createdAt": -1 }, { name: "createdAt_-1" });
db.members.createIndex({ "updatedAt": -1 }, { name: "updatedAt_-1" });
db.members.createIndex({ "createdBy": 1, "createdAt": -1 }, { name: "createdBy_createdAt_idx" });
db.members.createIndex({ "name": 1, "createdAt": -1 }, { name: "name_createdAt_idx" });

// Jobs Collection Indexes
print("Creating indexes for jobs collection...");
db.jobs.createIndex({ "type": 1 }, { name: "type_1" });
db.jobs.createIndex({ "status": 1 }, { name: "status_1" });
db.jobs.createIndex({ "userId": 1 }, { name: "userId_1" });
db.jobs.createIndex({ "createdAt": -1 }, { name: "createdAt_-1", expireAfterSeconds: 604800 });
db.jobs.createIndex({ "userId": 1, "status": 1, "createdAt": -1 }, { name: "userId_status_createdAt_idx" });
db.jobs.createIndex({ "status": 1, "createdAt": -1 }, { name: "status_createdAt_idx" });

// Users Collection Indexes
print("Creating indexes for users collection...");
db.users.createIndex({ "username": 1 }, { unique: true, name: "username_1" });
db.users.createIndex({ "email": 1 }, { unique: true, name: "email_1" });

// Refresh Tokens Collection Indexes
print("Creating indexes for refreshTokens collection...");
db.refreshTokens.createIndex({ "token": 1 }, { unique: true, name: "token_1" });
db.refreshTokens.createIndex({ "userId": 1 }, { name: "userId_1" });
db.refreshTokens.createIndex({ "expiryDate": 1 }, { name: "expiryDate_1", expireAfterSeconds: 0 });

// Audit Log Collection Indexes (if exists)
print("Creating indexes for auditLog collection...");
db.auditLog.createIndex({ "timestamp": -1 }, { name: "timestamp_-1" });
db.auditLog.createIndex({ "entityType": 1, "entityId": 1, "timestamp": -1 }, { name: "entityType_entityId_idx" });
db.auditLog.createIndex({ "userId": 1, "timestamp": -1 }, { name: "userId_timestamp_idx" });

print("\\nIndexes created successfully!");

// Show created indexes
print("\\n=== MEMBERS INDEXES ===");
printjson(db.members.getIndexes());

print("\\n=== JOBS INDEXES ===");
printjson(db.jobs.getIndexes());

print("\\n=== USERS INDEXES ===");
printjson(db.users.getIndexes());

print("\\n=== REFRESH TOKENS INDEXES ===");
printjson(db.refreshTokens.getIndexes());

// Show collection stats
print("\\n=== COLLECTION STATS ===");
print("Members count:", db.members.countDocuments());
print("Jobs count:", db.jobs.countDocuments());
print("Users count:", db.users.countDocuments());
print("Refresh Tokens count:", db.refreshTokens.countDocuments());
'

echo "Done! Check output above for created indexes."


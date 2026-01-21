# 🚀 Kitchensink Modernized - Backend

A modernized version of the classic JBoss EAP Kitchensink application, migrated from **Jakarta EE/JBoss** to **Spring Boot 3.x** with **MongoDB**, showcasing enterprise-grade features and best practices for modern application development.

> **Project Context:** This is a MongoDB Modernization Factory Developer Candidate Challenge submission, demonstrating a complete migration of a legacy JBoss application to a modern cloud-native stack.

---

## 📋 Table of Contents

- [Migration Overview](#-migration-overview)
- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [Project Structure](#-project-structure)
- [Performance](#-performance-comparison)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Troubleshooting](#-troubleshooting)

---

## 🎯 Migration Overview

| Aspect | Legacy (JBoss EAP) | Modernized (Spring Boot) |
|--------|-------------------|-------------------------|
| **Framework** | Jakarta EE 10 | Spring Boot 3.2 |
| **Java Version** | Java 11 | Java 21 |
| **Database** | H2 (JPA) | MongoDB 7.0 |
| **Security** | Container-managed | JWT + RBAC |
| **API** | JAX-RS | Spring REST |
| **Build** | Maven + WAR | Maven + JAR |
| **Runtime** | WildFly Server | Embedded Tomcat |
| **Frontend** | JSF | React 18 + Vite |
| **Caching** | None | Redis |
| **Sessions** | Server-side | JWT (stateless) |

---

## ✨ Features

### Core Features
- ✅ **Member Registration** - CRUD operations with validation
- ✅ **Search & Pagination** - Efficient member search with MongoDB indexes
- ✅ **Bean Validation** - JSR-380 validation with custom validators
- ✅ **RESTful API** - Spring REST with OpenAPI documentation

### Security Features
- ✅ **JWT Authentication** - Stateless, scalable authentication
- ✅ **Dual Token System** - Access tokens (1 hour) + Refresh tokens (7 days)
- ✅ **RBAC** - Role-based access control with granular permissions
- ✅ **Token Blacklist** - Instant logout using Redis
- ✅ **Session Management** - Track and revoke device sessions
- ✅ **Account Lockout** - Protection against brute-force attacks

### Production-Ready Features
- ✅ **Audit Logging** - AOP-based activity tracking
- ✅ **Redis Caching** - High-performance caching layer
- ✅ **MongoDB Indexing** - Optimized queries (IXSCAN vs COLLSCAN)
- ✅ **Exception Handling** - Global exception handler with localized messages
- ✅ **Internationalization** - Multi-language support (English, Hindi, Spanish)
- ✅ **OpenAPI/Swagger** - Interactive API documentation
- ✅ **Health Checks** - Kubernetes-ready actuator endpoints
- ✅ **Async Processing** - Background jobs for bulk operations

### Advanced Features
- ✅ **Bulk Delete** - Async job processing with progress tracking
- ✅ **Excel Upload** - Bulk member import with row-level validation
- ✅ **Smart Delete Logic** - Single item = sync, multiple = async job
- ✅ **Job Management** - Track background jobs with status updates
- ✅ **Scheduled Cleanup** - Auto-delete old jobs and sessions

---

## 🛠️ Technology Stack

### Backend
- **Framework:** Spring Boot 3.2.0
- **Language:** Java 21
- **Database:** MongoDB 7.0
- **Cache:** Redis 7.0
- **Security:** Spring Security 6, JWT (JJWT)
- **Validation:** Jakarta Validation (Hibernate Validator)
- **Documentation:** SpringDoc OpenAPI 3
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Build:** Maven 3.8+

### Key Dependencies
```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    
    <!-- Excel Processing -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>
</dependencies>
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** (JDK 21.0.9 or higher)
  ```bash
  java -version  # Should show version 21.x
  ```
- **Maven 3.8+**
  ```bash
  mvn -v
  ```
- **MongoDB 7.0+** (see installation guide below)
- **Redis 7.0+** (optional, for caching and blacklist)
- **Node.js 18+** (for frontend)

---

### MongoDB Installation

<details>
<summary><b>macOS (Homebrew)</b></summary>

```bash
brew tap mongodb/brew
brew install mongodb-community@7.0
brew services start mongodb-community@7.0

# Verify MongoDB is running
mongosh --eval "db.adminCommand('ping')"
```
</details>

<details>
<summary><b>Docker (All Platforms - Recommended)</b></summary>

```bash
# Start MongoDB
docker run -d --name mongodb \
  -p 27017:27017 \
  -v mongodb_data:/data/db \
  mongo:7.0

# Verify MongoDB is running
docker exec mongodb mongosh --eval "db.adminCommand('ping')"
```
</details>

<details>
<summary><b>Linux (Ubuntu/Debian)</b></summary>

```bash
# Import MongoDB GPG key
curl -fsSL https://www.mongodb.org/static/pgp/server-7.0.asc | \
  sudo gpg --dearmor -o /etc/apt/trusted.gpg.d/mongodb-7.gpg

# Add MongoDB repository
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu $(lsb_release -cs)/mongodb-org/7.0 multiverse" | \
  sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# Install MongoDB
sudo apt-get update
sudo apt-get install -y mongodb-org

# Start MongoDB
sudo systemctl start mongod
sudo systemctl enable mongod

# Verify
mongosh --eval "db.adminCommand('ping')"
```
</details>

<details>
<summary><b>Windows</b></summary>

Download and install from [MongoDB Download Center](https://www.mongodb.com/try/download/community)

After installation:
```cmd
# Start MongoDB as a service (run as Administrator)
net start MongoDB

# Or start manually
"C:\Program Files\MongoDB\Server\7.0\bin\mongod.exe" --dbpath="C:\data\db"
```
</details>

---

### Redis Installation (Optional)

<details>
<summary><b>macOS (Homebrew)</b></summary>

```bash
brew install redis
brew services start redis

# Verify
redis-cli ping  # Should return "PONG"
```
</details>

<details>
<summary><b>Docker (All Platforms - Recommended)</b></summary>

```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine

# Verify
docker exec redis redis-cli ping  # Should return "PONG"
```
</details>

<details>
<summary><b>Linux</b></summary>

```bash
sudo apt-get install redis-server
sudo systemctl start redis
sudo systemctl enable redis

# Verify
redis-cli ping  # Should return "PONG"
```
</details>

---

### 🎯 One-Command Startup (Recommended)

The easiest way to run the entire application:

```bash
# Clone both repositories (backend and frontend)
git clone https://github.com/nitinkumaragarwal0896-arch/kitchensink-modernized.git
git clone https://github.com/nitinkumaragarwal0896-arch/kitchensink-frontend.git

# Start everything from backend directory
cd kitchensink-modernized
./start-all.sh
```

**What this script does:**
1. ✅ Checks Java 21 is installed
2. ✅ Checks if MongoDB is running on port 27017
3. ✅ Checks if Redis is running on port 6379
4. 🔧 If not running, prompts you to start them (Docker or Homebrew)
5. ✅ Builds the backend (Maven)
6. ✅ Starts Spring Boot on port 8081
7. ✅ Finds and starts the frontend on port 3000
8. ✅ Displays URLs and default credentials

**Stop all services:**
```bash
./stop-all.sh
```

---

### Manual Setup (Alternative)

<details>
<summary><b>Step-by-Step Manual Installation</b></summary>

#### 1. Clone the Repository
```bash
git clone https://github.com/nitinkumaragarwal0896-arch/kitchensink-modernized.git
cd kitchensink-modernized
```

#### 2. Configure Application
Edit `src/main/resources/application.properties` if needed:
```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/kitchensink

# Redis (optional)
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Server
server.port=8081

# JWT
jwt.secret=your-secret-key-here-change-in-production
jwt.access-token-expiration=3600000  # 1 hour
jwt.refresh-token-expiration=604800000  # 7 days
```

#### 3. Build the Application
```bash
# Using Maven Wrapper (recommended)
./mvnw clean package -DskipTests

# Or using system Maven
mvn clean package -DskipTests
```

#### 4. Run the Application
```bash
# Using Maven
./mvnw spring-boot:run

# Or run the JAR
java -jar target/kitchensink-modernized-0.0.1-SNAPSHOT.jar
```

#### 5. Verify Application is Running
```bash
# Health check
curl http://localhost:8081/actuator/health

# Expected response:
# {"status":"UP"}
```
</details>

---

### 🌐 Access the Application

| URL | Description |
|-----|-------------|
| http://localhost:3000 | **React Frontend** (main UI) |
| http://localhost:8081/swagger-ui/index.html | **Swagger UI** - Interactive API docs |
| http://localhost:8081/actuator/health | Health Check endpoint |
| http://localhost:8081/api/v1/members | Members API (requires auth) |

---

## 🔐 Default Credentials

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| `admin` | `Admin@2024` | ADMIN | Full access to all features |
| `user` | `User@2024` | USER | Create, read, update members |
| `viewer` | `Viewer@2024` | VIEWER | Read-only access |

### Login Example

```bash
# Get JWT tokens
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin@2024"
  }'

# Response:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "username": "admin"
}

# Use access token for API calls
curl http://localhost:8081/api/v1/members \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## ⚙️ Configuration

### Application Properties

<details>
<summary><b>application.properties</b></summary>

```properties
# Application
spring.application.name=kitchensink-modernized

# Server
server.port=8081

# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/kitchensink
spring.data.mongodb.auto-index-creation=false

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=60000

# Cache TTL (10 minutes)
spring.cache.redis.time-to-live=600000

# JWT
jwt.secret=${JWT_SECRET:your-secret-key-minimum-32-characters-required-change-in-production}
jwt.access-token-expiration=3600000  # 1 hour
jwt.refresh-token-expiration=604800000  # 7 days

# Internationalization
spring.messages.basename=messages
spring.messages.encoding=UTF-8
spring.web.locale=en
spring.web.locale-resolver=accept-header

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized

# Logging
logging.level.com.modernizedkitechensink=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.data.mongodb=DEBUG

# Async
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=50
spring.task.execution.pool.queue-capacity=100
```
</details>

### Environment Variables

For production deployment, use environment variables:

```bash
export MONGODB_URI=mongodb://username:password@host:27017/database
export REDIS_HOST=redis.example.com
export REDIS_PORT=6379
export JWT_SECRET=your-production-secret-key-32-characters-minimum
export JWT_ACCESS_TOKEN_EXPIRATION=3600000
export JWT_REFRESH_TOKEN_EXPIRATION=604800000
```

---

## 📚 API Documentation

### Interactive Documentation

Access Swagger UI at: http://localhost:8081/swagger-ui/index.html

### API Endpoints

<details>
<summary><b>Authentication & Session Management</b></summary>

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/login` | Login and get JWT tokens | No |
| POST | `/api/v1/auth/register` | Register new user | No |
| POST | `/api/v1/auth/refresh` | Refresh access token | Yes |
| GET | `/api/v1/auth/me` | Get current user info | Yes |
| POST | `/api/v1/auth/logout` | Logout from current session | Yes |
| POST | `/api/v1/auth/logout-all` | Logout from all sessions | Yes |
| GET | `/api/v1/sessions` | Get all active sessions | Yes |
| DELETE | `/api/v1/sessions/{id}` | Revoke specific session | Yes |
</details>

<details>
<summary><b>Members</b></summary>

| Method | Endpoint | Description | Permission |
|--------|----------|-------------|------------|
| GET | `/api/v1/members?page=0&size=10&sort=name,asc` | List members (paginated) | `member:read` |
| GET | `/api/v1/members?search=john` | Search members | `member:read` |
| GET | `/api/v1/members/{id}` | Get member by ID | `member:read` |
| POST | `/api/v1/members` | Create member | `member:create` |
| PUT | `/api/v1/members/{id}` | Update member | `member:update` |
| DELETE | `/api/v1/members/{id}` | Delete member | `member:delete` |
| POST | `/api/v1/members/bulk-delete` | Bulk delete (async job) | `member:delete` |
| POST | `/api/v1/members/excel-upload` | Bulk upload via Excel | `member:create` |
</details>

<details>
<summary><b>Jobs (Background Tasks)</b></summary>

| Method | Endpoint | Description | Permission |
|--------|----------|-------------|------------|
| GET | `/api/v1/jobs` | Get user's jobs | Any authenticated |
| GET | `/api/v1/jobs/{id}` | Get job status | Any authenticated |
| POST | `/api/v1/jobs/{id}/cancel` | Cancel pending job | Any authenticated |
| DELETE | `/api/v1/jobs/{id}` | Delete completed job | Any authenticated |
</details>

<details>
<summary><b>Admin - User Management</b></summary>

| Method | Endpoint | Description | Permission |
|--------|----------|-------------|------------|
| GET | `/api/v1/admin/users` | List all users | `user:read` |
| GET | `/api/v1/admin/users/{id}` | Get user by ID | `user:read` |
| POST | `/api/v1/admin/users` | Create user | `user:create` |
| PUT | `/api/v1/admin/users/{id}` | Update user | `user:update` |
| DELETE | `/api/v1/admin/users/{id}` | Delete user | `user:delete` |
</details>

<details>
<summary><b>Admin - Role Management</b></summary>

| Method | Endpoint | Description | Permission |
|--------|----------|-------------|------------|
| GET | `/api/v1/admin/roles` | List all roles | `role:read` |
| GET | `/api/v1/admin/roles/{id}` | Get role by ID | `role:read` |
| POST | `/api/v1/admin/roles` | Create role | `role:create` |
| PUT | `/api/v1/admin/roles/{id}` | Update role | `role:update` |
| GET | `/api/v1/admin/permissions` | List all permissions | `role:read` |
</details>

---

## 🏗️ Project Structure

```
kitchensink-modernized/
├── src/
│   ├── main/
│   │   ├── java/com/modernizedkitechensink/kitchensinkmodernized/
│   │   │   ├── config/                    # Configuration classes
│   │   │   │   ├── DataInitializer.java   # Seed default data
│   │   │   │   ├── SecurityConfig.java    # JWT & RBAC setup
│   │   │   │   ├── AsyncConfig.java       # Async processing
│   │   │   │   ├── RedisConfig.java       # Redis cache config
│   │   │   │   ├── MongoConfig.java       # MongoDB auditing
│   │   │   │   └── OpenApiConfig.java     # Swagger setup
│   │   │   ├── controller/                # REST endpoints
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthController.java      # Login, register
│   │   │   │   │   └── SessionController.java   # Session mgmt
│   │   │   │   ├── MemberController.java        # Member CRUD
│   │   │   │   ├── JobController.java           # Job status
│   │   │   │   └── admin/
│   │   │   │       ├── AdminUserController.java # User mgmt
│   │   │   │       └── AdminRoleController.java # Role mgmt
│   │   │   ├── model/                     # Domain entities
│   │   │   │   ├── Member.java            # Member entity
│   │   │   │   ├── Job.java               # Background job
│   │   │   │   └── auth/
│   │   │   │       ├── User.java          # User entity
│   │   │   │       ├── Role.java          # Role entity
│   │   │   │       ├── RefreshToken.java  # Session tracking
│   │   │   │       └── AuditLog.java      # Audit trail
│   │   │   ├── repository/                # MongoDB repositories
│   │   │   │   ├── MemberRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── RoleRepository.java
│   │   │   │   ├── RefreshTokenRepository.java
│   │   │   │   ├── JobRepository.java
│   │   │   │   └── AuditLogRepository.java
│   │   │   ├── service/                   # Business logic
│   │   │   │   ├── IMemberService.java    # Member service interface
│   │   │   │   ├── MemberServiceImpl.java # Member service impl
│   │   │   │   ├── JobService.java        # Async job processing
│   │   │   │   ├── RefreshTokenService.java # Session management
│   │   │   │   ├── TokenBlacklistService.java # Redis blacklist
│   │   │   │   └── AuditService.java      # Audit logging
│   │   │   ├── security/                  # Security components
│   │   │   │   ├── JwtAuthenticationFilter.java # JWT filter
│   │   │   │   ├── JwtTokenProvider.java        # JWT generation
│   │   │   │   └── CustomUserDetailsService.java # User loading
│   │   │   ├── audit/                     # AOP auditing
│   │   │   │   └── AuditAspect.java       # Audit interceptor
│   │   │   ├── validation/                # Custom validators
│   │   │   │   ├── EmailValidationService.java
│   │   │   │   └── PhoneValidationService.java
│   │   │   └── exception/                 # Exception handling
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── MemberNotFoundException.java
│   │   │       └── DuplicateEmailException.java
│   │   └── resources/
│   │       ├── application.properties     # Configuration
│   │       ├── messages.properties        # English
│   │       ├── messages_hi.properties     # Hindi
│   │       └── messages_es.properties     # Spanish
│   └── test/
│       ├── java/                          # Unit & integration tests
│       │   ├── controller/
│       │   ├── service/
│       │   └── integration/
│       └── resources/
│           └── application-test.properties
├── start-all.sh                           # One-command startup
├── stop-all.sh                            # Stop all services
├── settings-public.xml                    # Maven public repos
├── create-indexes.sh                      # MongoDB index setup
├── mongodb-indexes.md                     # Index documentation
├── pom.xml                                # Maven dependencies
└── README.md                              # This file
```

---

## 📊 Performance Comparison

### Startup Time
| Metric | Legacy (JBoss + WildFly) | Modernized (Spring Boot) |
|--------|--------------------------|--------------------------|
| Cold Start | ~30 seconds | **~5 seconds** (6x faster) |
| Memory | ~500MB baseline | **~250MB baseline** (50% less) |

### API Performance
| Metric | Legacy | Modernized | Improvement |
|--------|--------|------------|-------------|
| Requests/Second | 2,463 req/s | **4,259 req/s** | +73% faster |
| Response Time | 4.06 ms | **2.35 ms** | -42% faster |
| Transfer Rate | 529 KB/s | **2,108 KB/s** | 4x faster |

### MongoDB Indexing
- **Before Indexes:** COLLSCAN - Full collection scan (~50-100ms)
- **After Indexes:** IXSCAN - Index scan (~1-5ms)
- **Improvement:** 10-100x faster queries

### Load Test
```bash
# Run load test (1000 requests, 10 concurrent)
./load-test.sh
```

---

## 🧪 Testing

### Run All Tests
```bash
./mvnw test
```

### Run Specific Test
```bash
./mvnw test -Dtest=MemberServiceTest
```

### Integration Tests (with Testcontainers)
```bash
./mvnw verify -Pintegration-tests
```

### Test Coverage
```bash
./mvnw jacoco:report
# Open target/site/jacoco/index.html
```

---

## 🚀 Deployment

### Docker

<details>
<summary><b>Build Docker Image</b></summary>

```bash
# Build image
docker build -t kitchensink-modernized:latest .

# Run container
docker run -d \
  --name kitchensink-backend \
  -p 8081:8081 \
  -e MONGODB_URI=mongodb://host.docker.internal:27017/kitchensink \
  -e REDIS_HOST=host.docker.internal \
  -e JWT_SECRET=your-production-secret \
  kitchensink-modernized:latest
```
</details>

### Docker Compose

<details>
<summary><b>docker-compose.yml</b></summary>

```yaml
version: '3.8'

services:
  mongodb:
    image: mongo:7.0
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db
    environment:
      MONGO_INITDB_DATABASE: kitchensink

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  backend:
    build: .
    ports:
      - "8081:8081"
    environment:
      MONGODB_URI: mongodb://mongodb:27017/kitchensink
      REDIS_HOST: redis
      JWT_SECRET: change-this-in-production
    depends_on:
      - mongodb
      - redis

volumes:
  mongodb_data:
```

Run with:
```bash
docker-compose up -d
```
</details>

---

## 🔧 Troubleshooting

<details>
<summary><b>MongoDB Connection Issues</b></summary>

**Problem:** `MongoTimeoutException: Timed out after 30000 ms`

**Solutions:**
```bash
# Check if MongoDB is running
mongosh --eval "db.adminCommand('ping')"

# Start MongoDB (Homebrew)
brew services start mongodb-community

# Start MongoDB (Docker)
docker start mongodb

# Check MongoDB logs
docker logs mongodb
```
</details>

<details>
<summary><b>Redis Connection Issues</b></summary>

**Problem:** `Unable to connect to Redis`

**Solutions:**
```bash
# Check if Redis is running
redis-cli ping

# Start Redis (Homebrew)
brew services start redis

# Start Redis (Docker)
docker start redis

# If Redis is optional, comment out in application.properties
# spring.data.redis.host=localhost
```
</details>

<details>
<summary><b>Port Already in Use</b></summary>

**Problem:** `Port 8081 is already in use`

**Solutions:**
```bash
# Find process using port 8081
lsof -i :8081

# Kill the process
kill -9 <PID>

# Or change port in application.properties
server.port=8082
```
</details>

<details>
<summary><b>Java Version Issues</b></summary>

**Problem:** `release version 21 not supported`

**Solutions:**
```bash
# Check Java version
java -version

# Install Java 21 (macOS)
brew install openjdk@21

# Set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```
</details>

---

## 📖 Additional Documentation

- [MongoDB Indexes Guide](./mongodb-indexes.md)
- [Session Management Explained](./SESSION_MANAGEMENT_EXPLAINED.md)
- [Startup Script Documentation](./STARTUP.md)
- [Load Test Results](./LOAD_TEST_RESULTS.md)

---

## 🤝 Contributing

This is a showcase project for MongoDB Modernization Factory Developer Candidate Challenge. For production use, consider:

1. Changing default passwords
2. Using secure JWT secret (32+ characters)
3. Enabling HTTPS
4. Setting up MongoDB authentication
5. Configuring Redis password
6. Adding rate limiting
7. Implementing API versioning
8. Setting up monitoring (Prometheus, Grafana)

---

## 📝 License

This project is created for MongoDB Modernization Factory Developer Candidate Challenge.

---

## 👤 Author

**Nitin Agarwal**
- GitHub: [@nitinkumaragarwal0896-arch](https://github.com/nitinkumaragarwal0896-arch)
- Project: [kitchensink-modernized](https://github.com/nitinkumaragarwal0896-arch/kitchensink-modernized)

---

## 🙏 Acknowledgments

- Original JBoss Kitchensink application from Red Hat
- MongoDB team for the Modernization Factory challenge
- Spring Boot team for excellent documentation
- Open source community for amazing libraries

---

**Need Help?** Check the [Troubleshooting](#-troubleshooting) section or create an issue on GitHub!

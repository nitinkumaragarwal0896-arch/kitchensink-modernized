# Kitchensink Modernized

A modernized version of the classic JBoss EAP Kitchensink application, migrated from **Jakarta EE/JBoss** to **Spring Boot 3.x** with **MongoDB**.

## 🎯 Migration Overview

| Aspect | Legacy (JBoss EAP) | Modernized (Spring Boot) |
|--------|-------------------|-------------------------|
| **Framework** | Jakarta EE 10 | Spring Boot 3.2 |
| **Database** | H2 (JPA) | MongoDB |
| **Security** | Container-managed | JWT + RBAC |
| **API** | JAX-RS | Spring REST |
| **Build** | Maven + WAR | Maven + JAR |
| **Runtime** | WildFly Server | Embedded Tomcat |

## ✨ Features

### Core Features
- ✅ Member Registration (CRUD operations)
- ✅ Bean Validation
- ✅ RESTful API

### Production-Ready Features
- ✅ **JWT Authentication** - Stateless, scalable auth
- ✅ **RBAC** - Role-based access control with granular permissions
- ✅ **Audit Logging** - Track all operations with AOP
- ✅ **Rate Limiting** - Prevent API abuse (100 req/min)
- ✅ **Caching** - Caffeine cache for performance
- ✅ **OpenAPI/Swagger** - Interactive API documentation
- ✅ **Health Checks** - Kubernetes-ready actuator endpoints
- ✅ **CI/CD** - GitHub Actions pipeline

## 🚀 Quick Start

### Prerequisites
- **Java 21** (JDK 21.0.9 or higher recommended)
- **Maven 3.8+**
- **MongoDB 6.0+** (see installation guide below)
- **Node.js 18+** ⚠️ **REQUIRED for frontend** - [Download here](https://nodejs.org/)
  - Check version: `node -v` (should show v18.x or higher)
  - Older versions (v12, v14, v16) will fail with "Unexpected token" errors

#### Installing MongoDB

**macOS (Homebrew):**
```bash
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community
```

**Docker (All Platforms - Recommended):**
```bash
docker run -d --name mongodb -p 27017:27017 -v mongodb_data:/data/db mongo:7.0
```

**Linux (Ubuntu/Debian):**
```bash
# Import MongoDB GPG key
curl -fsSL https://www.mongodb.org/static/pgp/server-7.0.asc | sudo gpg --dearmor -o /etc/apt/trusted.gpg.d/mongodb-7.gpg

# Add MongoDB repository
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# Install MongoDB
sudo apt-get update
sudo apt-get install -y mongodb-org

# Start MongoDB
sudo systemctl start mongod
sudo systemctl enable mongod
```

**Windows:**
Download and install from [MongoDB Download Center](https://www.mongodb.com/try/download/community)

### 🎯 One-Command Startup (Recommended)

**Prerequisites:** Both backend and frontend repos must be cloned:

```bash
# Clone both repositories
git clone https://github.com/nitinkumaragarwal0896-arch/kitchensink-modernized.git
git clone https://github.com/nitinkumaragarwal0896-arch/kitchensink-frontend.git

# Start everything from backend directory
cd kitchensink-modernized
./start-all.sh
```

**What this script does:**
1. ✅ **Checks if MongoDB is running** on port 27017
2. 🔧 If not running, **prompts you to choose**:
   - Option 1: Start existing MongoDB (Homebrew)
   - Option 2: Start MongoDB with Docker (auto-installs container)
   - Option 3: Exit and install manually
3. ✅ Build the backend (Maven)
4. ✅ Start Spring Boot backend on port 8081
5. ✅ **Automatically finds and starts the frontend** (checks `../kitchensink-frontend`)
6. ✅ Display URLs and credentials

**First-time users without MongoDB:**
- Choose **Option 2** when prompted - it will automatically pull and start MongoDB in Docker
- No manual MongoDB installation needed!

**Note on directory structure:**
The script expects this directory structure:
```
parent-folder/
├── kitchensink-modernized/     (backend - clone this first)
└── kitchensink-frontend/       (frontend - clone this second)
```

If your frontend is in a different location, the script will prompt you for the path.

**Stop all services:**
```bash
./stop-all.sh
```

See [STARTUP.md](./STARTUP.md) for detailed script documentation.

### Manual Setup (Alternative)

#### 1. Start MongoDB

**Option A: Using Homebrew (macOS)**
```bash
brew services start mongodb-community
```

**Option B: Using Docker**
```bash
docker run -d --name mongodb -p 27017:27017 mongo:7.0
```

**Option C: Local MongoDB**
```bash
mongod --dbpath /path/to/data
```

#### 2. Build the Application
```bash
./mvnw clean package
```

#### 3. Run the Backend
```bash
./mvnw spring-boot:run
```

The backend starts on **http://localhost:8081**

#### 4. Start the Frontend (Optional)
```bash
cd ../kitchensink-spring-mongodb/frontend
npm install
npm run dev
```

The frontend starts on **http://localhost:3000**

### 🌐 Access the Application

| URL | Description |
|-----|-------------|
| http://localhost:3000 | **React Frontend** (recommended) |
| http://localhost:8081/swagger-ui/index.html | **Swagger UI** - Interactive API docs |
| http://localhost:8081/actuator/health | Health Check endpoint |
| http://localhost:8081/api/v1/members | Members API (requires auth) |

## 🔐 Authentication

### Default Users

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| `admin` | `Admin@2024` | ADMIN | Full access to all features |
| `user` | `User@2024` | USER | Read, Create, Update members |

### Login Example

```bash
# Get JWT token
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "Admin@2024"}'

# Response:
# {
#   "accessToken": "eyJhbGc...",
#   "refreshToken": "eyJhbGc...",
#   "username": "admin",
#   "roles": ["ADMIN"],
#   "permissions": ["member:read", "member:create", ...]
# }

# Use token for API calls
curl http://localhost:8081/api/v1/members \
  -H "Authorization: Bearer <your-access-token>"
```

### Token Lifecycle

- **Access Token**: Valid for 15 minutes (used for API calls)
- **Refresh Token**: Valid for 7 days (used to get new access tokens)
- **Max Concurrent Sessions**: 5 per user (oldest session auto-revoked)

See [SESSION_MANAGEMENT_EXPLAINED.md](./SESSION_MANAGEMENT_EXPLAINED.md) for details on session handling.## 📚 API Endpoints

### Authentication & Session Management
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/login` | Login and get JWT tokens | No |
| POST | `/api/v1/auth/register` | Register new user | No |
| POST | `/api/v1/auth/refresh` | Refresh access token | Yes (refresh token) |
| GET | `/api/v1/auth/me` | Get current user info | Yes |
| POST | `/api/v1/auth/logout` | Revoke current refresh token | Yes |
| POST | `/api/v1/auth/logout-all` | Revoke all refresh tokens for user | Yes |
| GET | `/api/v1/sessions` | Get all active sessions | Yes |
| DELETE | `/api/v1/sessions/{id}` | Revoke specific session | Yes |

### Members
| Method | Endpoint | Description | Permission |
|--------|----------|-------------|------------|
| GET | `/api/v1/members` | List all members | `member:read` |
| GET | `/api/v1/members/{id}` | Get member by ID | `member:read` |
| POST | `/api/v1/members` | Create member | `member:create` |
| PUT | `/api/v1/members/{id}` | Update member | `member:update` |
| DELETE | `/api/v1/members/{id}` | Delete member | `member:delete` |

### Admin - User Management
| Method | Endpoint | Description | Permission |
|--------|----------|-------------|------------|
| GET | `/api/v1/admin/users` | List all users | `user:read` |
| GET | `/api/v1/admin/users/{id}` | Get user by ID | `user:read` |
| PUT | `/api/v1/admin/users/{id}` | Update user | `user:update` |
| DELETE | `/api/v1/admin/users/{id}` | Delete user | `user:delete` |
| POST | `/api/v1/admin/users/{id}/enable` | Enable user account | `user:update` |
| POST | `/api/v1/admin/users/{id}/disable` | Disable user account | `user:update` |
| POST | `/api/v1/admin/users/{id}/unlock` | Unlock locked account | `user:update` |

### Admin - Role Management
| Method | Endpoint | Description | Permission |
|--------|----------|-------------|------------|
| GET | `/api/v1/admin/roles` | List all roles | `role:read` |
| GET | `/api/v1/admin/roles/{id}` | Get role by ID | `role:read` |
| POST | `/api/v1/admin/roles` | Create role | `role:create` |
| PUT | `/api/v1/admin/roles/{id}` | Update role | `role:update` |
| DELETE | `/api/v1/admin/roles/{id}` | Delete role | `role:delete` |
| GET | `/api/v1/admin/roles/permissions` | List all permissions | `role:read` |

## 🏗️ Project Structure

```
kitchensink-modernized/
├── src/
│   ├── main/
│   │   ├── java/com/modernizedkitechensink/kitchensinkmodernized/
│   │   │   ├── config/           # Configuration classes
│   │   │   │   ├── DataInitializer.java      # Seed default users/roles
│   │   │   │   ├── SecurityConfig.java       # JWT & RBAC config
│   │   │   │   ├── AsyncConfig.java          # Async task executor
│   │   │   │   ├── CacheConfig.java          # Caffeine cache
│   │   │   │   └── OpenApiConfig.java        # Swagger docs
│   │   │   ├── controller/       # REST endpoints
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthController.java   # Login, register, refresh
│   │   │   │   │   └── SessionController.java # Session management
│   │   │   │   ├── MemberController.java     # Member CRUD
│   │   │   │   ├── AdminUserController.java  # User management
│   │   │   │   └── AdminRoleController.java  # Role management
│   │   │   ├── model/            # Domain entities
│   │   │   │   ├── Member.java
│   │   │   │   └── auth/
│   │   │   │       ├── User.java
│   │   │   │       ├── Role.java
│   │   │   │       ├── Permission.java       # Granular permissions
│   │   │   │       └── RefreshToken.java     # Session tracking
│   │   │   ├── repository/       # MongoDB repositories
│   │   │   ├── service/          # Business logic
│   │   │   ├── security/         # JWT, filters
│   │   │   ├── audit/            # AOP audit logging
│   │   │   └── exception/        # Custom exceptions
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/                 # Unit tests
│       └── resources/
├── start-all.sh                  # One-command startup
├── stop-all.sh                   # Stop all services
├── load-test.sh                  # Performance testing
├── STARTUP.md                    # Startup script documentation
├── SESSION_MANAGEMENT_EXPLAINED.md  # Deep dive into JWT/sessions
└── README.md                     # This file
```

## 📊 Performance Comparison

### Quick Load Test

```bash
# Run load test (requires Apache Bench)
./load-test.sh
```

### Actual Load Test Results

**Test Scenario:** 1,000 requests with 10 concurrent users

| Metric | Legacy (JBoss + H2) | Modernized (Spring Boot + MongoDB) | Winner |
|--------|---------------------|-------------------------------------|--------|
| **Requests/Second** | 2,463 req/s | **4,259 req/s** | 🚀 **+73% faster** |
| **Response Time** | 4.06 ms | **2.35 ms** | 🚀 **-42% faster** |
| **Failed Requests** | 0 | **0** | ✅ **Both 100% reliable** |
| **Transfer Rate** | 529 KB/s | **2,108 KB/s** | 🚀 **4x faster** |

> 📖 **Full Analysis:** See [LOAD_TEST_RESULTS.md](./LOAD_TEST_RESULTS.md) for detailed breakdown, methodology, and scalability implications.

### Migration Benefits

| Aspect | Legacy (JBoss) | Modernized (Spring Boot) | Improvement |
|--------|----------------|-------------------------|-------------|
| **Startup Time** | ~30 seconds | ~5 seconds | **6x faster** |
| **Memory Footprint** | ~500MB baseline | ~250MB baseline | **50% less** |
| **Database** | H2 (in-memory) | MongoDB (persistent + scalable) | **Production-ready** |
| **Horizontal Scaling** | Session replication needed | Stateless JWT | **Cloud-native** |
| **Developer Setup** | Manual WildFly setup | `./start-all.sh` | **2 minutes** |
| **API Documentation** | None | Swagger UI | **Better DX** |

### Scalability Improvements

**Legacy Architecture:**
- Requires session replication for multi-instance
- H2 in-memory DB (data lost on restart)
- Heavyweight application server

**Modernized Architecture:**
- ✅ Stateless JWT (no session replication)
- ✅ MongoDB supports sharding/replica sets
- ✅ Embedded Tomcat (lightweight)
- ✅ Cloud-ready (Docker, Kubernetes)

### Load Testing Results

```bash
# Example results (your machine may vary)
Modernized App:
  Requests per second: 850 req/s
  Time taken: 1.176 seconds
  Failed requests: 0

# Supports horizontal scaling:
# 2 instances → ~1700 req/s
# 3 instances → ~2550 req/s
```

## 🏗️ Project Structure

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
- **MongoDB 6.0+** (or use Homebrew: `brew services start mongodb-community`)
- **Node.js 18+** (for frontend)

### 🎯 One-Command Startup (Recommended)

Start MongoDB, backend, and frontend with a single command:

```bash
./start-all.sh
```

This script will:
1. ✅ Check if MongoDB is running (starts it if needed)
2. ✅ Build the backend (Maven)
3. ✅ Start Spring Boot backend on port 8081
4. ✅ Start React frontend on port 3000
5. ✅ Display URLs and credentials

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
| `admin` | `admin123` | ADMIN | Full access to all features |
| `user` | `user12345` | USER | Read, Create, Update members |

### Login Example

```bash
# Get JWT token
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

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

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
- Java 17 or 21
- Maven 3.8+
- MongoDB 6.0+ (or Docker)
- Docker (optional, for containerized MongoDB)

### 1. Start MongoDB

**Option A: Using Docker (Recommended)**
```bash
docker run -d --name mongodb -p 27017:27017 mongo:7.0
```

**Option B: Local MongoDB**
```bash
mongod --dbpath /path/to/data
```

### 2. Build the Application
```bash
./mvnw clean package
```

### 3. Run the Application
```bash
./mvnw spring-boot:run
```

The application starts on **http://localhost:8081**

### 4. Access the Application

| URL | Description |
|-----|-------------|
| http://localhost:8081/swagger-ui.html | API Documentation |
| http://localhost:8081/actuator/health | Health Check |
| http://localhost:8081/api/v1/members | Members API |

## 🔐 Authentication

### Default Users

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| `admin` | `admin123` | ADMIN | Full access |
| `user` | `user123` | USER | Read, Create, Update members |

### Login Example
```bash
# Get JWT token
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# Use token for API calls
curl http://localhost:8081/api/v1/members \
  -H "Authorization: Bearer <your-token>"
```

## 📚 API Endpoints

### Authentication
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/login` | Login | No |
| POST | `/api/v1/auth/register` | Register new user | No |
| POST | `/api/v1/auth/refresh` | Refresh token | No |

### Members
| Method | Endpoint | Description | Permission |
|--------|----------|-------------|------------|
| GET | `/api/v1/members` | List all members | `member:read` |
| GET | `/api/v1/members/{id}` | Get member by ID | `member:read` |
| POST | `/api/v1/members` | Create member | `member:create` |
| PUT | `/api/v1/members/{id}` | Update member | `member:update` |
| DELETE | `/api/v1/members/{id}` | Delete member | `member:delete` |

## 🏗️ Project Structure

```
src/main/java/com/modernizedkitechensink/kitchensinkmodernized/
├── audit/                  # Audit logging (AOP)
├── config/                 # Configuration classes
│   ├── SecurityConfig.java
│   ├── CacheConfig.java
│   ├── AsyncConfig.java
│   └── OpenApiConfig.java
├── controller/             # REST controllers
├── dto/                    # Data Transfer Objects
├── exception/              # Custom exceptions & handlers
├── model/                  # Domain entities
│   └── auth/              # User, Role, Permission
├── repository/             # MongoDB repositories
├── security/               # JWT & authentication
└── service/                # Business logic
```

## 🧪 Testing

### Run Unit Tests
```bash
./mvnw test
```

### Run Integration Tests (requires Docker)
```bash
./mvnw verify
```

### Test Coverage
- Unit tests: Service layer with Mockito
- Integration tests: Full API with Testcontainers

## 🐳 Docker

### Build Docker Image
```bash
docker build -t kitchensink-modernized .
```

### Run with Docker Compose
```bash
docker-compose up -d
```

## 📊 Monitoring

### Actuator Endpoints
| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Metrics data |
| `/actuator/caches` | Cache statistics |

## 🔄 Migration Highlights

### Key Changes from Legacy

1. **JPA → MongoDB**
   - `@Entity` → `@Document`
   - `EntityManager` → `MongoRepository`
   - SQL → MongoDB queries

2. **EJB → Spring**
   - `@Stateless` → `@Service`
   - `@Inject` → `@Autowired` / constructor injection

3. **JAX-RS → Spring MVC**
   - `@Path` → `@RequestMapping`
   - `@GET/@POST` → `@GetMapping/@PostMapping`

4. **Container Security → Spring Security**
   - `web.xml` roles → JWT + `@PreAuthorize`
   - Session-based → Stateless tokens

## 📝 License

Apache 2.0

## 👤 Author

Developed as part of the MongoDB Modernization Factory Challenge.

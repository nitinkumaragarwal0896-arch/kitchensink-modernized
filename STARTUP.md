# Startup Scripts

Quick startup and shutdown scripts for the entire Kitchensink Modernized application stack.

## 🚀 Quick Start

### Start All Services

```bash
./start-all.sh
```

This single command will:
1. ✅ Check/Start **MongoDB** (port 27017)
2. ✅ Build & Start **Backend** (port 8081)
3. ✅ Start **Frontend** (port 5173)

### Stop All Services

```bash
./stop-all.sh
```

Stops backend and frontend (keeps MongoDB running).

## 📋 What Gets Started

| Service  | Port | URL |
|----------|------|-----|
| Frontend | 5173 | http://localhost:5173 |
| Backend  | 8081 | http://localhost:8081 |
| Swagger  | 8081 | http://localhost:8081/swagger-ui/index.html |
| MongoDB  | 27017 | mongodb://localhost:27017 |

## 🔧 Features

- **Automatic Port Checking**: Detects if services are already running
- **Smart Restart**: Asks before killing existing services
- **MongoDB Management**: Auto-starts MongoDB via Homebrew
- **Build Progress**: Shows real-time Maven build progress
- **Health Checks**: Waits for services to be fully started
- **Colored Output**: Clear visual status indicators
- **Log Files**: All output saved to `/tmp/kitchensink-*.log`

## 📝 Log Files

View logs in real-time:

```bash
# Backend logs
tail -f /tmp/kitchensink-backend.log

# Frontend logs
tail -f /tmp/kitchensink-frontend.log

# Build logs (if build fails)
cat /tmp/kitchensink-backend-build.log
```

## 🔐 Default Credentials

| Username | Password | Role |
|----------|----------|------|
| admin    | admin123 | Admin (Full Access) |
| user     | user12345 | User (Member CRUD) |

## ⚠️ Prerequisites

- **Java 21**: `java -version`
- **Maven**: `mvn -version`
- **Node.js 18+**: `node -version`
- **MongoDB**: `brew services list | grep mongodb`
- **Homebrew**: (for MongoDB management)

## 🛠️ Troubleshooting

### MongoDB won't start
```bash
# Check if MongoDB is installed
brew services list

# Start MongoDB manually
brew services start mongodb-community

# OR
mongod --config /usr/local/etc/mongod.conf
```

### Backend won't start
```bash
# Check Java version
java -version  # Should be 21

# Check Maven
mvn -version

# Check logs
cat /tmp/kitchensink-backend-build.log
tail -f /tmp/kitchensink-backend.log
```

### Frontend won't start
```bash
# Check Node version
node -version  # Should be 18+

# Reinstall dependencies
cd /Users/nitina/IdeaProjects/kitchensink-spring-mongodb/frontend
rm -rf node_modules package-lock.json
npm install

# Check logs
tail -f /tmp/kitchensink-frontend.log
```

### Port already in use
```bash
# Find process using port
lsof -i :8081  # Backend
lsof -i :5173  # Frontend

# Kill process
kill -9 <PID>

# Or let the script handle it (it will ask)
```

## 🎯 Usage Examples

### Demo Startup (from scratch)
```bash
# 1. Clone/navigate to project
cd /Users/nitina/IdeaProjects/kitchensink-modernized

# 2. Start everything
./start-all.sh

# 3. Open browser
open http://localhost:5173

# 4. Login with: admin / admin123
```

### Development Workflow
```bash
# Start all services
./start-all.sh

# ... make code changes ...

# Stop everything
./stop-all.sh

# Restart (or just restart the changed service manually)
./start-all.sh
```

### Interview Demo
```bash
# Before interview
./start-all.sh

# Show application
open http://localhost:5173

# Show API docs
open http://localhost:8081/swagger-ui/index.html

# After interview
./stop-all.sh
```

## 📂 Script Locations

- **Start Script**: `/Users/nitina/IdeaProjects/kitchensink-modernized/start-all.sh`
- **Stop Script**: `/Users/nitina/IdeaProjects/kitchensink-modernized/stop-all.sh`
- **Backend Dir**: `/Users/nitina/IdeaProjects/kitchensink-modernized`
- **Frontend Dir**: `/Users/nitina/IdeaProjects/kitchensink-spring-mongodb/frontend`

## 🔄 Manual Start (Alternative)

If scripts don't work, start manually:

```bash
# Terminal 1: MongoDB
brew services start mongodb-community

# Terminal 2: Backend
cd /Users/nitina/IdeaProjects/kitchensink-modernized
mvn spring-boot:run

# Terminal 3: Frontend
cd /Users/nitina/IdeaProjects/kitchensink-spring-mongodb/frontend
npm run dev
```

## ✨ Tips

- **First Time**: May take 1-2 minutes for Maven build
- **Subsequent Runs**: Much faster (~10-15 seconds)
- **Keep Logs Open**: `tail -f /tmp/kitchensink-*.log` in separate terminal
- **Browser**: Frontend auto-opens at http://localhost:5173
- **API Testing**: Use Swagger UI at http://localhost:8081/swagger-ui/index.html


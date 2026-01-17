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
3. ✅ Start **Frontend** (port 3000)

### Stop All Services

```bash
./stop-all.sh
```

Stops backend and frontend (keeps MongoDB running).

## 📋 What Gets Started

| Service  | Port | URL |
|----------|------|-----|
| Frontend | 3000 | http://localhost:3000 |
| Backend  | 8081 | http://localhost:8081 |
| Swagger  | 8081 | http://localhost:8081/swagger-ui/index.html |
| MongoDB  | 27017 | mongodb://localhost:27017 |

## 🔧 Features

- **MongoDB Auto-Setup**: Interactive prompt with 3 options (Homebrew, Docker, Manual)
- **Docker Support**: Automatically pulls and runs MongoDB container if not installed
- **Automatic Port Checking**: Detects if services are already running
- **Smart Restart**: Asks before killing existing services
- **Build Progress**: Shows real-time Maven build progress
- **Health Checks**: Waits for services to be fully started
- **Colored Output**: Clear visual status indicators
- **Log Files**: All output saved to `/tmp/kitchensink-*.log`

## 🐳 MongoDB Setup Options

When you run `./start-all.sh`, if MongoDB is not running, you'll see:

```
MongoDB Setup Options:
  1. Start existing MongoDB (Homebrew)
  2. Start MongoDB with Docker (recommended if not installed)
  3. Exit and install MongoDB manually

Choose option (1/2/3):
```

### Option 1: Homebrew (macOS)
- Starts MongoDB installed via `brew install mongodb-community`
- Best for: macOS users with MongoDB already installed

### Option 2: Docker ⭐ **Recommended for First-Time Users**
- Automatically pulls `mongo:7.0` Docker image (if needed)
- Creates container named `mongodb` with persistent volume
- No manual MongoDB installation required!
- Works on **macOS, Linux, and Windows** (with Docker Desktop)

**First run:**
```bash
# Pulls MongoDB image and creates container
docker run -d --name mongodb -p 27017:27017 -v mongodb_data:/data/db mongo:7.0
```

**Subsequent runs:**
```bash
# Reuses existing container and data
docker start mongodb
```

### Option 3: Manual Installation
- Displays installation instructions for your platform
- Exits script so you can install MongoDB first

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


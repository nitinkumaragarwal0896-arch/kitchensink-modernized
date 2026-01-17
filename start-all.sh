#!/bin/bash

# Kitchensink Modernized - Startup Script
# This script starts MongoDB, Backend (Spring Boot), and Frontend (React)

set -e  # Exit on error

# Set JAVA_HOME to Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Directories
BACKEND_DIR="/Users/nitina/IdeaProjects/kitchensink-modernized"
FRONTEND_DIR="/Users/nitina/IdeaProjects/kitchensink-spring-mongodb/frontend"

# Function to check if a port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to print status
print_status() {
    local status=$1
    local message=$2
    if [ "$status" == "success" ]; then
        echo -e "${GREEN}✓${NC} $message"
    elif [ "$status" == "error" ]; then
        echo -e "${RED}✗${NC} $message"
    elif [ "$status" == "info" ]; then
        echo -e "${BLUE}ℹ${NC} $message"
    elif [ "$status" == "warning" ]; then
        echo -e "${YELLOW}⚠${NC} $message"
    fi
}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Kitchensink Modernized - Startup${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check Java version
echo -e "${YELLOW}Java Version Check:${NC}"
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
print_status "info" "Using Java: $JAVA_VERSION"
print_status "info" "JAVA_HOME: $JAVA_HOME"
echo ""

# Step 1: Check MongoDB
echo -e "${YELLOW}[1/3] Checking MongoDB...${NC}"
if check_port 27017; then
    print_status "success" "MongoDB is already running on port 27017"
else
    print_status "warning" "MongoDB is not running. Starting MongoDB..."
    
    # Try to start MongoDB using brew services
    if command -v brew &> /dev/null; then
        brew services start mongodb-community &> /dev/null || brew services restart mongodb-community &> /dev/null
        sleep 3
        
        if check_port 27017; then
            print_status "success" "MongoDB started successfully"
        else
            print_status "error" "Failed to start MongoDB automatically"
            echo -e "${RED}Please start MongoDB manually:${NC}"
            echo "  brew services start mongodb-community"
            echo "  OR"
            echo "  mongod --config /usr/local/etc/mongod.conf"
            exit 1
        fi
    else
        print_status "error" "Homebrew not found. Please start MongoDB manually"
        exit 1
    fi
fi
echo ""

# Step 2: Start Backend (Spring Boot)
echo -e "${YELLOW}[2/3] Starting Backend (Spring Boot on port 8081)...${NC}"

# Check if backend is already running
if check_port 8081; then
    print_status "warning" "Backend is already running on port 8081"
    read -p "Kill and restart? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "info" "Stopping existing backend..."
        lsof -ti:8081 | xargs kill -9 2>/dev/null || true
        sleep 2
    else
        print_status "info" "Keeping existing backend"
    fi
fi

if ! check_port 8081; then
    print_status "info" "Building and starting backend..."
    cd "$BACKEND_DIR"
    
    # Build the project
    print_status "info" "Running Maven build (skipping tests)..."
    mvn clean package -DskipTests > /tmp/kitchensink-backend-build.log 2>&1 &
    BUILD_PID=$!
    
    # Show spinner while building
    spin='-\|/'
    i=0
    while kill -0 $BUILD_PID 2>/dev/null; do
        i=$(( (i+1) %4 ))
        printf "\r${BLUE}Building backend ${spin:$i:1}${NC}"
        sleep .1
    done
    wait $BUILD_PID
    BUILD_EXIT=$?
    printf "\r"
    
    if [ $BUILD_EXIT -eq 0 ]; then
        print_status "success" "Backend built successfully"
        
        # Start the application
        print_status "info" "Starting Spring Boot application..."
        nohup java -jar target/kitchensink-modernized-*.jar > /tmp/kitchensink-backend.log 2>&1 &
        BACKEND_PID=$!
        
        # Wait for backend to start (check port)
        print_status "info" "Waiting for backend to start..."
        for i in {1..30}; do
            if check_port 8081; then
                print_status "success" "Backend started successfully (PID: $BACKEND_PID)"
                echo -e "${GREEN}   → Backend running at: http://localhost:8081${NC}"
                echo -e "${GREEN}   → Swagger UI: http://localhost:8081/swagger-ui/index.html${NC}"
                echo -e "${GREEN}   → Logs: tail -f /tmp/kitchensink-backend.log${NC}"
                break
            fi
            sleep 1
            if [ $i -eq 30 ]; then
                print_status "error" "Backend failed to start within 30 seconds"
                echo -e "${RED}Check logs: tail -f /tmp/kitchensink-backend.log${NC}"
                exit 1
            fi
        done
    else
        print_status "error" "Backend build failed"
        echo -e "${RED}Check build logs: cat /tmp/kitchensink-backend-build.log${NC}"
        exit 1
    fi
fi
echo ""

# Step 3: Start Frontend (React)
echo -e "${YELLOW}[3/3] Starting Frontend (React on port 3000)...${NC}"

# Check if frontend is already running
if check_port 3000; then
    print_status "warning" "Frontend is already running on port 3000"
    read -p "Kill and restart? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "info" "Stopping existing frontend..."
        lsof -ti:3000 | xargs kill -9 2>/dev/null || true
        sleep 2
    else
        print_status "info" "Keeping existing frontend"
    fi
fi

if ! check_port 3000; then
    print_status "info" "Starting frontend development server..."
    cd "$FRONTEND_DIR"
    
    # Check if node_modules exists
    if [ ! -d "node_modules" ]; then
        print_status "warning" "node_modules not found. Running npm install..."
        npm install > /tmp/kitchensink-frontend-install.log 2>&1
        if [ $? -eq 0 ]; then
            print_status "success" "Dependencies installed"
        else
            print_status "error" "npm install failed"
            echo -e "${RED}Check logs: cat /tmp/kitchensink-frontend-install.log${NC}"
            exit 1
        fi
    fi
    
    # Start frontend
    nohup npm run dev > /tmp/kitchensink-frontend.log 2>&1 &
    FRONTEND_PID=$!
    
    # Wait for frontend to start
    print_status "info" "Waiting for frontend to start..."
    for i in {1..20}; do
        if check_port 3000; then
            print_status "success" "Frontend started successfully (PID: $FRONTEND_PID)"
            echo -e "${GREEN}   → Frontend running at: http://localhost:3000${NC}"
            echo -e "${GREEN}   → Logs: tail -f /tmp/kitchensink-frontend.log${NC}"
            break
        fi
        sleep 1
        if [ $i -eq 20 ]; then
            print_status "error" "Frontend failed to start within 20 seconds"
            echo -e "${RED}Check logs: tail -f /tmp/kitchensink-frontend.log${NC}"
            exit 1
        fi
    done
fi
echo ""

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✓ All services started successfully!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Service URLs:${NC}"
echo -e "  ${BLUE}Frontend:${NC}       http://localhost:3000"
echo -e "  ${BLUE}Backend API:${NC}    http://localhost:8081"
echo -e "  ${BLUE}Swagger UI:${NC}     http://localhost:8081/swagger-ui/index.html"
echo -e "  ${BLUE}MongoDB:${NC}        mongodb://localhost:27017"
echo ""
echo -e "${YELLOW}Default Credentials:${NC}"
echo -e "  ${BLUE}Admin:${NC}  admin / admin123"
echo -e "  ${BLUE}User:${NC}   user / user12345"
echo ""
echo -e "${YELLOW}Logs:${NC}"
echo -e "  ${BLUE}Backend:${NC}   tail -f /tmp/kitchensink-backend.log"
echo -e "  ${BLUE}Frontend:${NC}  tail -f /tmp/kitchensink-frontend.log"
echo ""
echo -e "${YELLOW}To stop all services:${NC}"
echo -e "  ./stop-all.sh"
echo ""
echo -e "${GREEN}Press Ctrl+C to stop this script (services will continue running)${NC}"
echo ""

# Keep script running to show it's done
read -p "Press Enter to exit..."


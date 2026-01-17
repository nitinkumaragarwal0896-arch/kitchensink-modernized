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

# Directories - Automatically detect based on script location
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BACKEND_DIR="$SCRIPT_DIR"

# Try to find frontend in common locations
if [ -d "$SCRIPT_DIR/../kitchensink-frontend" ]; then
    FRONTEND_DIR="$SCRIPT_DIR/../kitchensink-frontend"
elif [ -d "$SCRIPT_DIR/../kitchensink-spring-mongodb/frontend" ]; then
    FRONTEND_DIR="$SCRIPT_DIR/../kitchensink-spring-mongodb/frontend"
else
    FRONTEND_DIR=""
fi

# If frontend not found, prompt user
if [ -z "$FRONTEND_DIR" ] || [ ! -d "$FRONTEND_DIR" ]; then
    echo -e "${YELLOW}⚠ Frontend directory not found automatically${NC}"
    echo -e "${BLUE}Checked locations:${NC}"
    echo "  - $SCRIPT_DIR/../kitchensink-frontend"
    echo "  - $SCRIPT_DIR/../kitchensink-spring-mongodb/frontend"
    echo ""
    echo -e "${BLUE}Please enter the full path to the frontend directory:${NC}"
    read -r FRONTEND_DIR
    if [ ! -d "$FRONTEND_DIR" ]; then
        echo -e "${RED}✗ Frontend directory not found at: $FRONTEND_DIR${NC}"
        echo -e "${YELLOW}Continuing without frontend (backend-only mode)${NC}"
        FRONTEND_DIR=""
    fi
fi

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
    print_status "warning" "MongoDB is not running on port 27017"
    echo ""
    echo -e "${BLUE}MongoDB Setup Options:${NC}"
    echo "  1. Start existing MongoDB (Homebrew)"
    echo "  2. Start MongoDB with Docker (recommended if not installed)"
    echo "  3. Exit and install MongoDB manually"
    echo ""
    read -p "Choose option (1/2/3): " -n 1 -r
    echo ""
    
    if [[ $REPLY == "1" ]]; then
        print_status "info" "Attempting to start MongoDB via Homebrew..."
        
        if command -v brew &> /dev/null; then
            if brew services list | grep -q "mongodb-community"; then
                brew services start mongodb-community &> /dev/null || brew services restart mongodb-community &> /dev/null
                sleep 3
                
                if check_port 27017; then
                    print_status "success" "MongoDB started successfully"
                else
                    print_status "error" "Failed to start MongoDB"
                    echo -e "${YELLOW}Try manually: brew services start mongodb-community${NC}"
                    exit 1
                fi
            else
                print_status "error" "MongoDB not installed via Homebrew"
                echo -e "${YELLOW}Install with: brew install mongodb-community${NC}"
                exit 1
            fi
        else
            print_status "error" "Homebrew not found"
            echo -e "${YELLOW}Install Homebrew: https://brew.sh${NC}"
            exit 1
        fi
        
    elif [[ $REPLY == "2" ]]; then
        print_status "info" "Starting MongoDB with Docker..."
        
        if command -v docker &> /dev/null; then
            # Check if mongodb container already exists
            if docker ps -a --format '{{.Names}}' | grep -q "^mongodb$"; then
                print_status "info" "MongoDB container exists, starting..."
                docker start mongodb &> /dev/null
            else
                print_status "info" "Creating new MongoDB container..."
                docker run -d --name mongodb -p 27017:27017 \
                    -v mongodb_data:/data/db \
                    mongo:7.0 &> /dev/null
            fi
            
            # Wait for MongoDB to be ready
            print_status "info" "Waiting for MongoDB to be ready..."
            for i in {1..20}; do
                if check_port 27017; then
                    print_status "success" "MongoDB started successfully in Docker"
                    break
                fi
                sleep 1
                if [ $i -eq 20 ]; then
                    print_status "error" "MongoDB failed to start within 20 seconds"
                    exit 1
                fi
            done
        else
            print_status "error" "Docker not found"
            echo -e "${YELLOW}Install Docker: https://docs.docker.com/get-docker/${NC}"
            exit 1
        fi
        
    else
        print_status "info" "Exiting. Please install MongoDB and try again."
        echo ""
        echo -e "${BLUE}Installation Instructions:${NC}"
        echo -e "${YELLOW}macOS (Homebrew):${NC}"
        echo "  brew install mongodb-community"
        echo "  brew services start mongodb-community"
        echo ""
        echo -e "${YELLOW}Docker (All platforms):${NC}"
        echo "  docker run -d --name mongodb -p 27017:27017 mongo:7.0"
        echo ""
        echo -e "${YELLOW}Linux (Ubuntu/Debian):${NC}"
        echo "  sudo apt-get install mongodb-org"
        echo "  sudo systemctl start mongod"
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
if [ -n "$FRONTEND_DIR" ] && [ -d "$FRONTEND_DIR" ]; then
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
else
    echo -e "${YELLOW}[3/3] Skipping Frontend (not found)${NC}"
    print_status "info" "Frontend directory not configured. Backend-only mode."
    print_status "info" "To add frontend, clone: git clone https://github.com/nitinkumaragarwal0896-arch/kitchensink-frontend.git"
    echo ""
fi

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✓ Services started successfully!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Service URLs:${NC}"
if [ -n "$FRONTEND_DIR" ] && [ -d "$FRONTEND_DIR" ]; then
    echo -e "  ${BLUE}Frontend:${NC}       http://localhost:3000"
fi
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
if [ -n "$FRONTEND_DIR" ] && [ -d "$FRONTEND_DIR" ]; then
    echo -e "  ${BLUE}Frontend:${NC}  tail -f /tmp/kitchensink-frontend.log"
fi
echo ""
echo -e "${YELLOW}To stop all services:${NC}"
echo -e "  ./stop-all.sh"
echo ""
echo -e "${GREEN}Press Ctrl+C to stop this script (services will continue running)${NC}"
echo ""

# Keep script running to show it's done
read -p "Press Enter to exit..."


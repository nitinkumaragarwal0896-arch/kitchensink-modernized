#!/bin/bash

# Kitchensink Modernized - Stop Script
# This script stops Backend and Frontend (keeps MongoDB running)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Kitchensink Modernized - Shutdown${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

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
    fi
}

# Stop Backend (port 8081)
echo -e "${YELLOW}Stopping Backend...${NC}"
if lsof -Pi :8081 -sTCP:LISTEN -t >/dev/null 2>&1; then
    lsof -ti:8081 | xargs kill -9 2>/dev/null
    print_status "success" "Backend stopped"
else
    print_status "info" "Backend was not running"
fi

# Stop Frontend (port 3000)
echo -e "${YELLOW}Stopping Frontend...${NC}"
if lsof -Pi :3000 -sTCP:LISTEN -t >/dev/null 2>&1; then
    lsof -ti:3000 | xargs kill -9 2>/dev/null
    print_status "success" "Frontend stopped"
else
    print_status "info" "Frontend was not running"
fi

echo ""
echo -e "${GREEN}All services stopped!${NC}"
echo ""
echo -e "${YELLOW}Note:${NC} MongoDB is still running (use 'brew services stop mongodb-community' to stop it)"
echo ""


#!/bin/bash

# Time Tracking Integration Test Script
# Tests Clock In/Out functionality with Time Logging Service

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
APPOINTMENT_SERVICE_URL="http://localhost:8083"
TIME_LOGGING_SERVICE_URL="http://localhost:8085"
EMPLOYEE_ID="emp-test-001"
APPOINTMENT_ID="test-appointment-001"

# Get JWT token (you'll need to implement this based on your auth service)
# For now, using a placeholder
JWT_TOKEN="your-jwt-token-here"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Time Tracking Integration Test${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Test 1: Clock In
echo -e "${YELLOW}Test 1: Clock In${NC}"
echo "POST ${APPOINTMENT_SERVICE_URL}/appointments/${APPOINTMENT_ID}/clock-in"
echo ""

CLOCK_IN_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST "${APPOINTMENT_SERVICE_URL}/appointments/${APPOINTMENT_ID}/clock-in" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-User-Subject: ${EMPLOYEE_ID}" \
  -H "Content-Type: application/json")

HTTP_STATUS=$(echo "$CLOCK_IN_RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)
RESPONSE_BODY=$(echo "$CLOCK_IN_RESPONSE" | sed '/HTTP_STATUS/d')

if [ "$HTTP_STATUS" == "200" ]; then
  echo -e "${GREEN}✓ Clock In Successful${NC}"
  echo "$RESPONSE_BODY" | jq '.'
  
  # Extract timeLogId from response
  TIME_SESSION_ID=$(echo "$RESPONSE_BODY" | jq -r '.id')
  TIME_LOG_ID=$(echo "$RESPONSE_BODY" | jq -r '.timeLogId // empty')
  
  echo ""
  echo "Time Session ID: $TIME_SESSION_ID"
  if [ -n "$TIME_LOG_ID" ]; then
    echo "Time Log ID: $TIME_LOG_ID"
  fi
else
  echo -e "${RED}✗ Clock In Failed (HTTP $HTTP_STATUS)${NC}"
  echo "$RESPONSE_BODY"
fi

echo ""
echo "---"
echo ""

# Test 2: Get Active Time Session
echo -e "${YELLOW}Test 2: Get Active Time Session${NC}"
echo "GET ${APPOINTMENT_SERVICE_URL}/appointments/${APPOINTMENT_ID}/time-session"
echo ""

sleep 2 # Wait 2 seconds to show elapsed time

SESSION_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X GET "${APPOINTMENT_SERVICE_URL}/appointments/${APPOINTMENT_ID}/time-session" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-User-Subject: ${EMPLOYEE_ID}")

HTTP_STATUS=$(echo "$SESSION_RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)
RESPONSE_BODY=$(echo "$SESSION_RESPONSE" | sed '/HTTP_STATUS/d')

if [ "$HTTP_STATUS" == "200" ]; then
  echo -e "${GREEN}✓ Active Session Found${NC}"
  echo "$RESPONSE_BODY" | jq '.'
  
  ELAPSED_SECONDS=$(echo "$RESPONSE_BODY" | jq -r '.elapsedSeconds')
  echo ""
  echo "Elapsed Time: ${ELAPSED_SECONDS} seconds"
else
  echo -e "${RED}✗ Failed to Get Session (HTTP $HTTP_STATUS)${NC}"
  echo "$RESPONSE_BODY"
fi

echo ""
echo "---"
echo ""

# Test 3: Wait and show timer
echo -e "${YELLOW}Test 3: Live Timer Simulation${NC}"
echo "Waiting 5 seconds to simulate work..."
echo ""

for i in {1..5}; do
  sleep 1
  
  # Poll for updated elapsed time
  SESSION_RESPONSE=$(curl -s \
    -X GET "${APPOINTMENT_SERVICE_URL}/appointments/${APPOINTMENT_ID}/time-session" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "X-User-Subject: ${EMPLOYEE_ID}")
  
  ELAPSED=$(echo "$SESSION_RESPONSE" | jq -r '.elapsedSeconds // 0')
  HOURS=$((ELAPSED / 3600))
  MINUTES=$(((ELAPSED % 3600) / 60))
  SECONDS=$((ELAPSED % 60))
  
  printf "Timer: %02d:%02d:%02d\r" $HOURS $MINUTES $SECONDS
done

echo ""
echo ""
echo "---"
echo ""

# Test 4: Clock Out
echo -e "${YELLOW}Test 4: Clock Out${NC}"
echo "POST ${APPOINTMENT_SERVICE_URL}/appointments/${APPOINTMENT_ID}/clock-out"
echo ""

CLOCK_OUT_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST "${APPOINTMENT_SERVICE_URL}/appointments/${APPOINTMENT_ID}/clock-out" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-User-Subject: ${EMPLOYEE_ID}" \
  -H "Content-Type: application/json")

HTTP_STATUS=$(echo "$CLOCK_OUT_RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)
RESPONSE_BODY=$(echo "$CLOCK_OUT_RESPONSE" | sed '/HTTP_STATUS/d')

if [ "$HTTP_STATUS" == "200" ]; then
  echo -e "${GREEN}✓ Clock Out Successful${NC}"
  echo "$RESPONSE_BODY" | jq '.'
  
  HOURS_WORKED=$(echo "$RESPONSE_BODY" | jq -r '.hoursWorked')
  echo ""
  echo "Total Hours Worked: $HOURS_WORKED"
else
  echo -e "${RED}✗ Clock Out Failed (HTTP $HTTP_STATUS)${NC}"
  echo "$RESPONSE_BODY"
fi

echo ""
echo "---"
echo ""

# Test 5: Verify Time Log in Time Logging Service
if [ -n "$TIME_LOG_ID" ]; then
  echo -e "${YELLOW}Test 5: Verify Time Log in Time Logging Service${NC}"
  echo "GET ${TIME_LOGGING_SERVICE_URL}/time-logs/${TIME_LOG_ID}"
  echo ""

  TIME_LOG_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
    -X GET "${TIME_LOGGING_SERVICE_URL}/time-logs/${TIME_LOG_ID}" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "X-User-Subject: ${EMPLOYEE_ID}")

  HTTP_STATUS=$(echo "$TIME_LOG_RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)
  RESPONSE_BODY=$(echo "$TIME_LOG_RESPONSE" | sed '/HTTP_STATUS/d')

  if [ "$HTTP_STATUS" == "200" ]; then
    echo -e "${GREEN}✓ Time Log Found${NC}"
    echo "$RESPONSE_BODY" | jq '.'
  else
    echo -e "${RED}✗ Time Log Not Found (HTTP $HTTP_STATUS)${NC}"
    echo "$RESPONSE_BODY"
  fi
fi

echo ""
echo "---"
echo ""

# Test 6: Get Summary Stats
echo -e "${YELLOW}Test 6: Get Employee Time Summary${NC}"
echo "GET ${TIME_LOGGING_SERVICE_URL}/time-logs/summary?period=daily&date=$(date +%Y-%m-%d)"
echo ""

SUMMARY_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X GET "${TIME_LOGGING_SERVICE_URL}/time-logs/summary?period=daily&date=$(date +%Y-%m-%d)" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-User-Subject: ${EMPLOYEE_ID}")

HTTP_STATUS=$(echo "$SUMMARY_RESPONSE" | grep "HTTP_STATUS" | cut -d: -f2)
RESPONSE_BODY=$(echo "$SUMMARY_RESPONSE" | sed '/HTTP_STATUS/d')

if [ "$HTTP_STATUS" == "200" ]; then
  echo -e "${GREEN}✓ Summary Retrieved${NC}"
  echo "$RESPONSE_BODY" | jq '.'
else
  echo -e "${RED}✗ Summary Failed (HTTP $HTTP_STATUS)${NC}"
  echo "$RESPONSE_BODY"
fi

echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Test Complete${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Checklist
echo "Verification Checklist:"
echo ""
echo "[ ] Clock in created time log with 0 hours"
echo "[ ] Clock in changed appointment status to IN_PROGRESS"
echo "[ ] Active session returns current elapsed time"
echo "[ ] Timer increments every second"
echo "[ ] Clock out calculated correct hours"
echo "[ ] Clock out updated time log with actual hours"
echo "[ ] Clock out changed appointment status to COMPLETED"
echo "[ ] Summary shows today's total hours"
echo ""
echo "Next Steps:"
echo "1. Update JWT_TOKEN in this script with real token"
echo "2. Create a test appointment with valid ID"
echo "3. Run this script to test the full flow"
echo "4. Check database for TimeSession and TimeLog entries"
echo "5. Implement frontend UI components"

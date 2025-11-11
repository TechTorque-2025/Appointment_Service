# Intelligent Time Tracking Implementation

## Overview
Implemented automated time tracking with Clock In/Out functionality that integrates with the Time Logging Service. This eliminates manual time logging and provides live timer functionality for employees.

## Backend Implementation

### 1. New Entity: TimeSession
**Location:** `entity/TimeSession.java`

Tracks active clock-in sessions locally in Appointment Service:
- `id` - UUID primary key
- `appointmentId` - Links to appointment
- `employeeId` - Employee who clocked in
- `clockInTime` - Timestamp when work started (auto-set)
- `clockOutTime` - Timestamp when work ended
- `active` - Boolean flag for active sessions
- `timeLogId` - Reference to Time Logging Service entry

### 2. Repository: TimeSessionRepository
**Location:** `repository/TimeSessionRepository.java`

JPA repository with custom queries:
- `findByAppointmentIdAndActiveTrue()` - Find active session for appointment
- `findByAppointmentIdAndEmployeeIdAndActiveTrue()` - Find specific employee's active session
- `findByEmployeeIdAndActiveTrue()` - All active sessions for employee
- `findByEmployeeIdOrderByClockInTimeDesc()` - Employee's session history

### 3. DTO: TimeSessionResponse
**Location:** `dto/response/TimeSessionResponse.java`

API response containing:
- Session details (id, appointmentId, employeeId)
- Time information (clockInTime, clockOutTime, active)
- **Calculated fields:**
  - `elapsedSeconds` - For live timer display
  - `hoursWorked` - Total hours when completed

### 4. Updated TimeLoggingClient
**Location:** `client/TimeLoggingClient.java`

Added JWT-authenticated methods:
- `createTimeLog()` - Creates entry in Time Logging Service, returns timeLogId
- `updateTimeLog()` - Updates existing log with actual hours worked
- `createAuthHeaders()` - Extracts JWT token for service-to-service auth

### 5. Service Methods
**Location:** `service/impl/AppointmentServiceImpl.java`

#### `clockIn(appointmentId, employeeId)`
1. Validates appointment exists and employee is assigned
2. Checks for existing active session
3. Creates time log in Time Logging Service (0 hours initially)
4. Creates local TimeSession entity
5. Updates appointment status to `IN_PROGRESS`
6. Sends notification to customer
7. Returns TimeSessionResponse with clockInTime

#### `clockOut(appointmentId, employeeId)`
1. Finds active TimeSession
2. Sets clockOutTime to now
3. Calculates hours worked: `(clockOutTime - clockInTime) / 60 minutes`
4. Updates Time Logging Service with actual hours
5. Marks TimeSession as inactive
6. Updates appointment status to `COMPLETED`
7. Sends completion notification with hours worked
8. Returns TimeSessionResponse with total hours

#### `getActiveTimeSession(appointmentId, employeeId)`
- Retrieves active session if exists
- Calculates `elapsedSeconds` for live timer
- Returns null if no active session

### 6. Controller Endpoints
**Location:** `controller/AppointmentController.java`

| Endpoint | Method | Role | Description |
|----------|--------|------|-------------|
| `/appointments/{id}/clock-in` | POST | EMPLOYEE | Start time tracking |
| `/appointments/{id}/clock-out` | POST | EMPLOYEE | Stop time tracking |
| `/appointments/{id}/time-session` | GET | EMPLOYEE | Get active session (for timer) |

All endpoints require JWT authentication with `X-User-Subject` header.

### 7. Integration with Existing Flow
**Updated:** `acceptVehicleArrival()` method

Now automatically calls `clockIn()` when employee accepts vehicle arrival, eliminating the need for manual clock-in after accepting work.

## API Usage

### Clock In
```bash
POST /appointments/{appointmentId}/clock-in
Headers:
  Authorization: Bearer <JWT_TOKEN>
  X-User-Subject: <employeeId>

Response:
{
  "id": "uuid",
  "appointmentId": "appt-123",
  "employeeId": "emp-456",
  "clockInTime": "2025-01-20T10:30:00",
  "clockOutTime": null,
  "active": true,
  "elapsedSeconds": 0,
  "hoursWorked": null
}
```

### Get Active Session (for live timer)
```bash
GET /appointments/{appointmentId}/time-session
Headers:
  Authorization: Bearer <JWT_TOKEN>
  X-User-Subject: <employeeId>

Response:
{
  "id": "uuid",
  "appointmentId": "appt-123",
  "employeeId": "emp-456",
  "clockInTime": "2025-01-20T10:30:00",
  "clockOutTime": null,
  "active": true,
  "elapsedSeconds": 3600,  // Updated in real-time
  "hoursWorked": null
}
```

### Clock Out
```bash
POST /appointments/{appointmentId}/clock-out
Headers:
  Authorization: Bearer <JWT_TOKEN>
  X-User-Subject: <employeeId>

Response:
{
  "id": "uuid",
  "appointmentId": "appt-123",
  "employeeId": "emp-456",
  "clockInTime": "2025-01-20T10:30:00",
  "clockOutTime": "2025-01-20T12:45:00",
  "active": false,
  "elapsedSeconds": 8100,
  "hoursWorked": 2.25
}
```

## Time Logging Service Integration

### Flow Diagram
```
Clock In:
  Appointment Service → Time Logging Service
  POST /time-logs
  {
    "employeeId": "emp-123",
    "serviceId": "appt-456",
    "hours": 0,
    "description": "Work started...",
    "date": "2025-01-20",
    "workType": "SERVICE"
  }
  ← Returns: { "id": "log-789", ... }

Clock Out:
  Appointment Service → Time Logging Service
  PUT /time-logs/log-789
  {
    "hours": 2.25,
    "description": "Completed: 2.25 hours worked"
  }
```

### JWT Authentication
All Time Logging Service requests include:
```
Headers:
  Authorization: Bearer <JWT_TOKEN>
  X-User-Subject: <employeeId>
```

Token is extracted from SecurityContext OAuth2 authentication.

## Frontend Implementation Requirements

### 1. Appointment Details Page Updates

#### Add Clock In/Out Button Component
```typescript
// Conditional rendering based on appointment status
{appointment.status === 'CONFIRMED' && !activeSession && (
  <Button onClick={handleClockIn}>
    <ClockIcon /> Clock In
  </Button>
)}

{appointment.status === 'IN_PROGRESS' && activeSession && (
  <div>
    <Timer elapsedSeconds={activeSession.elapsedSeconds} />
    <Button onClick={handleClockOut}>
      <StopIcon /> Clock Out
    </Button>
  </div>
)}
```

#### API Integration
```typescript
// Clock in
const handleClockIn = async () => {
  const response = await fetch(
    `/api/appointments/${appointmentId}/clock-in`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Subject': employeeId
      }
    }
  );
  const session = await response.json();
  setActiveSession(session);
};

// Clock out
const handleClockOut = async () => {
  const response = await fetch(
    `/api/appointments/${appointmentId}/clock-out`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Subject': employeeId
      }
    }
  );
  const session = await response.json();
  setActiveSession(null);
  showNotification(`Work completed! ${session.hoursWorked} hours logged.`);
};
```

### 2. Live Timer Component
```typescript
const Timer: React.FC<{ elapsedSeconds: number }> = ({ elapsedSeconds }) => {
  const [seconds, setSeconds] = useState(elapsedSeconds);

  useEffect(() => {
    const interval = setInterval(async () => {
      // Fetch updated elapsed time from server
      const response = await fetch(
        `/api/appointments/${appointmentId}/time-session`,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'X-User-Subject': employeeId
          }
        }
      );
      
      if (response.ok) {
        const session = await response.json();
        setSeconds(session.elapsedSeconds);
      }
    }, 1000); // Update every second

    return () => clearInterval(interval);
  }, []);

  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;

  return (
    <div className="timer">
      <span className="font-mono text-2xl">
        {String(hours).padStart(2, '0')}:
        {String(minutes).padStart(2, '0')}:
        {String(secs).padStart(2, '0')}
      </span>
      <span className="text-sm text-gray-600">Time Elapsed</span>
    </div>
  );
};
```

### 3. Time Logs Summary Page

#### Stats Display
Call Time Logging Service endpoints:
```typescript
// Today's hours
GET /api/time-logs/summary?period=daily&date=2025-01-20

// This week
GET /api/time-logs/summary?period=weekly&date=2025-01-20

// Overall stats
GET /api/time-logs/stats

// Recent logs
GET /api/time-logs?employeeId={id}&fromDate=2025-01-01&toDate=2025-01-31
```

#### UI Layout
```
┌─────────────────────────────────────┐
│  Time Tracking Summary              │
├─────────────────────────────────────┤
│  Today:     4.5 hours               │
│  This Week: 22.0 hours              │
│  This Month: 88.5 hours             │
│  Total:     450.0 hours             │
├─────────────────────────────────────┤
│  Recent Time Logs                   │
│  ┌────────────────────────────────┐ │
│  │ Jan 20 | Oil Change | 2.25h   │ │
│  │ Jan 19 | Brake Repair | 3.5h  │ │
│  │ Jan 18 | Inspection | 1.0h    │ │
│  └────────────────────────────────┘ │
└─────────────────────────────────────┘
```

## Benefits

### For Employees
✅ One-click clock in/out
✅ Live timer shows exactly how long they've been working
✅ No manual time entry required
✅ Automatic status updates

### For Business
✅ Accurate time tracking
✅ Real-time work status monitoring
✅ Automated customer notifications
✅ Historical time data for analytics

### Technical
✅ Single source of truth (Time Logging Service)
✅ Local session tracking for quick queries
✅ JWT authentication for security
✅ Transaction-safe operations

## Testing Checklist

- [ ] Clock in creates time log with 0 hours
- [ ] Clock in updates appointment status to IN_PROGRESS
- [ ] Clock in sends customer notification
- [ ] Cannot clock in twice for same appointment
- [ ] Get active session returns correct elapsed time
- [ ] Clock out calculates correct hours
- [ ] Clock out updates time log with actual hours
- [ ] Clock out updates appointment status to COMPLETED
- [ ] Clock out sends completion notification with hours
- [ ] Cannot clock out without active session
- [ ] Authorization: Only assigned employees can clock in/out
- [ ] JWT token properly propagated to Time Logging Service
- [ ] Frontend timer updates every second
- [ ] Summary page shows correct totals

## Next Steps

1. **Build and deploy Appointment Service**
   ```bash
   cd Appointment_Service
   mvn clean package
   docker build -t appointment-service .
   ```

2. **Update Frontend**
   - Add Timer component to appointment details page
   - Implement clock in/out buttons
   - Create time logs summary page

3. **Test Integration**
   - Test clock in → verify time log created
   - Let timer run for 1 minute
   - Clock out → verify hours logged correctly
   - Check Time Logging Service database

4. **Documentation**
   - Update API documentation
   - Add user guide for employees
   - Create admin monitoring dashboard

## Configuration

### Application Properties
No additional configuration needed. Uses existing:
- JWT authentication setup
- Time Logging Service URL from WebClient config
- Database connection for TimeSession table

### Database Migration
Table created automatically by JPA:
```sql
CREATE TABLE time_session (
  id VARCHAR(255) PRIMARY KEY,
  appointment_id VARCHAR(255) NOT NULL,
  employee_id VARCHAR(255) NOT NULL,
  clock_in_time TIMESTAMP NOT NULL,
  clock_out_time TIMESTAMP,
  active BOOLEAN NOT NULL DEFAULT true,
  time_log_id VARCHAR(255) NOT NULL
);
```

## Support
For issues or questions:
- Check logs: `docker logs appointment-service`
- Verify Time Logging Service connectivity
- Confirm JWT token in SecurityContext
- Check database for TimeSession entries

# Appointment Service Documentation

## Overview

The **Appointment Service** is a sophisticated scheduling and appointment management microservice for the TechTorque-2025 platform. It handles appointment booking, availability checking, employee scheduling, time tracking, and calendar management for vehicle service operations. The service includes business hours management, holiday tracking, service bay allocation, and comprehensive status tracking throughout the appointment lifecycle.

## Technology Stack

- **Framework**: Spring Boot 3.5.6
- **Language**: Java 17
- **Database**: PostgreSQL
- **Security**: Spring Security with JWT Bearer Authentication
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Build Tool**: Maven

## Service Configuration

**Port**: 8083
**API Documentation**: http://localhost:8083/swagger-ui/index.html
**Base URL**: http://localhost:8083/api

## Core Features

### 1. Appointment Booking System

**Capabilities**:
- Check real-time availability based on service type and date
- Book appointments with automatic bay allocation
- Generate unique confirmation numbers
- Validate against business hours and holidays
- Prevent double-booking of service bays
- Support for special customer instructions

### 2. Availability Management

**Features**:
- Dynamic availability calculation based on:
  - Business hours (configurable per day of week)
  - Existing appointments and bay capacity
  - Service duration requirements
  - Holiday exclusions
- 30-minute time slot intervals
- Multi-bay support for concurrent appointments

### 3. Time Tracking System

**Capabilities**:
- Clock in/out functionality for employees
- Track actual work hours per appointment
- Automatic duration calculation
- Multiple time sessions per appointment (breaks supported)
- Integration with Time Logging Service for payroll

### 4. Employee Scheduling

**Features**:
- View daily schedule for assigned appointments
- Multi-employee assignment per appointment
- Employee workload visibility
- Calendar view with appointment distribution

### 5. Status Management

**Lifecycle Tracking**:
- `PENDING` - Appointment booked, awaiting approval
- `CONFIRMED` - Appointment approved and confirmed
- `CHECKED_IN` - Customer arrived, vehicle accepted
- `IN_PROGRESS` - Work is ongoing
- `COMPLETED` - Work finished by employee
- `CUSTOMER_CONFIRMED` - Customer confirmed completion
- `CANCELLED` - Appointment cancelled
- `NO_SHOW` - Customer didn't arrive

### 6. Calendar & Reporting

**Capabilities**:
- Monthly calendar view with appointment counts
- Daily statistics (total, by status)
- Employee schedule management
- Appointment filtering by date range, status, vehicle

## API Endpoints

### Appointment Management

#### Book New Appointment
```http
POST /api/appointments
Authorization: Bearer <token>
Role: CUSTOMER

Request Body:
{
  "vehicleId": "vehicle-uuid-123",
  "serviceType": "Oil Change",
  "requestedDateTime": "2025-11-15T10:00:00",
  "specialInstructions": "Please check tire pressure as well"
}

Response: 201 Created
{
  "id": "appt-uuid",
  "customerId": "customer-uuid",
  "vehicleId": "vehicle-uuid-123",
  "serviceType": "Oil Change",
  "requestedDateTime": "2025-11-15T10:00:00",
  "status": "PENDING",
  "confirmationNumber": "APT-2025-001234",
  "assignedBayId": "bay-01",
  "specialInstructions": "Please check tire pressure as well",
  "createdAt": "2025-11-12T14:30:00",
  "updatedAt": "2025-11-12T14:30:00"
}
```

#### List Appointments
```http
GET /api/appointments
Authorization: Bearer <token>
Role: CUSTOMER, EMPLOYEE, ADMIN
Query Params (all optional):
  ?vehicleId=vehicle-uuid
  &status=CONFIRMED
  &fromDate=2025-11-01
  &toDate=2025-11-30

Response: 200 OK
[
  {
    "id": "appt-uuid-1",
    "confirmationNumber": "APT-2025-001234",
    "serviceType": "Oil Change",
    "requestedDateTime": "2025-11-15T10:00:00",
    "status": "CONFIRMED",
    "vehicleId": "vehicle-uuid"
  },
  ...
]
```

**Access Control**:
- Customers: See only their own appointments
- Employees: See only appointments assigned to them
- Admins: See all appointments

#### Get Appointment Details
```http
GET /api/appointments/{appointmentId}
Authorization: Bearer <token>
Role: CUSTOMER, EMPLOYEE, ADMIN

Response: 200 OK
{
  "id": "appt-uuid",
  "customerId": "customer-uuid",
  "vehicleId": "vehicle-uuid",
  "assignedEmployeeIds": ["emp-001", "emp-002"],
  "assignedBayId": "bay-01",
  "confirmationNumber": "APT-2025-001234",
  "serviceType": "Oil Change",
  "requestedDateTime": "2025-11-15T10:00:00",
  "status": "IN_PROGRESS",
  "specialInstructions": "Check tire pressure",
  "vehicleArrivedAt": "2025-11-15T09:55:00",
  "vehicleAcceptedByEmployeeId": "emp-001",
  "createdAt": "2025-11-12T14:30:00",
  "updatedAt": "2025-11-15T10:05:00"
}
```

#### Update Appointment
```http
PUT /api/appointments/{appointmentId}
Authorization: Bearer <token>
Role: CUSTOMER

Request Body:
{
  "requestedDateTime": "2025-11-15T14:00:00",
  "specialInstructions": "Updated: Please also check brake fluid"
}

Response: 200 OK
{
  "id": "appt-uuid",
  "requestedDateTime": "2025-11-15T14:00:00",
  "specialInstructions": "Updated: Please also check brake fluid",
  ...
}
```

**Note**: Customers can only update appointments with status `PENDING` or `CONFIRMED`

#### Cancel Appointment
```http
DELETE /api/appointments/{appointmentId}
Authorization: Bearer <token>
Role: CUSTOMER, EMPLOYEE, ADMIN

Response: 204 No Content
```

#### Update Appointment Status
```http
PATCH /api/appointments/{appointmentId}/status
Authorization: Bearer <token>
Role: EMPLOYEE, ADMIN

Request Body:
{
  "newStatus": "CONFIRMED"
}

Response: 200 OK
{
  "id": "appt-uuid",
  "status": "CONFIRMED",
  ...
}
```

### Availability & Scheduling

#### Check Availability
```http
GET /api/appointments/availability
Authorization: Not required (public endpoint)
Query Params:
  ?date=2025-11-15
  &serviceType=Oil Change
  &duration=60

Response: 200 OK
{
  "date": "2025-11-15",
  "serviceType": "Oil Change",
  "availableSlots": [
    {
      "startTime": "09:00",
      "endTime": "10:00",
      "available": true,
      "availableBays": 2
    },
    {
      "startTime": "09:30",
      "endTime": "10:30",
      "available": true,
      "availableBays": 1
    },
    {
      "startTime": "10:00",
      "endTime": "11:00",
      "available": false,
      "availableBays": 0
    },
    ...
  ],
  "businessHours": {
    "openTime": "08:00",
    "closeTime": "17:00"
  },
  "isHoliday": false
}
```

#### Get Employee Schedule
```http
GET /api/appointments/schedule
Authorization: Bearer <token>
Role: EMPLOYEE
Query Params: ?date=2025-11-15

Response: 200 OK
{
  "employeeId": "emp-001",
  "date": "2025-11-15",
  "appointments": [
    {
      "id": "appt-uuid-1",
      "confirmationNumber": "APT-2025-001234",
      "serviceType": "Oil Change",
      "startTime": "09:00",
      "endTime": "10:00",
      "status": "CONFIRMED",
      "customerName": "John Doe",
      "vehicleInfo": "Honda Civic 2020"
    },
    {
      "id": "appt-uuid-2",
      "confirmationNumber": "APT-2025-001235",
      "serviceType": "Brake Service",
      "startTime": "11:00",
      "endTime": "13:00",
      "status": "IN_PROGRESS",
      "customerName": "Jane Smith",
      "vehicleInfo": "Toyota Camry 2019"
    }
  ],
  "totalHours": 4,
  "appointmentCount": 2
}
```

#### Get Monthly Calendar
```http
GET /api/appointments/calendar
Authorization: Bearer <token>
Role: EMPLOYEE, ADMIN
Query Params: ?year=2025&month=11

Response: 200 OK
{
  "year": 2025,
  "month": 11,
  "days": [
    {
      "date": "2025-11-01",
      "dayOfWeek": "SATURDAY",
      "appointmentCount": 12,
      "statusCounts": {
        "CONFIRMED": 8,
        "IN_PROGRESS": 3,
        "COMPLETED": 1
      },
      "isHoliday": false,
      "isWeekend": true
    },
    ...
  ],
  "statistics": {
    "totalAppointments": 245,
    "byStatus": {
      "PENDING": 15,
      "CONFIRMED": 180,
      "IN_PROGRESS": 20,
      "COMPLETED": 200,
      "CANCELLED": 30
    },
    "averagePerDay": 8.2,
    "busiestDay": "2025-11-15"
  }
}
```

### Employee Operations

#### Assign Employees to Appointment
```http
POST /api/appointments/{appointmentId}/assign-employees
Authorization: Bearer <token>
Role: ADMIN

Request Body:
{
  "employeeIds": ["emp-001", "emp-002"]
}

Response: 200 OK
{
  "id": "appt-uuid",
  "assignedEmployeeIds": ["emp-001", "emp-002"],
  ...
}
```

#### Accept Vehicle Arrival
```http
POST /api/appointments/{appointmentId}/accept-vehicle
Authorization: Bearer <token>
Role: EMPLOYEE

Response: 200 OK
{
  "id": "appt-uuid",
  "status": "CHECKED_IN",
  "vehicleArrivedAt": "2025-11-15T09:55:00",
  "vehicleAcceptedByEmployeeId": "emp-001",
  ...
}
```

**Effect**: Changes appointment status from `CONFIRMED` to `CHECKED_IN`

#### Mark Work Complete
```http
POST /api/appointments/{appointmentId}/complete
Authorization: Bearer <token>
Role: EMPLOYEE

Response: 200 OK
{
  "id": "appt-uuid",
  "status": "COMPLETED",
  ...
}
```

**Effect**: Changes status to `COMPLETED`, awaiting customer confirmation

### Time Tracking

#### Clock In
```http
POST /api/appointments/{appointmentId}/clock-in
Authorization: Bearer <token>
Role: EMPLOYEE

Response: 200 OK
{
  "sessionId": "session-uuid",
  "appointmentId": "appt-uuid",
  "employeeId": "emp-001",
  "clockInTime": "2025-11-15T10:00:00",
  "clockOutTime": null,
  "duration": null,
  "isActive": true
}
```

#### Clock Out
```http
POST /api/appointments/{appointmentId}/clock-out
Authorization: Bearer <token>
Role: EMPLOYEE

Response: 200 OK
{
  "sessionId": "session-uuid",
  "appointmentId": "appt-uuid",
  "employeeId": "emp-001",
  "clockInTime": "2025-11-15T10:00:00",
  "clockOutTime": "2025-11-15T12:30:00",
  "duration": 150,
  "isActive": false
}
```

**Note**: Duration is in minutes. System automatically sends time log to Time Logging Service.

#### Get Active Time Session
```http
GET /api/appointments/{appointmentId}/time-session
Authorization: Bearer <token>
Role: EMPLOYEE

Response: 200 OK
{
  "sessionId": "session-uuid",
  "appointmentId": "appt-uuid",
  "employeeId": "emp-001",
  "clockInTime": "2025-11-15T10:00:00",
  "clockOutTime": null,
  "duration": null,
  "isActive": true
}

Or: 204 No Content (if no active session)
```

### Customer Operations

#### Confirm Completion
```http
POST /api/appointments/{appointmentId}/confirm-completion
Authorization: Bearer <token>
Role: CUSTOMER

Response: 200 OK
{
  "id": "appt-uuid",
  "status": "CUSTOMER_CONFIRMED",
  ...
}
```

**Effect**: Final confirmation that customer received vehicle and is satisfied

## Status Workflow & Transitions

### Valid Status Transitions

```
PENDING ──────────────────────────────┐
   │                                   │
   ├──> CONFIRMED ─────────────────────┤
   │       │                           │
   │       └──> CHECKED_IN             │
   │               │                   │
   │               └──> IN_PROGRESS    │
   │                       │           │
   │                       └──> COMPLETED ──> CUSTOMER_CONFIRMED
   │                                   │
   └──> NO_SHOW                        │
   └──> CANCELLED ◄────────────────────┘
```

### Status Descriptions

| Status | Description | Who Can Set | Next States |
|--------|-------------|-------------|-------------|
| `PENDING` | Booked, awaiting approval | System (on booking) | CONFIRMED, CANCELLED |
| `CONFIRMED` | Approved and ready | Employee/Admin | CHECKED_IN, CANCELLED, NO_SHOW |
| `CHECKED_IN` | Vehicle arrived | Employee | IN_PROGRESS, CANCELLED |
| `IN_PROGRESS` | Work ongoing | Employee | COMPLETED, CANCELLED |
| `COMPLETED` | Work finished | Employee | CUSTOMER_CONFIRMED, CANCELLED |
| `CUSTOMER_CONFIRMED` | Customer confirmed | Customer | (Terminal state) |
| `CANCELLED` | Appointment cancelled | Any role | (Terminal state) |
| `NO_SHOW` | Customer didn't arrive | Employee/Admin | (Terminal state) |

## Database Schema

### appointments
- `id` (UUID, Primary Key)
- `customer_id` (VARCHAR, NOT NULL)
- `vehicle_id` (VARCHAR, NOT NULL)
- `assigned_bay_id` (VARCHAR)
- `confirmation_number` (VARCHAR, UNIQUE)
- `service_type` (VARCHAR, NOT NULL)
- `requested_date_time` (TIMESTAMP, NOT NULL)
- `status` (VARCHAR(30), NOT NULL)
- `special_instructions` (TEXT)
- `vehicle_arrived_at` (TIMESTAMP)
- `vehicle_accepted_by_employee_id` (VARCHAR)
- `created_at` (TIMESTAMP, NOT NULL)
- `updated_at` (TIMESTAMP, NOT NULL)

### appointment_assigned_employees (Collection Table)
- `appointment_id` (UUID, Foreign Key)
- `employee_id` (VARCHAR)

### time_sessions
- `id` (UUID, Primary Key)
- `appointment_id` (UUID, Foreign Key, NOT NULL)
- `employee_id` (VARCHAR, NOT NULL)
- `clock_in_time` (TIMESTAMP, NOT NULL)
- `clock_out_time` (TIMESTAMP)
- `duration_minutes` (INTEGER)
- `is_active` (BOOLEAN, DEFAULT true)
- `created_at` (TIMESTAMP, NOT NULL)
- `updated_at` (TIMESTAMP, NOT NULL)

### service_types
- `id` (UUID, Primary Key)
- `name` (VARCHAR, UNIQUE, NOT NULL)
- `description` (TEXT)
- `estimated_duration_minutes` (INTEGER, NOT NULL)
- `price` (DECIMAL(10,2))
- `is_active` (BOOLEAN, DEFAULT true)
- `created_at` (TIMESTAMP, NOT NULL)

### service_bays
- `id` (UUID, Primary Key)
- `bay_number` (VARCHAR, UNIQUE, NOT NULL)
- `bay_name` (VARCHAR, NOT NULL)
- `is_active` (BOOLEAN, DEFAULT true)
- `capacity` (INTEGER, DEFAULT 1)

### business_hours
- `id` (UUID, Primary Key)
- `day_of_week` (VARCHAR(10), NOT NULL) - MONDAY, TUESDAY, etc.
- `open_time` (TIME, NOT NULL)
- `close_time` (TIME, NOT NULL)
- `is_closed` (BOOLEAN, DEFAULT false)

### holidays
- `id` (UUID, Primary Key)
- `holiday_date` (DATE, NOT NULL, UNIQUE)
- `name` (VARCHAR, NOT NULL)
- `description` (TEXT)

## Environment Configuration

```properties
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=techtorque_appointments
DB_USER=techtorque
DB_PASS=techtorque123
DB_MODE=update

# Profile
SPRING_PROFILE=dev

# Service URLs
ADMIN_SERVICE_URL=http://localhost:8087
TIME_LOGGING_SERVICE_URL=http://localhost:8085
NOTIFICATION_SERVICE_URL=http://localhost:8088
```

## Business Rules

### Booking Rules
1. **Time Slot Validation**: Requested time must fall within business hours
2. **Bay Availability**: At least one service bay must be available
3. **Duration Check**: Appointment duration must not exceed bay availability window
4. **Holiday Check**: Cannot book on holidays (configurable)
5. **Lead Time**: Minimum 30 minutes advance booking (configurable)

### Time Tracking Rules
1. **Single Active Session**: Employee can only have one active session per appointment
2. **Sequential Sessions**: Must clock out before clocking in again
3. **Auto-Calculation**: Duration calculated automatically on clock out
4. **Data Sync**: Time logs automatically sent to Time Logging Service

### Status Transition Rules
1. **Sequential Flow**: Most status changes follow a specific sequence
2. **Role Restrictions**: Only authorized roles can change status
3. **Terminal States**: `CUSTOMER_CONFIRMED`, `CANCELLED`, `NO_SHOW` are final
4. **Validation**: Invalid transitions are rejected with error

## Integration Points

### Time Logging Service
- **Used For**: Recording employee work hours for payroll
- **Trigger**: Automatic on clock-out
- **Endpoint**: `POST /api/time-logs`
- **Data**: Employee ID, appointment ID, clock in/out times, duration

### Notification Service
- **Used For**: Sending appointment notifications
- **Events**:
  - Appointment booked
  - Appointment confirmed
  - Appointment reminder (24h before)
  - Status changes
  - Vehicle ready for pickup
- **Endpoint**: `POST /api/notifications`

### Admin Service
- **Used For**: Fetching service type definitions
- **Endpoint**: `GET /api/service-types`
- **Authentication**: Forward JWT token

## Security & Authorization

### Authentication
- JWT Bearer token required (except `/availability` endpoint)
- Token validated by API Gateway
- User information passed via headers: `X-User-Subject`, `X-User-Roles`

### Authorization Matrix

| Endpoint | CUSTOMER | EMPLOYEE | ADMIN |
|----------|----------|----------|-------|
| Book Appointment | ✅ | ❌ | ❌ |
| List Appointments | ✅ (own) | ✅ (assigned) | ✅ (all) |
| Get Details | ✅ (own) | ✅ (assigned) | ✅ (all) |
| Update Appointment | ✅ (own, before confirmed) | ❌ | ❌ |
| Cancel Appointment | ✅ (own) | ✅ | ✅ |
| Update Status | ❌ | ✅ | ✅ |
| Check Availability | ✅ (public) | ✅ (public) | ✅ (public) |
| Get Schedule | ❌ | ✅ (own) | ❌ |
| Get Calendar | ❌ | ✅ | ✅ |
| Assign Employees | ❌ | ❌ | ✅ |
| Accept Vehicle | ❌ | ✅ | ❌ |
| Complete Work | ❌ | ✅ | ❌ |
| Clock In/Out | ❌ | ✅ | ❌ |
| Confirm Completion | ✅ (own) | ❌ | ❌ |

## Error Handling

### Common Errors

| Status Code | Error | Description |
|-------------|-------|-------------|
| 400 | Bad Request | Invalid date/time, missing required fields |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | User lacks permission for this action |
| 404 | Not Found | Appointment not found |
| 409 | Conflict | Slot not available, invalid status transition, already clocked in |
| 422 | Unprocessable Entity | Business rule violation (outside business hours, holiday, etc.) |
| 500 | Internal Server Error | Server-side error |

### Error Response Format

```json
{
  "timestamp": "2025-11-12T10:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "No available service bays for the requested time slot",
  "path": "/api/appointments"
}
```

## Frequently Asked Questions (Q&A)

### General Questions

**Q1: What is a service bay?**

A: A service bay is a physical workspace in the shop where vehicle work is performed. The system tracks bay availability to prevent overbooking and ensure efficient scheduling.

**Q2: Can a customer book an appointment outside business hours?**

A: No, the system validates requested times against configured business hours and rejects bookings outside these times.

**Q3: What happens if I try to book on a holiday?**

A: The availability check returns `isHoliday: true` and shows no available slots. The booking request will be rejected.

**Q4: How is the confirmation number generated?**

A: Format: `APT-YYYY-NNNNNN` where YYYY is the year and NNNNNN is a sequential number (e.g., APT-2025-001234).

**Q5: Can I book multiple appointments for the same vehicle?**

A: Yes, there's no restriction on multiple concurrent appointments per vehicle.

### Booking & Availability

**Q6: How does availability checking work?**

A: The system:
1. Checks business hours for the requested date
2. Verifies it's not a holiday
3. Retrieves all existing appointments for that day
4. Calculates available time slots based on bay capacity
5. Returns 30-minute interval slots with availability status

**Q7: What if no bays are available?**

A: The booking request fails with a 409 Conflict error. Customers should choose a different time slot.

**Q8: Can I change my appointment time after booking?**

A: Yes, customers can update appointments with status `PENDING` or `CONFIRMED` using the update endpoint.

**Q9: How far in advance can I book an appointment?**

A: There's no maximum advance booking limit. Minimum lead time is 30 minutes (configurable).

**Q10: What's the slot interval for availability?**

A: 30 minutes. Appointments can start at :00 or :30 of any hour within business hours.

### Employee Operations

**Q11: How do employees know which appointments are assigned to them?**

A: Employees use the "List Appointments" endpoint, which automatically filters to show only their assigned appointments. The "Get Schedule" endpoint provides a daily view.

**Q12: Can multiple employees be assigned to one appointment?**

A: Yes, the `assignedEmployeeIds` field supports multiple employee IDs for team-based work.

**Q13: What happens when an employee accepts vehicle arrival?**

A: The appointment status changes from `CONFIRMED` to `CHECKED_IN`, and the system records:
- `vehicleArrivedAt`: Current timestamp
- `vehicleAcceptedByEmployeeId`: ID of employee who accepted

**Q14: Can employees cancel appointments?**

A: Yes, employees and admins can cancel any appointment. Customers can only cancel their own appointments.

**Q15: What's the difference between "Complete Work" and "Confirm Completion"?**

A: "Complete Work" (employee) marks the work as done. "Confirm Completion" (customer) is the final confirmation that the customer received the vehicle and is satisfied.

### Time Tracking

**Q16: How does time tracking work?**

A: Employees clock in when starting work and clock out when finishing. The system:
- Creates a `TimeSession` record
- Calculates duration on clock-out
- Sends time log to Time Logging Service for payroll

**Q17: Can I clock in/out multiple times for the same appointment?**

A: Yes, multiple sessions are supported (e.g., breaks). Each session is tracked separately.

**Q18: What if I forget to clock out?**

A: You must clock out manually. There's no auto-clock-out. Check the "Get Active Time Session" endpoint to see if you have an active session.

**Q19: How is duration calculated?**

A: Duration (in minutes) = Clock Out Time - Clock In Time. Automatically calculated when clocking out.

**Q20: What happens to time logs after clock-out?**

A: The system automatically sends the time log to the Time Logging Service, which handles payroll integration and reporting.

### Status & Workflow

**Q21: Can I skip statuses in the workflow?**

A: No, the system enforces a sequential status flow. Invalid transitions are rejected with an error.

**Q22: What's a "NO_SHOW" status?**

A: Set by employees/admins when a customer doesn't arrive for their confirmed appointment. This is a terminal status.

**Q23: Can I reopen a completed appointment?**

A: No, `COMPLETED` and `CUSTOMER_CONFIRMED` are terminal states. Create a new appointment for additional work.

**Q24: What happens when an appointment is cancelled?**

A: The service bay is freed, notifications are sent, and the appointment moves to terminal `CANCELLED` status.

**Q25: How long do appointment records stay in the system?**

A: Indefinitely for historical tracking. Implement archival policies as needed for your business requirements.

---

## Summary

The **TechTorque Appointment Service** is a production-ready scheduling platform that provides:

### Core Features
- **Smart Booking**: Real-time availability checking with bay allocation
- **Employee Scheduling**: Daily schedules, assignments, and workload management
- **Time Tracking**: Clock in/out with automatic duration calculation and payroll integration
- **Status Management**: Comprehensive lifecycle tracking from booking to customer confirmation
- **Calendar Views**: Monthly overview with statistics and appointment distribution

### Key Capabilities
- Multi-bay scheduling with capacity management
- Business hours and holiday configuration
- Public availability API for integration with booking widgets
- Employee assignment and workload balancing
- Automatic notifications via integration
- Time log synchronization for payroll

### Technical Highlights
- **Framework**: Spring Boot 3.5.6 with PostgreSQL
- **Security**: JWT authentication with role-based authorization
- **Status Validation**: Enforced state transition rules
- **Integration**: REST APIs for time logging and notifications
- **API Documentation**: OpenAPI 3.0 (Swagger) for interactive testing

### Use Cases
- Vehicle service shops with multiple service bays
- Multi-employee work scheduling and tracking
- Customer self-service appointment booking
- Employee time tracking for payroll
- Capacity planning and workload management

**Version**: 0.0.1-SNAPSHOT
**Last Updated**: November 2025
**Maintainer**: TechTorque Development Team

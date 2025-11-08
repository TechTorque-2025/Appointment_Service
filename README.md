# 🗓️ Appointment & Scheduling Service

## 🚦 Build Status

**main**

[![Build and Test Appointment Service](https://github.com/TechTorque-2025/Appointment_Service/actions/workflows/buildtest.yaml/badge.svg)](https://github.com/TechTorque-2025/Appointment_Service/actions/workflows/buildtest.yaml)

**dev**

[![Build and Test Appointment Service](https://github.com/TechTorque-2025/Appointment_Service/actions/workflows/buildtest.yaml/badge.svg?branch=dev)](https://github.com/TechTorque-2025/Appointment_Service/actions/workflows/buildtest.yaml)

This microservice handles all aspects of appointment booking and work scheduling for the TechTorque vehicle service management system.

**Assigned Team:** Aditha, Chamodi

## 🎯 Key Features

### Core Functionality
- ✅ **Appointment Booking**: Customers can book service appointments for their vehicles
- ✅ **Appointment Management**: Full CRUD operations with status tracking (PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW)
- ✅ **Availability Checking**: Public endpoint to check available time slots with bay assignments
- ✅ **Employee Scheduling**: View daily work schedules for employees
- ✅ **Calendar View**: Monthly calendar with appointment statistics
- ✅ **Query Filters**: Filter appointments by status, vehicle, date range

### Business Logic
- ✅ **Service Types**: 10 predefined service types (Oil Change, Brake Service, Full Service, etc.)
- ✅ **Service Bays**: 4 service bays with capacity management
- ✅ **Business Hours**: Configurable operating hours with break times
- ✅ **Holiday Management**: Prevents bookings on holidays
- ✅ **Confirmation Numbers**: Auto-generated unique confirmation codes (APT-2025-001234)
- ✅ **Smart Bay Assignment**: Automatic bay allocation based on availability
- ✅ **Slot Validation**: Validates booking times against business hours and breaks

### Data Seeding
- ✅ **Service Types**: Oil Change, Brake Service, Tire Rotation, Wheel Alignment, Engine Diagnostic, Battery Replacement, AC Service, Full Service, Paint Protection, Custom Exhaust
- ✅ **Service Bays**: 4 bays (Quick Service, General Repair, Diagnostic, Modification)
- ✅ **Business Hours**: Mon-Fri 8AM-6PM (12-1PM break), Sat 9AM-3PM, Sun closed
- ✅ **Holidays**: New Year, Independence Day, May Day, Christmas
- ✅ **Sample Appointments**: 8 appointments with various statuses for testing

## ⚙️ Tech Stack

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

- **Framework:** Java 17 / Spring Boot 3.5.6
- **Database:** PostgreSQL
- **Security:** Spring Security (JWT validation)
- **Documentation:** OpenAPI 3.0 (Swagger)

## 📡 API Endpoints

### Appointment Management

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/appointments` | CUSTOMER | Book a new appointment |
| GET | `/appointments` | ANY | List appointments with filters |
| GET | `/appointments/{id}` | ANY | Get appointment details |
| PUT | `/appointments/{id}` | CUSTOMER | Update appointment |
| DELETE | `/appointments/{id}` | CUSTOMER | Cancel appointment |
| PATCH | `/appointments/{id}/status` | EMPLOYEE | Update appointment status |

### Scheduling & Availability

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/appointments/availability` | PUBLIC | Check available time slots |
| GET | `/appointments/schedule` | EMPLOYEE | Get employee daily schedule |
| GET | `/appointments/calendar` | EMPLOYEE/ADMIN | Get monthly calendar view |

### Service Type Management (Admin)

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/service-types` | EMPLOYEE/ADMIN | List all service types |
| GET | `/service-types/{id}` | EMPLOYEE/ADMIN | Get service type details |
| POST | `/service-types` | ADMIN | Create service type |
| PUT | `/service-types/{id}` | ADMIN | Update service type |
| DELETE | `/service-types/{id}` | ADMIN | Deactivate service type |
| GET | `/service-types/category/{category}` | EMPLOYEE/ADMIN | Get by category |

## 📊 Database Schema

### Core Tables
- **appointments**: Main appointment records with status tracking
- **service_types**: Available service offerings with pricing
- **service_bays**: Physical service bays for scheduling
- **business_hours**: Operating hours configuration
- **holidays**: Holiday dates to block bookings

### Key Fields
- **Appointment**: confirmation number, customer ID, vehicle ID, employee ID, bay ID, service type, requested time, status, special instructions
- **ServiceType**: name, category, base price (LKR), estimated duration, description, active status
- **ServiceBay**: bay number, name, description, capacity, active status

## ℹ️ API Information

- **Local Port:** `8083`
- **Swagger UI:** [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui.html)
- **API Docs:** [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs)

## 🚀 Running Locally

### With Docker Compose (Recommended)

This service is designed to be run as part of the main `docker-compose` setup from the project's root directory.

```bash
# From the root of the TechTorque-2025 project
docker-compose up --build appointment-service
```

### Standalone

```bash
# Navigate to the service directory
cd Appointment_Service/appointment-service

# Set environment variables
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=techtorque_appointments
export DB_USER=techtorque
export DB_PASS=techtorque123
export SPRING_PROFILE=dev

# Run with Maven
./mvnw spring-boot:run
```

## 🧪 Testing

### Sample API Calls

#### Book an Appointment
```bash
curl -X POST http://localhost:8083/appointments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-User-Subject: customer-uuid" \
  -d '{
    "vehicleId": "VEH-001",
    "serviceType": "Oil Change",
    "requestedDateTime": "2025-11-10T10:00:00",
    "specialInstructions": "Please check tire pressure"
  }'
```

#### Check Availability
```bash
curl -X GET "http://localhost:8083/appointments/availability?date=2025-11-10&serviceType=Oil%20Change&duration=30"
```

#### Get Monthly Calendar
```bash
curl -X GET "http://localhost:8083/appointments/calendar?year=2025&month=11" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-User-Roles: ADMIN"
```

## 🔒 Security

- **JWT Authentication**: All endpoints except `/appointments/availability` require valid JWT
- **Role-Based Access Control**: CUSTOMER, EMPLOYEE, ADMIN roles with specific permissions
- **Data Isolation**: Customers can only access their own appointments
- **Status Transition Validation**: Enforces valid appointment status flows

## 📝 Implementation Status

### ✅ Completed (100%)

All features from the API design document have been fully implemented:

1. ✅ **Core Entities**: Appointment, ServiceType, ServiceBay, BusinessHours, Holiday
2. ✅ **Business Logic**: Complete validation, bay assignment, confirmation generation
3. ✅ **Data Seeder**: Comprehensive seed data with cross-service references
4. ✅ **API Endpoints**: All 9 appointment endpoints + 6 service type endpoints
5. ✅ **Query Filters**: Filter by status, vehicle, date range
6. ✅ **Calendar View**: Monthly view with statistics
7. ✅ **Availability Checking**: Smart slot generation with bay information

### 📈 Improvements from Audit Report

The service has been enhanced from 0% implementation to 100% with the following additions:

- **Service Types**: Added entity, repository, service, and controller for CRUD operations
- **Service Bays**: Bay management with capacity and availability tracking
- **Business Hours**: Configurable hours with break times and holiday support
- **Confirmation Numbers**: Auto-generated unique identifiers
- **Enhanced Availability**: Bay-aware slot generation
- **Query Filters**: Flexible appointment filtering
- **Calendar Endpoint**: Monthly view with comprehensive statistics
- **Data Seeder**: Production-ready seed data with shared constants

## 🔗 Integration Points

### Dependencies on Other Services
- **Authentication Service** (Port 8081): User authentication and authorization
- **Vehicle Service** (Port 8082): Vehicle ownership validation (future enhancement)

### Shared Constants
Located in `DataSeeder.java`:
- Customer IDs: `00000000-0000-0000-0000-000000000101`, `00000000-0000-0000-0000-000000000102`
- Employee IDs: `00000000-0000-0000-0000-000000000003-005`
- Vehicle IDs: `VEH-001` to `VEH-004`

## 📚 Documentation

- **API Design**: See `/complete-api-design.md` in project root
- **Audit Report**: See `/PROJECT_AUDIT_REPORT_2025.md` in project root
- **Swagger UI**: Available at runtime for interactive API testing

## 🎯 Future Enhancements

- [ ] Vehicle ownership validation via inter-service communication
- [ ] Email notifications for appointment confirmations
- [ ] SMS reminders for upcoming appointments
- [ ] Recurring appointments
- [ ] Employee workload balancing
- [ ] Advanced analytics and reporting
- [ ] Integration with Service Management for workflow automation

## 👥 Development Team

- **Aditha**: Backend development, entity design, business logic
- **Chamodi**: API implementation, data seeding, testing

## 📄 License

Part of the TechTorque-2025 project.

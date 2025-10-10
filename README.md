# 🗓️ Appointment & Scheduling Service

This microservice handles all aspects of appointment booking and work scheduling.

**Assigned Team:** Aditha, Chamodi

### 🎯 Key Responsibilities

-   Allow customers to book new appointments for their vehicles.
-   Manage the status of appointments (e.g., `PENDING`, `CONFIRMED`, `COMPLETED`).
-   Provide a public endpoint to check for available time slots.
-   Allow employees to view their daily work schedules.

### ⚙️ Tech Stack

-   **Framework:** Java / Spring Boot
-   **Database:** PostgreSQL
-   **Security:** Spring Security (consumes JWTs)

### ℹ️ API Information

-   **Local Port:** `8083`
-   **Swagger UI:** [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)

### 🚀 Running Locally

This service is designed to be run as part of the main `docker-compose` setup from the project's root directory.

```bash
# From the root of the TechTorque-2025 project
docker-compose up --build appointment-service
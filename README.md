# 🗓️ Appointment & Scheduling Service

## 🚦 Build Status

**main**

[![Build and Test Appointment Service](https://github.com/TechTorque-2025/Appointment_Service/actions/workflows/buildtest.yaml/badge.svg)](https://github.com/TechTorque-2025/Appointment_Service/actions/workflows/buildtest.yaml)

**dev**

[![Build and Test Appointment Service](https://github.com/TechTorque-2025/Appointment_Service/actions/workflows/buildtest.yaml/badge.svg?branch=dev)](https://github.com/TechTorque-2025/Appointment_Service/actions/workflows/buildtest.yaml)

This microservice handles all aspects of appointment booking and work scheduling.

**Assigned Team:** Aditha, Chamodi

### 🎯 Key Responsibilities

- Allow customers to book new appointments for their vehicles.
- Manage the status of appointments (e.g., `PENDING`, `CONFIRMED`, `COMPLETED`).
- Provide a public endpoint to check for available time slots.
- Allow employees to view their daily work schedules.

### ⚙️ Tech Stack

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

- **Framework:** Java / Spring Boot
- **Database:** PostgreSQL
- **Security:** Spring Security (consumes JWTs)

### ℹ️ API Information

- **Local Port:** `8083`
- **Swagger UI:** [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)

### 🚀 Running Locally

This service is designed to be run as part of the main `docker-compose` setup from the project's root directory.

```bash
# From the root of the TechTorque-2025 project
docker-compose up --build appointment-service
```

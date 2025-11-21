-- H2-compatible schema for Appointment Service tests
DROP ALL OBJECTS;

-- Service Types table
CREATE TABLE service_types (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description CLOB,
    category VARCHAR(255) NOT NULL,
    estimated_duration_minutes INTEGER NOT NULL,
    base_pricelkr DECIMAL(38,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Service Bays table
CREATE TABLE service_bays (
    id VARCHAR(255) PRIMARY KEY,
    bay_number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description CLOB,
    capacity INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Business Hours table (DayOfWeek as INTEGER: MONDAY=1, TUESDAY=2, etc.)
CREATE TABLE business_hours (
    id VARCHAR(255) PRIMARY KEY,
    day_of_week INTEGER NOT NULL,
    open_time TIME,
    close_time TIME,
    break_start_time TIME,
    break_end_time TIME,
    is_open BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Holidays table
CREATE TABLE holidays (
    id VARCHAR(255) PRIMARY KEY,
    date DATE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Appointments table (without FK constraints for easier testing)
CREATE TABLE appointments (
    id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    vehicle_id VARCHAR(255),
    service_type VARCHAR(255) NOT NULL,
    requested_date_time TIMESTAMP NOT NULL,
    vehicle_arrived_at TIMESTAMP,
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    special_instructions CLOB,
    confirmation_number VARCHAR(255),
    assigned_bay_id VARCHAR(255),
    vehicle_accepted_by_employee_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Time Sessions table (without FK constraints for easier testing)
CREATE TABLE time_sessions (
    id VARCHAR(255) PRIMARY KEY,
    appointment_id VARCHAR(255) NOT NULL,
    employee_id VARCHAR(255) NOT NULL,
    clock_in_time TIMESTAMP NOT NULL,
    clock_out_time TIMESTAMP,
    time_log_id VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Appointment Assigned Employees junction table
CREATE TABLE appointment_assigned_employees (
    appointment_id VARCHAR(255) NOT NULL,
    employee_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (appointment_id, employee_id)
);

-- Indexes for performance
CREATE INDEX idx_appointments_customer_id ON appointments(customer_id);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_appointments_requested_date ON appointments(requested_date_time);
CREATE INDEX idx_service_bays_active ON service_bays(active);
CREATE INDEX idx_service_types_active ON service_types(active);
CREATE INDEX idx_time_sessions_appointment_id ON time_sessions(appointment_id);
CREATE INDEX idx_time_sessions_employee_id ON time_sessions(employee_id);
CREATE INDEX idx_business_hours_day ON business_hours(day_of_week);
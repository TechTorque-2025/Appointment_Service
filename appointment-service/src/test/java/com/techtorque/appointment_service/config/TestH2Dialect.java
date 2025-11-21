package com.techtorque.appointment_service.config;

import org.hibernate.dialect.H2Dialect;

/**
 * Custom H2 dialect for tests that handles PostgreSQL-specific types
 */
public class TestH2Dialect extends H2Dialect {

    public TestH2Dialect() {
        super();
    }

    @Override
    protected void initDefaultProperties() {
        super.initDefaultProperties();
        // Set properties for better PostgreSQL compatibility
        getDefaultProperties().setProperty("hibernate.globally_quoted_identifiers", "false");
    }
}
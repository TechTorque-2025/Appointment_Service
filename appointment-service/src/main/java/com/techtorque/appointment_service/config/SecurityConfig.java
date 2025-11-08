package com.techtorque.appointment_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  // A more comprehensive whitelist for Swagger/OpenAPI, based on the auth-service config.
  private static final String[] SWAGGER_WHITELIST = {
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/swagger-resources/**",
    "/webjars/**",
    "/api-docs/**"
  };

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      // Disable CSRF protection for stateless APIs
      .csrf(csrf -> csrf.disable())

      // Set session management to STATELESS
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
      // Explicitly disable form login and HTTP Basic authentication
      .formLogin(formLogin -> formLogin.disable())
      .httpBasic(httpBasic -> httpBasic.disable())
            
      // Set up authorization rules
      .authorizeHttpRequests(authz -> authz
        // Permit all requests to the Swagger UI and API docs paths
        .requestMatchers(SWAGGER_WHITELIST).permitAll()
        
        // Public endpoint for checking appointment availability
        .requestMatchers("/appointments/availability").permitAll()
                
        // All other requests must be authenticated
        .anyRequest().authenticated()
      )

      // Add our custom filter to read headers from the Gateway
      .addFilterBefore(new GatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
package com.techtorque.appointment_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class WebClientConfig {

  @Value("${admin.service.url:http://localhost:8087}")
  private String adminServiceUrl;

  /**
   * JWT token propagation filter that extracts the token from the current request
   * and adds it to outgoing requests.
   */
  private ExchangeFilterFunction jwtTokenPropagationFilter() {
    return (request, next) -> {
      String token = null;

      // Try to get token from RequestContextHolder (HTTP request context)
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest httpRequest = attributes.getRequest();
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
          token = authHeader.substring(7);
        }
      }

      // If not found in request context, try SecurityContext
      if (token == null) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
          token = jwtAuth.getToken().getTokenValue();
        }
      }

      // If token found, add it to the outgoing request
      if (token != null) {
        final String finalToken = token;
        ClientRequest modifiedRequest = ClientRequest.from(request)
            .header("Authorization", "Bearer " + finalToken)
            .build();
        return next.exchange(modifiedRequest);
      }

      return next.exchange(request);
    };
  }

  @Bean(name = "adminServiceWebClient")
  public WebClient adminServiceWebClient(WebClient.Builder builder) {
    return builder
        .baseUrl(adminServiceUrl)
        .filter(jwtTokenPropagationFilter())
        .build();
  }
}

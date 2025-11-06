package com.techtorque.appointment_service.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GatewayHeaderFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

    String userId = request.getHeader("X-User-Subject");
    String rolesHeader = request.getHeader("X-User-Roles");

    if (userId != null && !userId.isEmpty()) {
      List<SimpleGrantedAuthority> authorities = rolesHeader == null ? Collections.emptyList() :
              Arrays.stream(rolesHeader.split(","))
                      .map(role -> {
                        String roleUpper = role.trim().toUpperCase();
                        // Treat SUPER_ADMIN as ADMIN for authorization purposes
                        if ("SUPER_ADMIN".equals(roleUpper)) {
                          // Add both SUPER_ADMIN and ADMIN roles
                          return Arrays.asList(
                            new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                            new SimpleGrantedAuthority("ROLE_ADMIN")
                          );
                        }
                        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleUpper));
                      })
                      .flatMap(List::stream)
                      .collect(Collectors.toList());

      UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(userId, null, authorities);

      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }
}

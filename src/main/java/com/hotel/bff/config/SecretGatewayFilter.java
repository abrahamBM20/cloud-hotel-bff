package com.hotel.bff.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Protects the BFF entry point with the header injected by API Gateway.
 * Reservation endpoints are excluded because they are protected by Cognito JWT
 * and publish or query the asynchronous Lambda workflow.
 */
@Component
public class SecretGatewayFilter extends OncePerRequestFilter {

    @Value("${app.gateway.secret}")
    private String expectedSecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/api/health".equals(path)
                || isReservationEndpoint(path);
    }

    private boolean isReservationEndpoint(String path) {
        return path.matches("(/api)?/v1/reservations(/.*)?");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String secretHeader = request.getHeader("X-Secret-Gateway");

        if (expectedSecret == null || !expectedSecret.equals(secretHeader)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Acceso denegado: Secreto de pasarela inválido o ausente");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

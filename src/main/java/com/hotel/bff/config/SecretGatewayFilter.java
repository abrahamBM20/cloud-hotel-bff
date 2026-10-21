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
 * Exige la cabecera X-Secret-Gateway que inyecta el API Gateway.
 * Nadie puede saltarse el API Gateway para llamar al BFF directamente.
 *
 * Excepción: /api/health, para que el orquestador pueda consultar el estado
 * del contenedor sin conocer el secreto.
 */
@Component
public class SecretGatewayFilter extends OncePerRequestFilter {

    @Value("${app.gateway.secret}")
    private String expectedSecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/api/health".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String secretHeader = request.getHeader("X-Secret-Gateway");

        // Validar que la cabecera exista y coincida exactamente con el secreto
        if (expectedSecret == null || !expectedSecret.equals(secretHeader)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Acceso denegado: Secreto de pasarela inválido o ausente");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

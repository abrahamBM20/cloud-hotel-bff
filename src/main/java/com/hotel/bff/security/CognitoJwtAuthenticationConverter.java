package com.hotel.bff.security;

import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convierte los grupos de Cognito (claim "cognito:groups") en roles de Spring
 * Security: el grupo ADMIN queda como ROLE_ADMIN, RECEPCIONISTA como ROLE_RECEPCIONISTA, etc.
 * Con eso funcionan los @PreAuthorize("hasRole('ADMIN')").
 */
public class CognitoJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        // Extraemos el email como String para que coincida con el tipo esperado
        String principalName = jwt.getClaimAsString("email");
        if (principalName == null) {
            principalName = jwt.getSubject(); // Fallback al sub si no hay email
        }

        assert principalName != null;
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> groups = jwt.getClaim("cognito:groups");
        if (groups == null) {
            return List.of();
        }
        return groups.stream()
                .map(group -> new SimpleGrantedAuthority("ROLE_" + group))
                .collect(Collectors.toList());
    }
}

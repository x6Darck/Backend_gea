package com.calendario.callapp.callapp_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
/**
 * Provee el {@link AuditorAware} que alimenta los campos {@code usuarioCreacion} y
 * {@code usuarioActualizacion} de {@link com.calendario.callapp.callapp_backend.entity.BaseEntity}.
 *
 * <p>Lee el nombre del usuario autenticado del {@link org.springframework.security.core.context.SecurityContextHolder}.
 * Si no hay sesión activa (seeds de inicio, migraciones) devuelve {@code "SISTEMA"}
 * para que JPA Auditing tenga siempre un valor no nulo.</p>
 */
public class JpaAuditingConfig {

    @Bean
    @SuppressWarnings("null")
    public AuditorAware<String> auditorProvider() {
        return (AuditorAware<String>) () -> {
            SecurityContext context = SecurityContextHolder.getContext();
            Authentication authentication = context.getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.of("SISTEMA");
            }
            
            String name = authentication.getName();
            return Optional.ofNullable(name);
        };
    }
}

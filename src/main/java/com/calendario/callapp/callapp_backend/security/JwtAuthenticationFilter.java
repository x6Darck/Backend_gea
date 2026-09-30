package com.calendario.callapp.callapp_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * Filtro de autenticación JWT que se ejecuta una vez por petición HTTP.
 *
 * <p>Extrae el token de la cookie {@code gea_auth} (web) o del header {@code Authorization: Bearer ...} (móvil).
 * Valida la firma y el JTI con {@link TokenBlacklistService}; si pasa, establece el
 * {@link org.springframework.security.core.context.SecurityContext} con las autoridades del usuario.
 * Si no hay token válido, la cadena de filtros continúa sin autenticación y Spring Security
 * decidirá si el endpoint requiere auth o no.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Value("${app.security.cookie.name:gea_auth}")
    private String cookieName;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        String correo = jwtService.extractUsername(token);
        String rol = jwtService.extractRol(token);

        if (correo != null
                && rol != null
                && jwtService.isTokenValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                var userDetails = userDetailsService.loadUserByUsername(correo);

                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    correo,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                String correoMasked = correo != null && correo.contains("@")
                        ? correo.substring(0, Math.min(2, correo.indexOf('@'))) + "***" + correo.substring(correo.indexOf('@'))
                        : "***";
                log.warn("Fallo de autenticación JWT para '{}': {}", correoMasked, e.getClass().getSimpleName());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // 1. Bearer token (móvil / Swagger / clientes API)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. HttpOnly cookie (clientes web)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}

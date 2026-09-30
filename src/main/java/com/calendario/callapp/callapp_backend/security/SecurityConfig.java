package com.calendario.callapp.callapp_backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
/**
 * Configuración central de Spring Security.
 *
 * <p>Define la cadena de filtros stateless (sin sesión HTTP) con el orden:
 * {@link RateLimiterFilter} → {@link JwtAuthenticationFilter} → {@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}.
 * Configura CORS con los orígenes declarados en {@code app.security.cors.allowed-origins},
 * deshabilita CSRF (API REST stateless) y usa {@code @PreAuthorize} para control de acceso
 * por método. Los errores 401/403 se serializan como JSON mediante los handlers personalizados.</p>
 */
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final RateLimiterFilter rateLimiterFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @org.springframework.beans.factory.annotation.Value("${app.security.cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;"))
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/app/eventos/publicados/**", "/app/anuncios/publicados/**", "/archivos/public/**", "/app/eventos/agenda/export/pdf", "/app/eventos/proximos").permitAll()
                .requestMatchers("/lugares-fisicos/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/admin/usuarios").hasAnyRole("SUPER_ADMIN", "ADMIN", "COMUNICACIONES")
                .requestMatchers(HttpMethod.GET, "/admin/oficinas").hasAnyRole("SUPER_ADMIN", "ADMIN", "COMUNICACIONES", "OFICINA", "USUARIO_AUTENTICADO_APP", "CONSULTORIA")
                .requestMatchers("/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/comunicaciones/archivos/upload").hasAnyRole("SUPER_ADMIN", "ADMIN", "COMUNICACIONES", "OFICINA", "USUARIO_AUTENTICADO_APP")
                .requestMatchers(HttpMethod.GET, "/comunicaciones/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "COMUNICACIONES", "CONSULTORIA")
                .requestMatchers("/comunicaciones/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "COMUNICACIONES")
                .requestMatchers("/oficina/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "COMUNICACIONES", "OFICINA", "CONSULTORIA")
                .requestMatchers("/app/solicitudes-anuncio/**").hasAnyRole("USUARIO_AUTENTICADO_APP", "SUPER_ADMIN", "ADMIN", "COMUNICACIONES", "OFICINA", "USUARIO_APP", "CONSULTORIA")
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/reportes/**").authenticated()
                .requestMatchers("/usuario/**").hasAnyRole(
                        "SUPER_ADMIN", "ADMIN", "COMUNICACIONES", "OFICINA", "USUARIO_APP", "USUARIO_AUTENTICADO_APP", "CONSULTORIA"
                )
                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimiterFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Fail-closed: si no se configuran orígenes permitidos, no se admite ninguno
        // (no usar "*" porque allowCredentials=true lo haría inseguro).
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        } else {
            configuration.setAllowedOriginPatterns(java.util.List.of());
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "Bypass-Tunnel-Reminder"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

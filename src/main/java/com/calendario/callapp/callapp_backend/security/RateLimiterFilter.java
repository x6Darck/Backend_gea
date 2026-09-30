package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.util.ClientIpResolver;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * Filtro de limitación de tasa ({@code rate limiting}) para endpoints de autenticación.
 *
 * <p>Bloquea con 429 las claves que superen su cupo por minuto. La clave es
 * por-usuario cuando la petición trae un JWT válido (evita que el NAT de un
 * campus universitario junte a cientos de usuarios legítimos bajo la misma
 * cuota); si no hay token, cae a la IP, obtenida del header
 * {@code X-Forwarded-For} cuando {@code app.security.rate-limit.trust-proxy = true}
 * (despliegues detrás de un proxy inverso de confianza). Se puede deshabilitar
 * completamente con {@code app.security.rate-limit.enabled = false}.</p>
 *
 * <p>El cupo depende de la ruta ({@link #resolveMaxRequests}): {@code /auth/login}
 * usa {@code app.security.rate-limit.auth-requests-per-minute} (sigue siendo
 * por-IP — no hay sesión antes de loguearse — pero calibrado para tolerar un
 * NAT compartido, no solo un usuario aislado). El resto usa
 * {@code requests-per-minute} × {@code non-auth-multiplier}: la clave por-usuario
 * ya da a cada quien su propia cuota, así que el multiplicador solo necesita
 * cubrir ráfagas de navegación normales, no proteger contra un NAT compartido
 * (eso ya lo resuelve la clave).</p>
 *
 * <p>Este filtro corre ANTES que {@link JwtAuthenticationFilter} en la cadena de seguridad,
 * por lo que no puede leer el {@code SecurityContext}: decodifica el JWT directamente vía
 * {@link JwtService#extractUsername} (best-effort, sin validar expiración/blacklist — para
 * esta clave basta con una identidad estable, no una decisión de autenticación).</p>
 */
public class RateLimiterFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.rate-limit.requests-per-minute:10}")
    private int maxRequests;

    // Multiplicador para todo lo que NO sea /auth/**: tráfico autenticado
    // (clave por-usuario, cada quien su propia cuota) y rutas públicas de
    // solo lectura (permitAll, sin efectos secundarios, seguras de ser
    // generosas). Antes era un x5 fijo sin posibilidad de ajuste operativo.
    @Value("${app.security.rate-limit.non-auth-multiplier:5}")
    private int nonAuthMultiplier;

    // Límite dedicado para /auth/login, independiente del general: sigue
    // siendo por-IP (no hay sesión previa a un login), pero un NAT de campus
    // compartido puede tener a cientos de usuarios legítimos entrando en la
    // misma ventana de un minuto — un límite pensado para un solo usuario
    // (60/min) los bloquea a casi todos. Mantiene protección anti
    // fuerza-bruta real sin penalizar a todo un edificio por igual.
    @Value("${app.security.rate-limit.auth-requests-per-minute:60}")
    private int authRequestsPerMinute;

    @Value("${app.security.cookie.name:gea_auth}")
    private String cookieName;

    private final Map<String, UserRequests> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        // Skip protection for public files and swagger for performance
        if (path.contains("/archivos/public/") || path.contains("/swagger") || path.contains("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String rateLimitKey = resolveRateLimitKey(request);
        long currentTime = System.currentTimeMillis() / 60000; // Minute resolution

        UserRequests userRequests = requestCounts.compute(rateLimitKey, (k, v) -> {
            if (v == null || v.minute != currentTime) {
                return new UserRequests(currentTime, new AtomicInteger(1));
            }
            v.count.incrementAndGet();
            return v;
        });

        int currentMax = resolveMaxRequests(path);

        if (userRequests.count.get() > currentMax) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false, \"message\":\"Has superado el limite de peticiones. Por favor espera un momento.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Prevent Memory Leak: Clear old IP entries every 10 minutes.
     * This ensures the map doesn't grow indefinitely in production.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 600000) 
    public void cleanupOldEntries() {
        long currentTime = System.currentTimeMillis() / 60000;
        int initialSize = requestCounts.size();
        requestCounts.entrySet().removeIf(entry -> entry.getValue().minute < currentTime);
        if (initialSize > 0) {
            log.debug("RateLimiter cleanup: tamaño actual del mapa: {}", requestCounts.size());
        }
    }

    /**
     * Límite aplicable según la ruta: /auth/** usa su propio cupo dedicado
     * (sigue siendo por-IP, pero calibrado para tolerar un NAT compartido sin
     * perder la protección anti fuerza-bruta); el resto usa el cupo base
     * multiplicado. Paquete-visible para test unitario.
     */
    int resolveMaxRequests(String path) {
        boolean isAuthPath = path.startsWith("/auth/") || path.contains("/api/auth/");
        return isAuthPath ? authRequestsPerMinute : maxRequests * nonAuthMultiplier;
    }

    /**
     * Clave de cuota: por-usuario si la petición trae un JWT decodificable, si no por-IP.
     * Paquete-visible para test unitario.
     */
    String resolveRateLimitKey(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            String username = jwtService.extractUsername(token);
            if (username != null) {
                return "user:" + username;
            }
        }
        return "ip:" + clientIpResolver.resolve(request);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private static class UserRequests {
        final long minute;
        final AtomicInteger count;

        UserRequests(long minute, AtomicInteger count) {
            this.minute = minute;
            this.count = count;
        }
    }
}

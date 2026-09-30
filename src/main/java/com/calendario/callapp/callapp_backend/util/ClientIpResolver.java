package com.calendario.callapp.callapp_backend.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resuelve la IP real del cliente de forma endurecida contra spoofing. Única
 * implementación compartida entre el rate-limiter y el historial de login, para
 * que no puedan divergir (la lógica es sensible: de qué IP nos fiamos).
 *
 * <p>Solo confía en {@code X-Forwarded-For} cuando el backend está detrás de un
 * proxy reverso de confianza ({@code app.security.rate-limit.trust-proxy=true},
 * es decir Nginx). Nginx usa {@code $proxy_add_x_forwarded_for}, que SIEMPRE
 * agrega su propio remote_addr al FINAL de la cadena: por eso la posición
 * confiable es la última. Un cliente puede anteponer valores falsos, que Nginx
 * conserva intactos; tomar la primera posición permitiría falsificar la IP.</p>
 */
@Component
public class ClientIpResolver {

    @Value("${app.security.rate-limit.trust-proxy:false}")
    private boolean trustProxy;

    public String resolve(HttpServletRequest request) {
        if (trustProxy) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                String[] partes = xForwardedFor.split(",");
                String candidate = partes[partes.length - 1].trim();
                if (candidate.matches("^[0-9a-fA-F.:]+$") && candidate.length() <= 45) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }
}

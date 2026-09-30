package com.calendario.callapp.callapp_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "app.security")
@Data
/**
 * Propiedades de seguridad mapeadas desde el prefijo {@code app.security} en {@code application.yml}.
 *
 * <p>Incluye configuración de CORS ({@code app.security.cors.allowed-origins}) y
 * rate limiting ({@code app.security.rate-limit.*}). Véase también {@link AppSecurityProperties}
 * que cubre las mismas propiedades con un mapping alternativo.</p>
 */
public class SecurityProperties {

    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Cors {
        private String allowedOrigins;
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerMinute = 10;
    }
}

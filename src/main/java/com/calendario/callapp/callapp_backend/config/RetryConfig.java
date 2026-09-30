package com.calendario.callapp.callapp_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Habilita {@code @Retryable} (Spring Retry) en toda la aplicación.
 *
 * <p>Usado por {@link com.calendario.callapp.callapp_backend.service.impl.NotificacionServiceImpl}
 * para reintentar con backoff el envío de correo ante fallos transitorios de
 * SMTP (conexión, timeout) — Fase 4 del plan de migración.</p>
 */
@Configuration
@EnableRetry
public class RetryConfig {
}

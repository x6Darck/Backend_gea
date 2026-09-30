/**
 * Configuración técnica de la aplicación GEA.
 *
 * <p>Beans de infraestructura y propiedades de configuración. Componentes disponibles:</p>
 * <ul>
 *   <li>{@code AsyncConfig} — pool de hilos {@code notificacionesExecutor} para {@code @Async}.</li>
 *   <li>{@code CacheConfig} — caché Caffeine {@code "userDetails"} (TTL 45 s, máx 500 entradas).</li>
 *   <li>{@code JpaAuditingConfig} — proveedor {@code AuditorAware} para campos de auditoría JPA.</li>
 *   <li>{@code PasswordConfig} — bean {@code BCryptPasswordEncoder}.</li>
 *   <li>{@code SecurityProperties} / {@code AppSecurityProperties} — propiedades {@code app.security.*}.</li>
 *   <li>{@code DataInitializer} — seed de datos maestros al arrancar (desactivable con {@code app.data.seed-demo=false}).</li>
 *   <li>{@code OficinaDataSeeder} — seed idempotente de oficinas mínimas del sistema.</li>
 * </ul>
 */
package com.calendario.callapp.callapp_backend.config;

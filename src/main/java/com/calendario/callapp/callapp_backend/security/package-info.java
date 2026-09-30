/**
 * Seguridad y autenticación del backend GEA.
 *
 * <p>Cadena de filtros: {@code RateLimiterFilter → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter}.
 * Componentes disponibles:</p>
 * <ul>
 *   <li>{@code SecurityConfig} — configura Spring Security, CORS y la cadena de filtros.</li>
 *   <li>{@code JwtService} — generación y validación de tokens JWT (HMAC-SHA256, JTI incluido).</li>
 *   <li>{@code JwtAuthenticationFilter} — extrae el token de cookie o header y autentica la petición.</li>
 *   <li>{@code TokenBlacklistService} — lista negra JTI en memoria con limpieza programada.</li>
 *   <li>{@code RateLimiterFilter} — limita peticiones a endpoints de auth por IP.</li>
 *   <li>{@code CustomUserDetailsService} — carga y cachea (Caffeine 45 s) usuarios por correo.</li>
 *   <li>{@code JwtAuthenticationEntryPoint} — serializa el error 401 como JSON.</li>
 *   <li>{@code JwtAccessDeniedHandler} — serializa el error 403 como JSON.</li>
 * </ul>
 */
package com.calendario.callapp.callapp_backend.security;

/**
 * Capa HTTP del backend GEA — controladores REST.
 *
 * <p>Cada controlador recibe la petición HTTP, delega la lógica al servicio
 * correspondiente y devuelve la respuesta envuelta en {@code ApiResponse<T>}.
 * Controladores disponibles:</p>
 * <ul>
 *   <li>{@code AuthController} — login local, login Microsoft OAuth2, logout.</li>
 *   <li>{@code UsuarioController} — CRUD de usuarios ({@code /admin/usuarios}).</li>
 *   <li>{@code OficinaController} — CRUD de oficinas ({@code /admin/oficinas}).</li>
 *   <li>{@code LugarFisicoController} — catálogo de espacios físicos ({@code /lugares-fisicos}).</li>
 *   <li>{@code TipoEventoController} — catálogo de tipos de evento ({@code /usuario/tipos-evento}).</li>
 *   <li>{@code SolicitudEventoController} — ciclo de vida completo de eventos.</li>
 *   <li>{@code SolicitudAnuncioController} — ciclo de vida de anuncios.</li>
 *   <li>{@code ArchivoController} — subida y descarga pública de archivos adjuntos.</li>
 *   <li>{@code DispositivoUsuarioController} — tokens FCM ({@code /usuario/dispositivos}).</li>
 *   <li>{@code DashboardController} — métricas de resumen ({@code /dashboard}).</li>
 *   <li>{@code ReporteController} — generación y descarga de reportes ({@code /reportes}).</li>
 *   <li>{@code AuditoriaController} — historial Envers ({@code /admin/auditoria}).</li>
 * </ul>
 */
package com.calendario.callapp.callapp_backend.controller;

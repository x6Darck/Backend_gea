/**
 * Implementaciones concretas de los servicios de negocio del backend GEA.
 *
 * <p>Núcleo funcional de la aplicación. Clases disponibles:</p>
 * <ul>
 *   <li>{@code AuthServiceImpl} — autenticación local con anti-enumeración.</li>
 *   <li>{@code MicrosoftAuthServiceImpl} — login PKCE OAuth2 con auto-provisioning.</li>
 *   <li>{@code UsuarioServiceImpl} — CRUD de usuarios con validación de unicidad.</li>
 *   <li>{@code DispositivoUsuarioServiceImpl} — upsert de tokens FCM.</li>
 *   <li>{@code LugarFisicoServiceImpl} — consulta de espacios físicos.</li>
 *   <li>{@code OficinaServiceImpl} — CRUD de oficinas.</li>
 *   <li>{@code TipoEventoServiceImpl} — catálogo de tipos de evento con color hex.</li>
 *   <li>{@code ArchivoServiceImpl} — almacenamiento de archivos con token público.</li>
 *   <li>{@code PlantillaCorreoServiceImpl} — renderizado de plantillas HTML.</li>
 *   <li>{@code PushNotificationService} — Firebase FCM con modo simulado sin credenciales.</li>
 *   <li>{@code NotificacionServiceImpl} — email + push asíncronos ({@code @Async}).</li>
 *   <li>{@code DashboardServiceImpl} — métricas con alcance global o por oficina según rol.</li>
 *   <li>{@code AuditoriaServiceImpl} — historial de revisiones Hibernate Envers.</li>
 *   <li>{@code SolicitudEventoServiceImpl} — ciclo de vida completo de eventos con recurrencia.</li>
 *   <li>{@code SolicitudAnuncioServiceImpl} — ciclo de vida de anuncios.</li>
 *   <li>{@code ReporteServiceImpl} — generación de reportes PDF/Excel (OpenPDF + Apache POI).</li>
 *   <li>{@code AgendaPdfService} — agenda en PDF con paleta de marca GEA.</li>
 * </ul>
 */
package com.calendario.callapp.callapp_backend.service.impl;

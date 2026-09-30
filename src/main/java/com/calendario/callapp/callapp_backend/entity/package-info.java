/**
 * Modelo persistente del dominio GEA.
 *
 * <p>Contiene todas las entidades JPA que mapean tablas de PostgreSQL, los enums
 * de dominio y la clase base de auditoría. Resumen del modelo:</p>
 *
 * <ul>
 *   <li><b>Identidad:</b> {@code Usuario}, {@code RolEntity}, {@code Rol}, {@code AuthProvider}.</li>
 *   <li><b>Organización:</b> {@code Oficina}, {@code LugarFisico}, {@code TipoEventoCatalogo}.</li>
 *   <li><b>Solicitudes y publicaciones de eventos:</b> {@code SolicitudEvento},
 *       {@code SolicitudEventoParticipante}, {@code PublicacionEvento},
 *       {@code FrecuenciaRecurrencia}, {@code TipoIngreso}, {@code TipoParticipante}.</li>
 *   <li><b>Solicitudes y publicaciones de anuncios:</b> {@code SolicitudAnuncio},
 *       {@code PublicacionAnuncio}, {@code EstadoSolicitud}.</li>
 *   <li><b>Soporte:</b> {@code ArchivoAdjunto}, {@code ReporteGenerado},
 *       {@code DispositivoUsuario}, {@code NotificacionEnviada}.</li>
 *   <li><b>Auditoría Envers:</b> {@code AuditRevisionEntity} (tabla {@code audit_revision_info}).</li>
 *   <li><b>Base de auditoría JPA:</b> {@code BaseEntity} (campos creacion/modificacion).</li>
 * </ul>
 */
package com.calendario.callapp.callapp_backend.entity;

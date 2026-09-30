package com.calendario.callapp.callapp_backend.entity;

/**
 * Estados del ciclo de vida de una solicitud (evento o anuncio).
 *
 * <ul>
 *   <li>{@code PENDIENTE} — recién creada, esperando revisión de Comunicaciones.</li>
 *   <li>{@code APROBADA} — aprobada por Comunicaciones; lista para ser publicada.</li>
 *   <li>{@code RECHAZADA} — rechazada definitivamente con motivo.</li>
 *   <li>{@code PUBLICADA} — publicada y visible al público.</li>
 *   <li>{@code EN_REVISION} — devuelta al solicitante para correcciones antes de re-enviar.</li>
 * </ul>
 */
public enum EstadoSolicitud {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    PUBLICADA,
    EN_REVISION
}

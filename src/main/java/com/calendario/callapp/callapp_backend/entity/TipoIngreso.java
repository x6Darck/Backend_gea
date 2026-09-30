package com.calendario.callapp.callapp_backend.entity;

/**
 * Modalidad de acceso a un evento.
 *
 * <ul>
 *   <li>{@code LIBRE} — entrada libre y gratuita, abierta al público.</li>
 *   <li>{@code PAGO} — requiere pagar una inscripción o boleta.</li>
 *   <li>{@code PRIVADO} — acceso restringido a invitados específicos.</li>
 * </ul>
 */
public enum TipoIngreso {
    LIBRE,
    PAGO,
    PRIVADO
}

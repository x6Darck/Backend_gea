package com.calendario.callapp.callapp_backend.entity;

/**
 * Frecuencia con la que se repite un evento recurrente.
 *
 * <ul>
 *   <li>{@code NINGUNA} — evento puntual, sin recurrencia.</li>
 *   <li>{@code DIARIA} — se repite cada día hasta {@code fechaFinRecurrencia}.</li>
 *   <li>{@code SEMANAL} — se repite cada semana en el mismo día de la semana.</li>
 *   <li>{@code MENSUAL} — se repite el mismo día del mes.</li>
 * </ul>
 *
 * <p>La lógica de generación de fechas vive en
 * {@code SolicitudEventoServiceImpl.generarFechasRecurrencia()}.</p>
 */
public enum FrecuenciaRecurrencia {
    NINGUNA,
    DIARIA,
    SEMANAL,
    MENSUAL
}

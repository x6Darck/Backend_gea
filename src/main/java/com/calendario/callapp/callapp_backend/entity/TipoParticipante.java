package com.calendario.callapp.callapp_backend.entity;

/**
 * Rol que desempeña una persona dentro de un evento.
 *
 * <ul>
 *   <li>{@code ORGANIZADOR} — responsable principal de la realización del evento.</li>
 *   <li>{@code INVITADO} — ponente, conferencista o participante especial.</li>
 *   <li>{@code PATROCINADOR_ALIADO} — entidad o persona que apoya o patrocina el evento.</li>
 * </ul>
 *
 * <p>Usado por {@link SolicitudEventoParticipante}.</p>
 */
public enum TipoParticipante {
    ORGANIZADOR,
    INVITADO,
    PATROCINADOR_ALIADO
}

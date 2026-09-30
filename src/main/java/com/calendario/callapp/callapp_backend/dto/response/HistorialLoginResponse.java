package com.calendario.callapp.callapp_backend.dto.response;

import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Vista de lectura de un registro de {@code historial_login} para el panel. */
@Data
@Builder
public class HistorialLoginResponse {

    private Long id;
    private String correoIntentado;
    private Long idUsuario;
    private Boolean exito;
    private String metodo;
    private String motivoFallo;
    private String ip;
    private String userAgent;
    private LocalDateTime fecha;

    public static HistorialLoginResponse desde(HistorialLogin h) {
        return HistorialLoginResponse.builder()
                .id(h.getId())
                .correoIntentado(h.getCorreoIntentado())
                .idUsuario(h.getIdUsuario())
                .exito(h.getExito())
                .metodo(h.getMetodo() != null ? h.getMetodo().name() : null)
                .motivoFallo(h.getMotivoFallo() != null ? h.getMotivoFallo().name() : null)
                .ip(h.getIp())
                .userAgent(h.getUserAgent())
                .fecha(h.getFecha())
                .build();
    }
}

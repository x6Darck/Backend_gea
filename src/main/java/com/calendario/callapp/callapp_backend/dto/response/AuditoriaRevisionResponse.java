package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
/** Representa una revisión de Hibernate Envers: número de revisión, fecha, tipo de cambio (ADD/MOD/DEL) y resumen de estado. */
public class AuditoriaRevisionResponse {

    private Number revision;
    private LocalDateTime fechaRevision;
    private String tipoCambio;
    private String resumen;
}

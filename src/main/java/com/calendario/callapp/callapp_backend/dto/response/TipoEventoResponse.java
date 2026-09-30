package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/** Proyección de un tipo de evento del catálogo. El {@code colorHex} se usa para colorear eventos en el calendario. */
public class TipoEventoResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String colorHex;
    private Boolean activo;
}

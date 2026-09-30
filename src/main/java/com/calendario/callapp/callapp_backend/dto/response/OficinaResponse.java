package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/** Proyección de una oficina (unidad académica/administrativa) para el cliente. */
public class OficinaResponse {

    private Long id;
    private String nombre;
    private String programaAcademico;
    private String descripcion;
    private Boolean activa;
}

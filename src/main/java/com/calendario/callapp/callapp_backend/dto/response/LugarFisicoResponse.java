package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/** Proyección de un espacio físico para eventos. {@code esExterno} indica si está fuera del campus. */
public class LugarFisicoResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Integer capacidad;
    private Boolean activo;
    private Boolean esExterno;
}

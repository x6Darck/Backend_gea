package com.calendario.callapp.callapp_backend.dto.request;

import lombok.Data;

@Data
/** Permite actualizar el nombre y/o descripción de un reporte ya generado. */
public class ActualizarReporteRequest {
    private String nombre;
    private String descripcion;
}

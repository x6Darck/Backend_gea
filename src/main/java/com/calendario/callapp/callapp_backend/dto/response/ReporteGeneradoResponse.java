package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
/** Metadatos de un reporte persistido: nombre, formato, rango de fechas, alcance y quién lo generó. */
public class ReporteGeneradoResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String formato;
    private LocalDate desde;
    private LocalDate hasta;
    private String alcance;
    private LocalDateTime fechaCreacion;
    private Long usuarioGeneradorId;
    private String usuarioGeneradorCorreo;
    private String usuarioGeneradorOficina;
    private Long idOficina;
    private Long idTipoEvento;
}

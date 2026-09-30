package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
/**
 * Parámetros para generar un reporte estadístico.
 *
 * <p>{@code formato} puede ser {@code PDF} o {@code EXCEL}.
 * {@code alcance} determina si el reporte es global o filtrado por oficina.
 * Los filtros opcionales {@code idOficina} e {@code idTipoEvento} acotan los datos incluidos.</p>
 */
public class GenerarReporteRequest {

    @NotBlank
    private String nombre;

    private String descripcion;

    @NotBlank
    private String formato;

    @NotBlank
    private String alcance;

    @NotNull
    private LocalDate desde;

    @NotNull
    private LocalDate hasta;

    private Long idOficina;

    private Long idTipoEvento;
}

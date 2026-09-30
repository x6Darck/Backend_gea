package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
/**
 * Payload para crear o editar una solicitud de anuncio.
 *
 * <p>Incluye título, categoría, fechas de vigencia ({@code fechaInicioPublicacion}/{@code fechaFinPublicacion})
 * y opcionalmente una pieza gráfica. Sin campos de recurrencia ni control de espacio físico.</p>
 */
public class SolicitudAnuncioRequest {

    @NotBlank
    private String titulo;

    private String descripcion;

    private String categoria;

    private java.util.List<Long> idsLugaresFisicos;

    private String correoContacto;

    private String responsableAnuncio;

    private LocalDate fechaInicioPublicacion;

    private LocalDate fechaFinPublicacion;

    private LocalTime horaInicio;

    private LocalTime horaFin;

    private String piezaGraficaUrl;

    private Boolean requierePiezaGrafica;
}

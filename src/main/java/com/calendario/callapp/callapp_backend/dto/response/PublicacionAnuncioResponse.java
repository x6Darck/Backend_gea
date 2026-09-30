package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
/**
 * Proyección de un anuncio publicado. Combina datos de {@code PublicacionAnuncio} y su {@code SolicitudAnuncio}
 * subyacente, incluyendo fechas de vigencia y pieza gráfica.
 */
public class PublicacionAnuncioResponse {

    private Long id;
    private Long solicitudAnuncioId;
    private String tituloVisible;
    private String descripcionVisible;
    private String categoria;
    private String lugar;
    private java.util.List<String> lugares;
    private java.util.List<Long> idsLugaresFisicos;
    private String correoContacto;
    private String responsableAnuncio;
    private LocalDate fechaInicioPublicacion;
    private LocalDate fechaFinPublicacion;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String piezaGraficaUrl;
    private String oficinaNombre;
    private LocalDateTime fechaPublicacion;
    private Boolean visible;
    private Boolean requierePiezaGrafica;
}

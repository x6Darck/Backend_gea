package com.calendario.callapp.callapp_backend.dto.response;

import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
/**
 * Proyección completa de una solicitud de anuncio para el cliente.
 * Incluye estado actual, datos del solicitante, oficina y fechas de vigencia de publicación.
 */
public class SolicitudAnuncioResponse {

    private Long id;
    private String titulo;
    private String descripcion;
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
    private EstadoSolicitud estado;
    private String piezaGraficaUrl;
    private String motivoRechazo;
    private String observacionesRevision;
    private Boolean visible;
    private Boolean requierePiezaGrafica;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String usuarioCreacion;
    private String usuarioActualizacion;
    private Long usuarioSolicitanteId;
    private String usuarioSolicitanteCorreo;
    private String usuarioSolicitanteNombre;
    private Long oficinaId;
    private String oficinaNombre;
}

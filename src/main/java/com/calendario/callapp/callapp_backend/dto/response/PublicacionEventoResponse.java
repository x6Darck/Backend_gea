package com.calendario.callapp.callapp_backend.dto.response;

import com.calendario.callapp.callapp_backend.entity.TipoIngreso;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
/**
 * Proyección de un evento publicado visible en la app. Combina datos de {@code PublicacionEvento} y
 * su {@code SolicitudEvento} subyacente, incluyendo los campos de recurrencia.
 */
public class PublicacionEventoResponse {

    private Long id;
    private Long solicitudEventoId;
    private String tituloVisible;
    private String descripcionVisible;
    private String piezaGraficaUrl;
    private LocalDateTime fechaPublicacion;
    private LocalDate fechaEvento;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String lugar;
    private java.util.List<String> lugares;
    private java.util.List<Long> idsLugaresFisicos;
    private String linkConexion;
    private String ubicacionExterna;
    private Long tipoEventoId;
    private String tipoEvento;
    private String tipoEventoColorHex;
    private String oficinaNombre;
    private String responsableEvento;
    private String usuarioSolicitanteCorreo;
    private Boolean requiereTransmision;
    private Boolean requiereCubrimiento;
    private String observaciones;
    private Boolean esImportante;
    private TipoIngreso tipoIngreso;
    private Boolean requierePiezaGrafica;
    private Boolean visible;
}

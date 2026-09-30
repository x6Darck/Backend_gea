package com.calendario.callapp.callapp_backend.dto.response;

import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.FrecuenciaRecurrencia;
import com.calendario.callapp.callapp_backend.entity.TipoIngreso;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
/**
 * Proyección completa de una solicitud de evento para el cliente.
 *
 * <p>Incluye datos de la solicitud base, el estado actual, los campos de recurrencia
 * ({@code idGrupoRecurrencia}, {@code frecuenciaRecurrencia}) y los participantes asociados.
 * Los lugares físicos se exponen tanto como string concatenado ({@code lugar}) como lista ({@code lugares}).</p>
 */
public class SolicitudEventoResponse {

    private Long id;
    private String nombreEvento;
    private String descripcionEvento;
    private LocalDate fechaEvento;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String lugar;
    private java.util.List<String> lugares;
    private java.util.List<Long> idsLugaresFisicos;
    private String linkConexion;
    private String ubicacionExterna;
    private String responsableEvento;
    private Long tipoEventoId;
    private String tipoEvento;
    private String tipoEventoColorHex;
    private EstadoSolicitud estado;
    private String motivoRechazo;
    private String observacionesRevision;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String usuarioCreacion;
    private String usuarioActualizacion;
    private Long oficinaId;
    private String oficinaNombre;
    private Long usuarioSolicitanteId;
    private String usuarioSolicitanteCorreo;
    private String piezaGraficaUrl;
    private Boolean requiereTransmision;
    private Boolean requiereCubrimiento;
    private String observaciones;
    private Boolean esImportante;
    private TipoIngreso tipoIngreso;
    private Boolean requierePiezaGrafica;
    private Boolean requiereServiciosGenerales;
    private FrecuenciaRecurrencia frecuenciaRecurrencia;
    private LocalDate fechaFinRecurrencia;
    private String idGrupoRecurrencia;
    private Boolean esPrincipal;
    private Boolean visible;
    private List<SolicitudEventoParticipanteResponse> participantes;
}

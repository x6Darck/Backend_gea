package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.calendario.callapp.callapp_backend.entity.FrecuenciaRecurrencia;

@Data
/**
 * Payload para crear o editar una solicitud de evento.
 *
 * <p>Incluye los datos base del evento (nombre, fechas, hora, lugar) y los campos de recurrencia
 * ({@code frecuenciaRecurrencia}, {@code fechaFinRecurrencia}). Si {@code frecuenciaRecurrencia}
 * es distinto de {@code NINGUNA} se creará una serie recurrente con UUID compartido.</p>
 */
public class SolicitudEventoRequest {

    @NotBlank
    private String nombreEvento;

    private String descripcionEvento;

    @NotNull
    private LocalDate fechaEvento;

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    private LocalTime horaFin;

    private java.util.List<Long> idsLugaresFisicos;

    private String linkConexion;

    private String ubicacionExterna;

    private String responsableEvento;

    @NotBlank
    private String tipoEvento;

    private String piezaGraficaUrl;

    private Boolean requierePiezaGrafica;

    private Boolean requiereServiciosGenerales;

    private FrecuenciaRecurrencia frecuenciaRecurrencia;

    private LocalDate fechaFinRecurrencia;

    private Boolean requiereTransmision;

    private Boolean requiereCubrimiento;

    private String observaciones;

    private Boolean esImportante;

    private com.calendario.callapp.callapp_backend.entity.TipoIngreso tipoIngreso;

    private Long idOficina;

    @Valid
    private List<SolicitudEventoParticipanteRequest> participantes;
}

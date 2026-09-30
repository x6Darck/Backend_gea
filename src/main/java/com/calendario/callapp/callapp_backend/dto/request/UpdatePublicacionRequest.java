package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
/**
 * Payload multipropósito para editar tanto la publicación como los datos de la solicitud subyacente.
 *
 * <p>Agrupa campos de eventos ({@code fechaEvento}, {@code horaInicio}, {@code horaFin}, participantes)
 * y campos de anuncios ({@code titulo}, {@code fechaInicioPublicacion}) en un mismo DTO para
 * simplificar el endpoint de actualización.</p>
 */
public class UpdatePublicacionRequest {

    @Size(max = 200, message = "El título no puede superar 200 caracteres")
    private String tituloVisible;
    private String descripcionVisible;
    private String piezaGraficaUrl;
    private java.util.List<Long> idsLugaresFisicos;

    // Campos de Solicitud (Evento/Anuncio)
    private String nombreEvento;
    private String descripcionEvento;
    private java.time.LocalDate fechaEvento;
    private java.time.LocalTime horaInicio;
    private java.time.LocalTime horaFin;
    private String lugar;
    private String linkConexion;
    private String ubicacionExterna;
    private String responsableEvento;
    private String tipoEvento;
    private Boolean requiereTransmision;
    private Boolean requiereCubrimiento;
    private String observaciones;
    private Boolean esImportante;
    private com.calendario.callapp.callapp_backend.entity.TipoIngreso tipoIngreso;
    private Boolean requierePiezaGrafica;
    private Boolean requiereServiciosGenerales;

    // Para Anuncios
    private String titulo;
    private String descripcion;
    private String categoria;
    private java.time.LocalDate fechaInicioPublicacion;
    private java.time.LocalDate fechaFinPublicacion;
    private String correoContacto;
    private String responsableAnuncio;
    private Long idOficina;
    private java.util.List<SolicitudEventoParticipanteRequest> participantes;
}

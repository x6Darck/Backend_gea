package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
/** Conteos estadísticos de solicitudes de evento y anuncio en un rango de fechas, desglosados por estado. */
public class ReporteSolicitudesResponse {

    private String alcance;
    private LocalDate desde;
    private LocalDate hasta;
    private long totalSolicitudesEvento;
    private long totalSolicitudesAnuncio;
    private long eventosPendientes;
    private long eventosAprobados;
    private long eventosRechazados;
    private long eventosPublicados;
    private long anunciosPendientes;
    private long anunciosAprobados;
    private long anunciosRechazados;
    private long anunciosPublicados;
    private java.util.List<SolicitudResumenDTO> solicitudes;
}

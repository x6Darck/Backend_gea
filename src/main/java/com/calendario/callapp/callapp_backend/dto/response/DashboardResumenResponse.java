package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
/**
 * Resumen de métricas para el panel de control.
 *
 * <p>{@code alcance} indica si los conteos son globales ({@code "GLOBAL"}) o limitados a una oficina.
 * Incluye conteos por estado de eventos y anuncios, totales de usuarios/oficinas y la lista de próximos eventos.</p>
 */
public class DashboardResumenResponse {

    private String alcance;
    private long totalSolicitudesEvento;
    private long eventosPendientes;
    private long eventosAprobados;
    private long eventosPublicados;
    private long totalSolicitudesAnuncio;
    private long anunciosPendientes;
    private long anunciosAprobados;
    private long anunciosPublicados;
    private long totalUsuarios;
    private long totalOficinas;
    private List<PublicacionEventoResponse> proximosEventos;
}

package com.calendario.callapp.callapp_backend.dto.response;

import com.calendario.callapp.callapp_backend.dto.common.GrupoDatoDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
/**
 * Datos para el dashboard de reportes: KPIs agregados (totales y tasa de aprobación)
 * más series para gráficas ({@code eventosPorTipo}, por oficina, por mes, etc.) usando {@link GrupoDatoDTO}.
 */
public class ReporteDashboardDTO {
    // KPIs
    private long totalSolicitudes;
    private long totalAprobados;
    private long totalPendientes;
    private long totalRechazados;
    private double tasaAprobacion;

    // Reportes Gráficos
    private List<GrupoDatoDTO> eventosPorTipo;
    private List<GrupoDatoDTO> solicitudesPorMes;
    private List<GrupoDatoDTO> solicitudesPorOficina;
    private List<GrupoDatoDTO> tendenciaEstado;
    private List<SolicitudResumenDTO> solicitudes;
}

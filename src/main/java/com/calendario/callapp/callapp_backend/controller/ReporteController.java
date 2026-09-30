package com.calendario.callapp.callapp_backend.controller;


import com.calendario.callapp.callapp_backend.dto.request.ActualizarReporteRequest;
import com.calendario.callapp.callapp_backend.dto.request.GenerarReporteRequest;
import com.calendario.callapp.callapp_backend.dto.response.ReporteDashboardDTO;
import com.calendario.callapp.callapp_backend.dto.response.ReporteGeneradoResponse;
import com.calendario.callapp.callapp_backend.dto.response.ReporteSolicitudesResponse;
import com.calendario.callapp.callapp_backend.service.impl.ReporteServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
/**
 * Generación y descarga de reportes estadísticos (PDF y Excel).
 *
 * <p>Endpoints bajo {@code /reportes}. El rango de exportación está limitado a
 * {@value #MAX_RANGO_DIAS_EXPORT} días para evitar documentos demasiado grandes.
 * Los reportes generados se persisten en BD para auditoría y pueden re-descargarse
 * posteriormente por su {@code id}.</p>
 */
public class ReporteController {

    private static final int MAX_RANGO_DIAS_EXPORT = 366;

    private final ReporteServiceImpl reporteService;

    @PreAuthorize("hasAnyRole('COMUNICACIONES', 'SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP')")
    @PostMapping("/solicitudes")
    public ResponseEntity<ApiResponse<ReporteGeneradoResponse>> crearReporte(
            @Valid @RequestBody GenerarReporteRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(reporteService.crearReporte(request, authentication), "Reporte generado y guardado"));
    }

    @PreAuthorize("hasAnyRole('COMUNICACIONES', 'SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP')")
    @GetMapping("/solicitudes")
    public ResponseEntity<ApiResponse<List<ReporteGeneradoResponse>>> listarReportes(
            @RequestParam(required = false) Long idOficina,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(reporteService.listarReportes(idOficina, desde, hasta, authentication)));
    }

    @PreAuthorize("hasAnyRole('COMUNICACIONES', 'SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP')")
    @GetMapping("/solicitudes/resumen")
    public ResponseEntity<ApiResponse<ReporteSolicitudesResponse>> generarResumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(reporteService.generarResumen(desde, hasta, authentication)));
    }

    @PreAuthorize("hasAnyRole('COMUNICACIONES', 'SUPER_ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ReporteDashboardDTO>> obtenerDashboard(
            @RequestParam(required = false) Long idOficina,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false, defaultValue = "GLOBAL") String tipo,
            Authentication authentication) {

        LocalDate d = desde != null ? desde : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();

        return ResponseEntity.ok(ApiResponse.success(reporteService.obtenerDashboardStats(idOficina, d, h, tipo, authentication)));
    }

    @PreAuthorize("hasAnyRole('COMUNICACIONES', 'SUPER_ADMIN', 'ADMIN')")
    @GetMapping("/solicitudes/export/xlsx")
    public ResponseEntity<byte[]> exportarXlsx(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication) {

        validarRangoFechas(desde, hasta);
        byte[] data = reporteService.exportarXlsx(desde, hasta, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reporte-solicitudes.xlsx").build().toString())
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(data);
    }

    @PreAuthorize("hasAnyRole('COMUNICACIONES', 'SUPER_ADMIN', 'ADMIN')")
    @GetMapping("/solicitudes/export/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication) {

        validarRangoFechas(desde, hasta);
        byte[] data = reporteService.exportarPdf(desde, hasta, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reporte-solicitudes.pdf").build().toString())
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(data);
    }

    @PreAuthorize("hasAnyRole('COMUNICACIONES', 'SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP')")
    @GetMapping("/solicitudes/{id}/export")
    public ResponseEntity<byte[]> exportarReporteGuardado(
            @PathVariable Long id,
            Authentication authentication) {

        ReporteGeneradoResponse reporte = reporteService.listarReportes(authentication).stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "El reporte solicitado no existe o no tiene permisos para acceder a él"));

        byte[] data = reporteService.exportarReporteGenerado(id, authentication);
        String contentType = "PDF".equalsIgnoreCase(reporte.getFormato()) ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String extension = "PDF".equalsIgnoreCase(reporte.getFormato()) ? "pdf" : "xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reporte-" + id + "." + extension).build().toString())
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(data);
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Las fechas 'desde' y 'hasta' son obligatorias");
        }
        if (hasta.isBefore(desde)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "La fecha 'hasta' no puede ser anterior a 'desde'");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta);
        if (dias > MAX_RANGO_DIAS_EXPORT) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "El rango de fechas no puede superar " + MAX_RANGO_DIAS_EXPORT + " días para exportaciones");
        }
    }

    @PreAuthorize("hasAnyRole('COMUNICACIONES', 'SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP')")
    @PutMapping("/solicitudes/{id}")
    public ResponseEntity<ApiResponse<ReporteGeneradoResponse>> actualizarReporte(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody ActualizarReporteRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(reporteService.actualizarReporte(id, request, authentication), "Reporte actualizado exitosamente"));
    }
}

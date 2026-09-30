package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.request.PublicacionEventoRequest;
import com.calendario.callapp.callapp_backend.dto.request.RechazoRequest;
import com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoRequest;
import com.calendario.callapp.callapp_backend.dto.request.UpdatePublicacionRequest;
import com.calendario.callapp.callapp_backend.dto.response.PublicacionEventoResponse;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudEventoResponse;
import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.service.impl.AgendaPdfService;
import com.calendario.callapp.callapp_backend.service.impl.SolicitudEventoServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
/**
 * Gestión completa del ciclo de vida de solicitudes de evento.
 *
 * <p>Expone rutas bajo dos prefijos según el tipo de cliente:</p>
 * <ul>
 *   <li>{@code /oficina/solicitudes-evento} — creación y gestión por oficinas.</li>
 *   <li>{@code /comunicaciones/solicitudes-evento} — revisión, aprobación y publicación por el equipo de comunicaciones.</li>
 *   <li>{@code /app/publicaciones-evento} — consulta pública de eventos publicados.</li>
 *   <li>{@code /agenda/pdf} — descarga de agenda en PDF ({@link AgendaPdfService}).</li>
 * </ul>
 */
public class SolicitudEventoController {

    private final SolicitudEventoServiceImpl solicitudEventoService;
    private final AgendaPdfService agendaPdfService;

    @PostMapping("/oficina/solicitudes-evento")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<SolicitudEventoResponse>> crear(
            @Valid @RequestBody SolicitudEventoRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(solicitudEventoService.crear(request, authentication), "Solicitud creada exitosamente"));
    }

    @GetMapping("/oficina/solicitudes-evento")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<List<SolicitudEventoResponse>>> listarPropias(
            Authentication authentication,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.listarPropias(authentication, q, estado, mes, anio)));
    }

    @GetMapping("/oficina/solicitudes-evento/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<SolicitudEventoResponse>> obtenerPropia(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.obtenerPropia(id, authentication)));
    }

    @PutMapping("/oficina/solicitudes-evento/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<SolicitudEventoResponse>> actualizarPropia(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudEventoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.actualizarPropia(id, request, authentication), "Solicitud actualizada correctamente"));
    }

    @PutMapping("/oficina/solicitudes-evento/serie/{idGrupo}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<SolicitudEventoResponse>> actualizarSeriePropia(
            @PathVariable String idGrupo,
            @Valid @RequestBody SolicitudEventoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.actualizarSerie(idGrupo, request, authentication), "Serie actualizada correctamente"));
    }

    @DeleteMapping("/oficina/solicitudes-evento/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<Void>> eliminarPropia(@PathVariable Long id, Authentication authentication) {
        solicitudEventoService.eliminarPropia(id, authentication);
        return ResponseEntity.ok(ApiResponse.success(null, "Solicitud eliminada correctamente"));
    }

    @GetMapping("/comunicaciones/solicitudes-evento")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN', 'CONSULTORIA')")
    public ResponseEntity<ApiResponse<List<SolicitudEventoResponse>>> listarParaRevision(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.listarParaRevision(q, estado, mes, anio)));
    }

    @GetMapping("/comunicaciones/solicitudes-evento/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN', 'CONSULTORIA')")
    public ResponseEntity<ApiResponse<SolicitudEventoResponse>> obtenerParaRevision(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.obtenerParaRevision(id)));
    }

    @PostMapping("/comunicaciones/solicitudes-evento/{id}/aprobar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<SolicitudEventoResponse>> aprobar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.aprobar(id, authentication), "Solicitud aprobada"));
    }

    @PostMapping("/comunicaciones/solicitudes-evento/{id}/rechazar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<SolicitudEventoResponse>> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody RechazoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.rechazar(id, request, authentication), "Solicitud rechazada"));
    }

    @PostMapping("/comunicaciones/solicitudes-evento/{id}/devolver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<SolicitudEventoResponse>> devolver(
            @PathVariable Long id,
            @Valid @RequestBody com.calendario.callapp.callapp_backend.dto.request.DevolucionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                solicitudEventoService.devolver(id, request, authentication),
                "Solicitud devuelta para revisión"));
    }

    @PostMapping("/comunicaciones/solicitudes-evento/{id}/publicar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<PublicacionEventoResponse>> publicar(
            @PathVariable Long id,
            @Valid @RequestBody PublicacionEventoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.publicar(id, request, authentication), "Evento publicado exitosamente"));
    }

    // --- ACCIONES MASIVAS PARA SERIES RECURRENTES ---

    @PostMapping("/comunicaciones/solicitudes-evento/serie/{idGrupo}/aprobar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> aprobarSerie(@PathVariable String idGrupo, Authentication authentication) {
        solicitudEventoService.aprobarSerie(idGrupo, authentication);
        return ResponseEntity.ok(ApiResponse.success(null, "Serie aprobada correctamente"));
    }

    @PostMapping("/comunicaciones/solicitudes-evento/serie/{idGrupo}/publicar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> publicarSerie(
            @PathVariable String idGrupo,
            @Valid @RequestBody PublicacionEventoRequest request,
            Authentication authentication) {
        solicitudEventoService.publicarSerie(idGrupo, request, authentication);
        return ResponseEntity.ok(ApiResponse.success(null, "Serie publicada correctamente"));
    }

    @DeleteMapping("/comunicaciones/solicitudes-evento/serie/{idGrupo}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminarSerie(@PathVariable String idGrupo, Authentication authentication) {
        solicitudEventoService.eliminarSerie(idGrupo, authentication);
        return ResponseEntity.ok(ApiResponse.success(null, "Serie eliminada correctamente"));
    }

    @DeleteMapping("/oficina/solicitudes-evento/serie/{idGrupo}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<Void>> eliminarSeriePropia(@PathVariable String idGrupo, Authentication authentication) {
        solicitudEventoService.eliminarSerie(idGrupo, authentication);
        return ResponseEntity.ok(ApiResponse.success(null, "Serie eliminada correctamente"));
    }

    @GetMapping("/app/eventos/publicados")
    public ResponseEntity<ApiResponse<List<PublicacionEventoResponse>>> listarPublicados(
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipoEvento,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.listarPublicadas(filtro, fecha, q, tipoEvento, mes, anio)));
    }

    @GetMapping("/app/eventos/publicados/{id}")
    public ResponseEntity<ApiResponse<PublicacionEventoResponse>> obtenerPublicado(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.obtenerPublicacion(id)));
    }

    @GetMapping("/app/eventos/proximos")
    public ResponseEntity<ApiResponse<List<PublicacionEventoResponse>>> listarProximos(
            @RequestParam(defaultValue = "3") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(50) int limit,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.listarProximos(limit, q)));
    }

    @PatchMapping("/comunicaciones/eventos-publicados/{id}/visibilidad")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<PublicacionEventoResponse>> toggleVisibilidad(
            @PathVariable Long id,
            @RequestParam boolean visible) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.toggleVisibilidad(id, visible), "Visibilidad actualizada"));
    }

    @DeleteMapping("/comunicaciones/eventos-publicados/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminarPublicacion(@PathVariable Long id) {
        solicitudEventoService.eliminarPublicacion(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Publicación eliminada correctamente"));
    }

    @PutMapping("/comunicaciones/eventos-publicados/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<PublicacionEventoResponse>> updatePublicacion(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePublicacionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(solicitudEventoService.updatePublicacion(id, request), "Publicación actualizada correctamente"));
    }

    @GetMapping("/app/eventos/agenda/export/pdf")
    public ResponseEntity<byte[]> exportarAgendaPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Rango de fechas inválido");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(desde, hasta) > 366) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango no puede superar 1 año");
        }
        byte[] data = agendaPdfService.exportarAgendaPdf(desde, hasta);
        String filename = "agenda-gea-" + desde + "-al-" + hasta + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(data);
    }
}

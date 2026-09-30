package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.request.PublicacionAnuncioRequest;
import com.calendario.callapp.callapp_backend.dto.request.RechazoRequest;
import com.calendario.callapp.callapp_backend.dto.request.SolicitudAnuncioRequest;
import com.calendario.callapp.callapp_backend.dto.request.UpdatePublicacionRequest;
import com.calendario.callapp.callapp_backend.dto.response.PublicacionAnuncioResponse;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudAnuncioResponse;
import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.service.impl.SolicitudAnuncioServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
/**
 * Gestión del ciclo de vida de solicitudes de anuncio.
 *
 * <p>Rutas bajo dos prefijos:</p>
 * <ul>
 *   <li>{@code /app/solicitudes-anuncio} — creación y consulta de las propias solicitudes.</li>
 *   <li>{@code /comunicaciones/solicitudes-anuncio} — revisión, aprobación y publicación.</li>
 *   <li>{@code /app/publicaciones-anuncio} — anuncios públicos visibles en la app.</li>
 * </ul>
 */
public class SolicitudAnuncioController {

    private final SolicitudAnuncioServiceImpl solicitudAnuncioService;

    @PostMapping("/app/solicitudes-anuncio")
    @PreAuthorize("hasAnyRole('USUARIO_AUTENTICADO_APP', 'OFICINA', 'SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<SolicitudAnuncioResponse>> crear(
            @Valid @RequestBody SolicitudAnuncioRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(solicitudAnuncioService.crear(request, authentication), "Anuncio solicitado exitosamente"));
    }

    @GetMapping("/app/solicitudes-anuncio/mis-solicitudes")
    @PreAuthorize("hasAnyRole('USUARIO_AUTENTICADO_APP', 'OFICINA', 'SUPER_ADMIN', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<List<SolicitudAnuncioResponse>>> listarPropias(
            Authentication authentication,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(ApiResponse.success(solicitudAnuncioService.listarPropias(authentication, q, estado, mes, anio)));
    }

    @GetMapping("/comunicaciones/solicitudes-anuncio")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN', 'CONSULTORIA')")
    public ResponseEntity<ApiResponse<List<SolicitudAnuncioResponse>>> listarParaRevision(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(ApiResponse.success(solicitudAnuncioService.listarParaRevision(q, estado, mes, anio)));
    }

    @GetMapping("/comunicaciones/solicitudes-anuncio/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN', 'CONSULTORIA')")
    public ResponseEntity<ApiResponse<SolicitudAnuncioResponse>> obtenerParaRevision(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(solicitudAnuncioService.obtenerParaRevision(id)));
    }

    @PostMapping("/comunicaciones/solicitudes-anuncio/{id}/aprobar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<SolicitudAnuncioResponse>> aprobar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(solicitudAnuncioService.aprobar(id, authentication), "Anuncio aprobado"));
    }

    @PostMapping("/comunicaciones/solicitudes-anuncio/{id}/rechazar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<SolicitudAnuncioResponse>> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody RechazoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(solicitudAnuncioService.rechazar(id, request, authentication), "Anuncio rechazado"));
    }

    @PostMapping("/comunicaciones/solicitudes-anuncio/{id}/devolver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<SolicitudAnuncioResponse>> devolver(
            @PathVariable Long id,
            @Valid @RequestBody com.calendario.callapp.callapp_backend.dto.request.DevolucionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                solicitudAnuncioService.devolver(id, request, authentication),
                "Anuncio devuelto para revisión"));
    }

    @PostMapping("/comunicaciones/solicitudes-anuncio/{id}/publicar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<PublicacionAnuncioResponse>> publicar(
            @PathVariable Long id,
            @Valid @RequestBody PublicacionAnuncioRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(solicitudAnuncioService.publicar(id, request, authentication), "Anuncio publicado exitosamente"));
    }

    @GetMapping("/app/anuncios/publicados")
    public ResponseEntity<ApiResponse<List<PublicacionAnuncioResponse>>> listarPublicados(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(ApiResponse.success(solicitudAnuncioService.listarPublicados(q, categoria, mes, anio)));
    }

    @GetMapping("/app/anuncios/publicados/{id}")
    public ResponseEntity<ApiResponse<PublicacionAnuncioResponse>> obtenerPublicado(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(solicitudAnuncioService.obtenerPublicacion(id)));
    }

    @DeleteMapping("/app/solicitudes-anuncio/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<Void>> eliminarSolicitudPropia(@PathVariable Long id, Authentication authentication) {
        solicitudAnuncioService.eliminarSolicitud(id, authentication);
        return ResponseEntity.ok(ApiResponse.success(null, "Solicitud eliminada"));
    }

    @PutMapping("/app/solicitudes-anuncio/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<SolicitudAnuncioResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudAnuncioRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(solicitudAnuncioService.actualizar(id, request, authentication), "Solicitud actualizada"));
    }

    @PatchMapping("/comunicaciones/anuncios-publicados/{id}/visibilidad")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<PublicacionAnuncioResponse>> toggleVisibilidad(
            @PathVariable Long id,
            @RequestParam boolean visible) {
        return ResponseEntity.ok(ApiResponse.success(solicitudAnuncioService.toggleVisibilidad(id, visible), "Visibilidad actualizada"));
    }

    @DeleteMapping("/comunicaciones/anuncios-publicados/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminarPublicacion(@PathVariable Long id) {
        solicitudAnuncioService.eliminarPublicacion(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Publicación eliminada correctamente"));
    }

    @PutMapping("/comunicaciones/anuncios-publicados/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN')")
    public ResponseEntity<ApiResponse<PublicacionAnuncioResponse>> updatePublicacion(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePublicacionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(solicitudAnuncioService.updatePublicacion(id, request), "Publicación actualizada correctamente"));
    }
}

package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.response.AuditoriaRevisionResponse;
import com.calendario.callapp.callapp_backend.service.impl.AuditoriaServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/auditoria")
@RequiredArgsConstructor
/**
 * Consulta del historial de auditoría Hibernate Envers. Restringido a administradores.
 *
 * <p>Expone el historial de revisiones para {@link com.calendario.callapp.callapp_backend.entity.Usuario},
 * {@link com.calendario.callapp.callapp_backend.entity.SolicitudEvento} y
 * {@link com.calendario.callapp.callapp_backend.entity.SolicitudAnuncio} bajo {@code /admin/auditoria}.
 * Cada revisión incluye fecha, tipo de operación (ADD/MOD/DEL) y estado del objeto en ese instante.</p>
 */
public class AuditoriaController {

    private final AuditoriaServiceImpl auditoriaService;

    @GetMapping("/usuarios/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<List<AuditoriaRevisionResponse>>> historialUsuarios(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auditoriaService.historialUsuarios(id)));
    }

    @GetMapping("/solicitudes-evento/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<List<AuditoriaRevisionResponse>>> historialEventos(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auditoriaService.historialSolicitudesEvento(id)));
    }

    @GetMapping("/solicitudes-anuncio/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<List<AuditoriaRevisionResponse>>> historialAnuncios(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auditoriaService.historialSolicitudesAnuncio(id)));
    }
}

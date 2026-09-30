package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.request.OficinaRequest;
import com.calendario.callapp.callapp_backend.dto.response.OficinaResponse;
import com.calendario.callapp.callapp_backend.service.impl.OficinaServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/oficinas")
@RequiredArgsConstructor
/**
 * CRUD de oficinas (unidades académicas/administrativas). Restringido a {@code SUPER_ADMIN} y {@code ADMIN}.
 *
 * <p>Base URL: {@code /admin/oficinas}. Permite crear y listar oficinas activas.
 * Las oficinas son el criterio de segmentación principal de solicitudes en el sistema.</p>
 */
public class OficinaController {

    private final OficinaServiceImpl oficinaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<OficinaResponse>> crear(@Valid @RequestBody OficinaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(oficinaService.crear(request), "Oficina/Programa creado exitosamente"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMUNICACIONES', 'OFICINA', 'USUARIO_AUTENTICADO_APP', 'CONSULTORIA')")
    public ResponseEntity<ApiResponse<List<OficinaResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.success(oficinaService.listar()));
    }
}

package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.response.LugarFisicoResponse;
import com.calendario.callapp.callapp_backend.service.LugarFisicoService;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/lugares-fisicos")
@RequiredArgsConstructor
/**
 * Consulta del catálogo de lugares físicos disponibles para eventos.
 *
 * <p>{@code GET /lugares-fisicos} devuelve sólo los espacios con {@code activo = true},
 * ordenados alfabéticamente. No requiere autenticación especial (cualquier rol autenticado
 * puede consultar el catálogo para crear una solicitud de evento).</p>
 */
public class LugarFisicoController {

    private final LugarFisicoService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LugarFisicoResponse>>> listarActivos() {
        return ResponseEntity.ok(ApiResponse.success(service.listarActivos()));
    }
}

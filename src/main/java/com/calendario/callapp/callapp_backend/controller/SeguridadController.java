package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.response.HistorialLoginResponse;
import com.calendario.callapp.callapp_backend.service.impl.HistorialLoginService;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Panel de seguridad (solo SuperAdmin): historial de inicios de sesión.
 * Reservado a SUPER_ADMIN — a diferencia de /admin/auditoria (que también deja
 * entrar a COMUNICACIONES), este expone IPs y por eso es más restringido.
 */
@RestController
@RequestMapping("/admin/seguridad")
@RequiredArgsConstructor
public class SeguridadController {

    private final HistorialLoginService historialLoginService;

    @GetMapping("/historial-login")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<HistorialLoginResponse>>> historialLogin(
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) Boolean exito,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        int tam = Math.min(Math.max(size, 1), 100); // techo de 100 por página
        Page<HistorialLoginResponse> resultado = historialLoginService
                .buscar(correo, ip, exito, desde, hasta, PageRequest.of(Math.max(page, 0), tam))
                .map(HistorialLoginResponse::desde);

        return ResponseEntity.ok(ApiResponse.success(resultado));
    }
}

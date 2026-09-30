package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.request.UsuarioRequest;
import com.calendario.callapp.callapp_backend.dto.response.UsuarioResponse;
import com.calendario.callapp.callapp_backend.entity.Rol;
import com.calendario.callapp.callapp_backend.service.impl.UsuarioServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
/**
 * CRUD de usuarios del sistema. Restringido a {@code SUPER_ADMIN} y {@code ADMIN}.
 *
 * <p>Permite crear, consultar, actualizar, desactivar y buscar usuarios.
 * La búsqueda admite filtros por nombre, rol y oficina.
 * Base URL: {@code /admin/usuarios}.</p>
 */
public class UsuarioController {

    private final UsuarioServiceImpl usuarioService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> crearUsuario(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(usuarioService.crearUsuario(request), "Usuario creado exitosamente"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(usuarioService.obtenerUsuario(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(ApiResponse.success(usuarioService.actualizarUsuario(id, request), "Usuario actualizado exitosamente"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMUNICACIONES')")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listarUsuarios(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(ApiResponse.success(usuarioService.listarUsuarios(q, rol, estado)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Usuario eliminado exitosamente"));
    }
}

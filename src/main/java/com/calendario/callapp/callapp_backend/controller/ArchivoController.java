package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.response.ArchivoResponse;
import com.calendario.callapp.callapp_backend.service.ArchivoStorageService;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping
/**
 * Gestión de archivos adjuntos: subida con validación y descarga pública por token.
 *
 * <p>{@code POST /comunicaciones/archivos/upload} — sube el archivo y devuelve el
 * {@code tokenAcceso} UUID que se usa para la URL pública de descarga.<br>
 * {@code GET /archivos/public/{tokenAcceso}} — sirve el archivo sin autenticación;
 * detecta el content-type automáticamente para que el navegador lo muestre inline.</p>
 */
public class ArchivoController {

    private final ArchivoStorageService archivoService;

    @PostMapping("/comunicaciones/archivos/upload")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN', 'OFICINA', 'USUARIO_AUTENTICADO_APP')")
    public ResponseEntity<ApiResponse<ArchivoResponse>> subir(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(ApiResponse.success(archivoService.guardar(archivo), "Archivo subido correctamente"));
    }

    @GetMapping("/archivos/public/{tokenAcceso}")
    public ResponseEntity<Resource> descargar(@PathVariable String tokenAcceso) {
        Resource recurso = archivoService.cargarPublicoComoRecurso(tokenAcceso);
        String contentType = null;
        try {
            if (recurso instanceof org.springframework.core.io.UrlResource) {
                contentType = java.nio.file.Files.probeContentType(java.nio.file.Paths.get(recurso.getURI()));
            } else if (recurso instanceof org.springframework.core.io.ClassPathResource) {
                // Para recursos en el classpath, usamos el nombre del archivo
                String filename = recurso.getFilename();
                if (filename != null) {
                    if (filename.endsWith(".png")) contentType = "image/png";
                    else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) contentType = "image/jpeg";
                }
            }
        } catch (Exception e) {
            // fallback a octet-stream si falla la detección
        }
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .body(recurso);
    }
}

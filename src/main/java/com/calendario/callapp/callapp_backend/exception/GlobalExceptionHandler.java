package com.calendario.callapp.callapp_backend.exception;

import com.calendario.callapp.callapp_backend.util.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@lombok.extern.slf4j.Slf4j
/**
 * Manejador global de excepciones para toda la API REST.
 *
 * <p>Intercepta las excepciones más comunes y las serializa como {@link com.calendario.callapp.callapp_backend.util.ApiResponse}
 * con el código HTTP correspondiente:</p>
 * <ul>
 *   <li>{@link org.springframework.web.server.ResponseStatusException} — usa el código y mensaje del lanzador.</li>
 *   <li>{@link org.springframework.web.bind.MethodArgumentNotValidException} — 400 con mapa campo→error de validación JSR-303.</li>
 *   <li>{@link org.springframework.dao.DataIntegrityViolationException} — 409 por violación de restricción única en BD.</li>
 *   <li>{@link org.springframework.security.access.AccessDeniedException} — 403 (complementa al {@code JwtAccessDeniedHandler}).</li>
 *   <li>Resto de {@code Exception} — 500 genérico, loguea el stack trace.</li>
 * </ul>
 */
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ApiResponse.error(ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Error de validación")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        String mensaje = "El registro no puede ser guardado porque viola una restricción de unicidad o integridad en la base de datos.";
        // Intentar dar un mensaje más específico si es constraint de correo o similar
        String causa = ex.getMostSpecificCause().getMessage();
        if (causa != null && causa.toLowerCase().contains("correo")) {
            mensaje = "Ya existe un registro con ese correo electrónico.";
        } else if (causa != null && causa.toLowerCase().contains("telefono")) {
            mensaje = "Ya existe un registro con ese número de teléfono.";
        }
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(mensaje));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permisos para realizar esta acción."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("CRITICAL ERROR: {}", ex.getMessage(), ex);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Ha ocurrido un error interno. Por favor contacta al administrador."));
    }
}

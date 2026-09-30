package com.calendario.callapp.callapp_backend.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
/**
 * Envoltorio universal de respuesta HTTP para toda la API REST.
 *
 * <p>Estructura de respuesta: {@code success} (boolean), {@code message} (texto legible),
 * {@code data} (payload tipado {@code T}), {@code timestamp} (marca de tiempo UTC).
 * Factories estáticas: {@link #success(Object, String)}, {@link #success(Object)} y
 * {@link #error(String)} para construir respuestas sin usar el builder directamente.</p>
 */
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operación exitosa");
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

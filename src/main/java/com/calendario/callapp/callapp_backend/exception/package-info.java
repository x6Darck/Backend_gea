/**
 * Manejo global de excepciones de la API REST.
 *
 * <p>Contiene {@code GlobalExceptionHandler} ({@link org.springframework.web.bind.annotation.RestControllerAdvice})
 * que intercepta excepciones de toda la aplicación y las serializa como
 * {@link com.calendario.callapp.callapp_backend.util.ApiResponse} con el código HTTP correspondiente.</p>
 */
package com.calendario.callapp.callapp_backend.exception;

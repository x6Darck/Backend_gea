package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.util.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
/**
 * Maneja el error 403 Forbidden serializando la respuesta como JSON {@code ApiResponse}.
 *
 * <p>Se invoca cuando un usuario autenticado intenta acceder a un recurso para el que
 * no tiene el rol necesario. Devuelve un cuerpo JSON estándar en lugar de la
 * respuesta de error por defecto de Spring Security.</p>
 */
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.error("No tienes permisos para acceder a este recurso");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

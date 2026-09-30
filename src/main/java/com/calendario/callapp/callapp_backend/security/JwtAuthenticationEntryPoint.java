package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.util.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
/**
 * Maneja el error 401 Unauthorized serializando la respuesta como JSON {@code ApiResponse}.
 *
 * <p>Spring Security invoca este handler cuando una petición no autenticada intenta
 * acceder a un recurso protegido. Devuelve un cuerpo JSON estándar en lugar de la
 * página de error HTML por defecto.</p>
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.error("Debes autenticarte para acceder a este recurso");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

package com.calendario.callapp.callapp_backend.service;

import com.calendario.callapp.callapp_backend.dto.request.AuthRequest;
import com.calendario.callapp.callapp_backend.dto.response.AuthResponse;

/**
 * Contrato del servicio de autenticación local (usuario y contraseña).
 *
 * <p>La autenticación Microsoft OAuth2 tiene su propio servicio
 * ({@code MicrosoftAuthServiceImpl}) y no forma parte de esta interfaz.
 * Implementado por {@code AuthServiceImpl}.</p>
 */
public interface AuthService {

    /**
     * Autentica un usuario con correo y contraseña y emite un JWT de sesión.
     *
     * @param request credenciales de login (correo + contraseña)
     * @return DTO con el token JWT, nombre de usuario, rol y datos de la sesión
     * @throws org.springframework.web.server.ResponseStatusException 401 si las credenciales son inválidas
     *         o la cuenta está inactiva
     */
    AuthResponse login(AuthRequest request);
}
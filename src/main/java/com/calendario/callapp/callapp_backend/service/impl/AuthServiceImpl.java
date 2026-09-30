package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.request.AuthRequest;
import com.calendario.callapp.callapp_backend.dto.response.AuthResponse;
import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.MotivoFalloLogin;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.security.JwtService;
import com.calendario.callapp.callapp_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * Implementación del servicio de autenticación local (correo + contraseña).
 *
 * <p>Valida credenciales contra la base de datos y emite un JWT de sesión
 * mediante {@link JwtService}. Devuelve el mismo mensaje de error genérico
 * para correo no encontrado, cuenta inactiva y contraseña incorrecta, a fin de
 * no revelar información sobre qué dato falló (defensa contra enumeración de usuarios).</p>
 */
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final HistorialLoginService historialLoginService;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {

        String correo = request.getCorreo();
        Usuario usuario = usuarioRepository.getByCorreoOptimized(correo).orElse(null);

        if (usuario == null) {
            log.warn("Intento de login fallido - correo no registrado: {}", maskEmail(correo));
            historialLoginService.registrarFallo(correo, null, AuthProvider.LOCAL, MotivoFalloLogin.CORREO_NO_REGISTRADO);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }

        if (!"ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
            log.warn("Intento de login con cuenta inactiva: {}", maskEmail(correo));
            historialLoginService.registrarFallo(correo, usuario.getId(), AuthProvider.LOCAL, MotivoFalloLogin.CUENTA_INACTIVA);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("Intento de login fallido - contraseña incorrecta para: {}", maskEmail(correo));
            historialLoginService.registrarFallo(correo, usuario.getId(), AuthProvider.LOCAL, MotivoFalloLogin.CREDENCIALES_INVALIDAS);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }

        String token = jwtService.generarToken(usuario);
        log.info("Login exitoso: {} [{}]", maskEmail(correo), usuario.getRol());
        historialLoginService.registrarExito(correo, usuario.getId(), AuthProvider.LOCAL);

        Oficina oficina = usuario.getOficina();
        Long idOficina = oficina != null ? oficina.getId() : null;
        String oficinaNombre = oficina != null ? oficina.getNombre() : null;

        return AuthResponse.builder()
                .id(usuario.getId())
                .token(token)
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol().name())
                .idOficina(idOficina)
                .oficinaNombre(oficinaNombre)
                .fotoUrl(usuario.getFotoUrl())
                .build();
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String masked = local.length() <= 2 ? "**" : local.substring(0, 2) + "***";
        return masked + email.substring(at);
    }
}
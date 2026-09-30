package com.calendario.callapp.callapp_backend.service;

import com.calendario.callapp.callapp_backend.dto.request.DispositivoTokenRequest;
import org.springframework.security.core.Authentication;

/**
 * Contrato para la gestión de tokens FCM de dispositivos móviles.
 *
 * <p>La app Flutter llama a {@code registrarToken} al iniciar sesión y a
 * {@code removerToken} al cerrar sesión para mantener sincronizados los tokens
 * con los que {@code PushNotificationService} envía notificaciones push.
 * Implementado por {@code DispositivoUsuarioServiceImpl}.</p>
 */
public interface DispositivoUsuarioService {

    /**
     * Registra o actualiza el token FCM del dispositivo del usuario autenticado.
     * Si el token ya existe, actualiza el registro; si no, crea uno nuevo.
     *
     * @param request       DTO con el token FCM del dispositivo
     * @param authentication contexto de seguridad del usuario que realiza la petición
     */
    void registrarToken(DispositivoTokenRequest request, Authentication authentication);

    /**
     * Elimina el token FCM del dispositivo para que no reciba más notificaciones push.
     * Llamado al cerrar sesión en la app móvil.
     *
     * @param token         token FCM a eliminar
     * @param authentication contexto de seguridad del usuario propietario del dispositivo
     */
    void removerToken(String token, Authentication authentication);
}

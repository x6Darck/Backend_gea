package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.DispositivoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;

/**
 * Repositorio de {@link DispositivoUsuario} — tokens FCM registrados por los usuarios.
 *
 * <p>Usado por {@code DispositivoUsuarioServiceImpl} para registrar/actualizar tokens
 * y por {@code PushNotificationService} para el envío masivo de notificaciones push.</p>
 */
public interface DispositivoUsuarioRepository extends JpaRepository<DispositivoUsuario, Long> {

    /** Busca el registro de dispositivo por token FCM exacto. */
    Optional<DispositivoUsuario> findByToken(String token);

    /** Devuelve todos los dispositivos registrados por un usuario (puede tener más de uno). */
    List<DispositivoUsuario> findByUsuarioId(Long usuarioId);

    /** Elimina el registro del token FCM dado; útil al revocar acceso o al reutilizar el token. */
    void deleteByToken(String token);

    /**
     * Devuelve solo los strings de tokens FCM distintos (no las entidades completas).
     * Optimización para el envío masivo: evita cargar todos los campos de {@link DispositivoUsuario}.
     * Llamado por {@code PushNotificationService} para notificar a todos los dispositivos activos.
     *
     * @return lista de tokens FCM no nulos y sin duplicados
     */
    @Query("SELECT DISTINCT d.token FROM DispositivoUsuario d WHERE d.token IS NOT NULL")
    List<String> findAllTokens();
}

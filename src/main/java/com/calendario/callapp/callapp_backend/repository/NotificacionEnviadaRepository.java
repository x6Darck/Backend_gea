package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.NotificacionEnviada;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de {@link NotificacionEnviada} — log de correos y notificaciones push enviadas.
 *
 * <p>Usado por {@code NotificacionServiceImpl} para persistir el registro de cada envío.
 * Solo usa las operaciones CRUD heredadas de {@code JpaRepository}.</p>
 */
public interface NotificacionEnviadaRepository extends JpaRepository<NotificacionEnviada, Long> {
}

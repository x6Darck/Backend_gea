package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Registro de auditoría de cada notificación enviada (correo o push).
 *
 * <p>Almacenado en la tabla {@code notificaciones_enviadas}. Cada vez que
 * {@code NotificacionServiceImpl} envía un correo o una notificación push,
 * persiste un registro con el tipo, destinatarios, asunto, fecha de envío,
 * si el envío fue exitoso y el detalle del error en caso de fallo.
 * Permite diagnosticar problemas de entrega sin revisar logs del servidor.</p>
 */
@Entity
@Table(name = "notificaciones_enviadas")
@Data
public class NotificacionEnviada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion_enviada")
    private Long id;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(length = 1000)
    private String destinatarios;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(nullable = false)
    private Boolean exito;

    @Column(name = "detalle_error", length = 1000)
    private String detalleError;
}

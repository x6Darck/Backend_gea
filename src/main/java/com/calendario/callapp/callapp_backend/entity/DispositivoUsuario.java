package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * Token FCM (Firebase Cloud Messaging) asociado a un dispositivo móvil del usuario.
 *
 * <p>Almacenado en la tabla {@code dispositivos_usuario}. La app Flutter registra
 * su token FCM al iniciar sesión mediante {@code POST /usuario/dispositivo/token}.
 * {@code PushNotificationService} consulta todos los tokens activos de la tabla
 * para enviar notificaciones push masivas al publicar un evento. Un mismo usuario
 * puede tener varios registros (múltiples dispositivos); el token es único a nivel
 * global ({@code unique = true}).</p>
 */
@Entity
@Table(name = "dispositivos_usuario", indexes = {
    @Index(name = "idx_dispositivo_token", columnList = "token")
})
@Data
@EqualsAndHashCode(callSuper = false)
public class DispositivoUsuario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispositivo")
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}

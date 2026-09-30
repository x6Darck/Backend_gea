package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Un intento de inicio de sesión (exitoso o fallido) registrado para el panel
 * de seguridad. Tabla {@code historial_login}: solo-inserción, nunca se
 * actualiza; por eso NO extiende {@link BaseEntity} ni se anota {@code @Audited}
 * (ya es en sí mismo un registro de auditoría).
 */
@Entity
@Table(name = "historial_login")
@Data
public class HistorialLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo tal como se escribió en el formulario de login. */
    @Column(name = "correo_intentado", nullable = false, length = 160)
    private String correoIntentado;

    /**
     * Usuario real asociado, o null si el correo no existe. FK cruda (no
     * {@code @ManyToOne}) para evitar el costo de lazy-load en este log de
     * alto volumen y solo-inserción.
     */
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(nullable = false)
    private Boolean exito;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthProvider metodo;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_fallo", length = 40)
    private MotivoFalloLogin motivoFallo;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 400)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime fecha;
}

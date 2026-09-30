package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.envers.Audited;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;


import jakarta.persistence.Index;

/**
 * Representa a un usuario del sistema GEA.
 *
 * <p>Almacenado en la tabla {@code usuarios}. Un usuario pertenece a una
 * {@link Oficina} (puede ser nula para administradores globales) y tiene
 * exactamente un {@link RolEntity} que determina sus permisos.
 * Admite autenticación local (contraseña bcrypt) y autenticación OAuth2
 * de Microsoft ({@link AuthProvider#MICROSOFT}), identificada por
 * {@code microsoftOid}. La columna {@code estado} marca si la cuenta
 * está activa o inactiva.</p>
 */
@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuario_correo", columnList = "correo"),
    @Index(name = "idx_usuario_microsoft_oid", columnList = "microsoft_oid")
})
@Data
@Audited
@lombok.EqualsAndHashCode(callSuper = false)
public class Usuario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "password", nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol")
    private RolEntity rolEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_oficina")
    private Oficina oficina;

    @Column(name = "estado")
    private String estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 30)
    private AuthProvider authProvider;

    @Column(name = "microsoft_oid", unique = true)
    private String microsoftOid;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    /**
     * Retorna el enum Rol mapeado desde la entidad RolEntity.
     * Esto permite mantener compatibilidad con la logica de seguridad
     * sin duplicar datos en la base de datos.
     */
    @Transient
    public Rol getRol() {
        return Rol.fromNombre(rolEntity != null ? rolEntity.getNombre() : null);
    }
}

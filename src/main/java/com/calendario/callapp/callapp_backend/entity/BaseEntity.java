package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Clase base de la que heredan todas las entidades auditables.
 *
 * <p>Provee los cuatro campos de auditoría JPA: fechas de creación y última
 * modificación, y el nombre de usuario responsable de cada operación.
 * Spring Data los rellena automáticamente gracias a {@link AuditingEntityListener}
 * y a {@link org.springframework.data.domain.AuditorAware} configurado en
 * {@code JpaAuditingConfig}.</p>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
    @CreatedBy
    @Column(name = "usuario_creacion", updatable = false)
    private String usuarioCreacion;

    @LastModifiedBy
    @Column(name = "usuario_actualizacion")
    private String usuarioActualizacion;
}

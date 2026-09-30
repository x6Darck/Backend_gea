package com.calendario.callapp.callapp_backend.entity;

import com.calendario.callapp.callapp_backend.util.AuditRevisionListener;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

/**
 * Entidad de revisión personalizada de Hibernate Envers.
 *
 * <p>Extiende {@code DefaultRevisionEntity} añadiendo el nombre del usuario
 * autenticado y su dirección IP en el momento del cambio. Estos datos los
 * rellena {@link com.calendario.callapp.callapp_backend.util.AuditRevisionListener}
 * al inicio de cada revisión. Se usa la tabla {@code audit_revision_info} en
 * lugar de la tabla {@code revinfo} por defecto, para evitar conflictos con
 * esquemas anteriores y garantizar portabilidad entre entornos.</p>
 */
@Entity
@Table(name = "audit_revision_info")
@AttributeOverrides({
    @AttributeOverride(name = "id", column = @Column(name = "rev")),
    @AttributeOverride(name = "timestamp", column = @Column(name = "revtstmp"))
})
@Data
@EqualsAndHashCode(callSuper = true)
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevisionEntity extends DefaultRevisionEntity {

    @Column(name = "nombre_usuario")
    private String nombreUsuario;

    @Column(name = "ip_address")
    private String ipAddress;
}

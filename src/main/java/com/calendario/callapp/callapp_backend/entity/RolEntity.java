package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

/**
 * Entidad JPA que persiste los roles disponibles en la tabla {@code roles}.
 *
 * <p>El nombre almacenado (p. ej. {@code "Comunicaciones"}, {@code "Oficina"})
 * se traduce al enum {@link Rol} mediante {@link Rol#fromNombre(String)}.
 * Esta separación permite añadir roles en base de datos sin recompilar el
 * código, manteniendo la lógica de negocio en el enum.</p>
 */
@Entity
@Table(name = "roles")
@Data
@Audited
public class RolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}

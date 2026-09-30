package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.envers.Audited;

/**
 * Espacio físico donde puede realizarse un evento o anuncio.
 *
 * <p>Almacenado en la tabla {@code lugares_fisicos}. Cuando {@code esExterno}
 * es {@code true}, el lugar no pertenece al campus y se usa únicamente como
 * referencia descriptiva. Los lugares desactivados ({@code activo = false}) no
 * aparecen en las listas de selección, pero conservan la relación con
 * solicitudes históricas. Se usa en tablas de unión
 * {@code solicitud_evento_lugares} y {@code solicitud_anuncio_lugares}.</p>
 */
@Entity
@Table(name = "lugares_fisicos")
@Data
@Audited
@lombok.EqualsAndHashCode(callSuper = false)
public class LugarFisico extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lugar_fisico")
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column
    private Integer capacidad;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "es_externo", nullable = false, columnDefinition = "boolean default false")
    private Boolean esExterno = false;
}

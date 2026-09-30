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
 * Unidad organizativa (dependencia, facultad o programa) que solicita eventos y anuncios.
 *
 * <p>Almacenada en la tabla {@code oficinas}. Cada {@link Usuario} con rol
 * {@link Rol#OFICINA} o {@link Rol#USUARIO_AUTENTICADO_APP} pertenece a una
 * oficina. Las solicitudes de evento quedan vinculadas a la oficina del
 * solicitante. El campo {@code activa} controla si la oficina puede seguir
 * enviando solicitudes.</p>
 */
@Entity
@Table(name = "oficinas")
@Data
@Audited
@lombok.EqualsAndHashCode(callSuper = false)
public class Oficina extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_oficina")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(name = "programa_academico", length = 120)
    private String programaAcademico;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activa;
}

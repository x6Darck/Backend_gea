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
 * Catálogo de tipos de evento (p. ej. Congreso, Taller, Conferencia).
 *
 * <p>Almacenado en la tabla {@code tipos_evento}. Cada {@link SolicitudEvento}
 * referencia opcionalmente un tipo de evento para clasificación y filtrado.
 * El campo {@code colorHex} (formato {@code #RRGGBB}) se usa en el calendario
 * del frontend para colorear eventos por categoría.
 * Los tipos desactivados ({@code activo = false}) no se ofrecen al crear nuevas
 * solicitudes, pero permanecen vinculados a solicitudes existentes.</p>
 */
@Entity
@Table(name = "tipos_evento")
@Data
@Audited
public class TipoEventoCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_evento")
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Column(nullable = false)
    private Boolean activo;
}

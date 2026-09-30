package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.envers.Audited;

/**
 * Persona que participa como ponente, invitado o moderador en un evento.
 *
 * <p>Almacenado en la tabla {@code solicitud_evento_participantes}.
 * Tiene relación {@code @ManyToOne} con {@link SolicitudEvento}; la colección
 * inversa se gestiona con {@code CascadeType.ALL} y {@code orphanRemoval = true},
 * de modo que borrar la solicitud elimina en cascada sus participantes.
 * El tipo de participante se define en {@link TipoParticipante}.</p>
 */
@Entity
@Table(name = "solicitud_evento_participantes")
@Data
@lombok.EqualsAndHashCode(exclude = "solicitudEvento")
@Audited
public class SolicitudEventoParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participante")
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 120)
    private String cargo;

    @Column(length = 1000)
    private String descripcion;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(length = 20)
    private String telefono;

    @Column(length = 120)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoParticipante tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud_evento", nullable = false)
    private SolicitudEvento solicitudEvento;
}

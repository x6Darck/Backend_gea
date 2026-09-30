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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Data;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Solicitud de anuncio enviada por un usuario de la app y sujeta a revisión de Comunicaciones.
 *
 * <p>Almacenada en la tabla {@code solicitudes_anuncio}. Sigue el mismo ciclo
 * de estados que {@link SolicitudEvento} ({@link EstadoSolicitud}), pero no
 * admite series recurrentes. Puede llevar archivos adjuntos gestionados a
 * través de {@link ArchivoAdjunto}. El rango
 * {@code fechaInicioPublicacion} – {@code fechaFinPublicacion} indica el
 * período durante el cual el anuncio debe aparecer visible al público.</p>
 */
@Entity
@Table(name = "solicitudes_anuncio")
@Data
@Audited
@lombok.EqualsAndHashCode(callSuper = false)
public class SolicitudAnuncio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud_anuncio")
    private Long id;

    @Column(name = "titulo", nullable = false, length = 160)
    private String titulo;

    @Column(length = 2000)
    private String descripcion;

    @Column(length = 100)
    private String categoria;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE})
    @JoinTable(
        name = "solicitud_anuncio_lugares",
        joinColumns = @JoinColumn(name = "id_solicitud_anuncio"),
        inverseJoinColumns = @JoinColumn(name = "id_lugar_fisico")
    )
    // Ver el comentario equivalente en SolicitudEvento.lugaresFisicos: @BatchSize
    // evita el N+1 sin meter esta colección en el JOIN FETCH principal, que
    // rompería la paginación SQL de los listados (HHH90003004).
    @org.hibernate.annotations.BatchSize(size = 500)
    private List<LugarFisico> lugaresFisicos = new java.util.ArrayList<>();

    @Column(name = "correo_contacto", length = 150)
    private String correoContacto;

    @Column(name = "responsable_anuncio", length = 150)
    private String responsableAnuncio;

    @Column(name = "fecha_inicio_publicacion")
    private LocalDate fechaInicioPublicacion;

    @Column(name = "fecha_fin_publicacion")
    private LocalDate fechaFinPublicacion;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "pieza_grafica_url", length = 500)
    private String piezaGraficaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(length = 1000)
    private String motivoRechazo;

    @Column(length = 1000)
    private String observacionesRevision;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_solicitante", nullable = false)
    private Usuario usuarioSolicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_oficina")
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_revisor")
    private Usuario usuarioRevisor;

    @Column(name = "requiere_pieza_grafica", nullable = false)
    private Boolean requierePiezaGrafica = false;
}

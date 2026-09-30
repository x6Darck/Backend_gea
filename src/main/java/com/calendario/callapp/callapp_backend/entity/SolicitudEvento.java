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
import org.hibernate.envers.Audited;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Solicitud de evento creada por una oficina y sujeta al ciclo de revisión de Comunicaciones.
 *
 * <p>Almacenada en la tabla {@code solicitudes_evento}. El campo {@link #estado}
 * controla en qué etapa del ciclo se encuentra la solicitud:
 * {@code PENDIENTE → APROBADA → PUBLICADA}, con posibilidad de
 * {@code RECHAZADA} o devolución a {@code EN_REVISION}.
 * Para eventos recurrentes, varias solicitudes comparten el mismo
 * {@code idGrupoRecurrencia} (UUID) y la marcada como {@code esPrincipal = true}
 * es la representante de la serie. La frecuencia de recurrencia se define en
 * {@link FrecuenciaRecurrencia}. Los lugares donde se realiza el evento se
 * guardan en la tabla de unión {@code solicitud_evento_lugares}.</p>
 */
@Entity
@Table(name = "solicitudes_evento", indexes = {
    @jakarta.persistence.Index(name = "idx_solicitud_evento_estado", columnList = "estado"),
    @jakarta.persistence.Index(name = "idx_solicitud_evento_oficina", columnList = "id_oficina"),
    @jakarta.persistence.Index(name = "idx_solicitud_evento_fecha", columnList = "fecha_evento"),
    @jakarta.persistence.Index(name = "idx_solicitud_evento_grupo", columnList = "id_grupo_recurrencia")
})
@Getter
@Setter
@Audited
@lombok.EqualsAndHashCode(callSuper = false, exclude = "participantes")
public class SolicitudEvento extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long id;

    @Column(name = "nombre_evento", nullable = false, length = 160)
    private String nombreEvento;

    @Column(name = "descripcion_evento", length = 2000)
    private String descripcionEvento;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDate fechaEvento;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "solicitud_evento_lugares",
        joinColumns = @JoinColumn(name = "id_solicitud_evento"),
        inverseJoinColumns = @JoinColumn(name = "id_lugar_fisico")
    )
    // Sin esto, listar publicaciones con JOIN FETCH de esta colección rompe la
    // paginación SQL (Hibernate no puede aplicar LIMIT con un JOIN a un
    // to-many y trae todas las filas a memoria — visto en pruebas de carga
    // reales, warning HHH90003004). Con @BatchSize, la colección se queda
    // lazy normal (no entra en el JOIN FETCH principal, que sí pagina bien),
    // y Hibernate la carga en un único "WHERE id IN (...)" para toda la
    // página en cuanto se toca la primera fila — evita el N+1 sin sacrificar
    // la paginación real.
    @org.hibernate.annotations.BatchSize(size = 500)
    private List<LugarFisico> lugaresFisicos = new java.util.ArrayList<>();

    @Column(name = "link_conexion", length = 500)
    private String linkConexion;

    @Column(name = "ubicacion_externa", length = 300)
    private String ubicacionExterna;

    @Column(name = "responsable_evento", length = 120)
    private String responsableEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_evento")
    private TipoEventoCatalogo tipoEventoCatalogo;

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
    @JoinColumn(name = "id_oficina", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_solicitante", nullable = false)
    private Usuario usuarioSolicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_revisor")
    private Usuario usuarioRevisor;

    @Column(name = "pieza_grafica_url", length = 500)
    private String piezaGraficaUrl;

    @Column(name = "requiere_transmision", nullable = false)
    private Boolean requiereTransmision = false;

    @Column(name = "requiere_cubrimiento", nullable = false)
    private Boolean requiereCubrimiento = false;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    @Column(name = "es_importante", nullable = false)
    private Boolean esImportante = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ingreso", nullable = false, length = 20)
    private TipoIngreso tipoIngreso = TipoIngreso.LIBRE;

    @Column(name = "requiere_pieza_grafica", nullable = false)
    private Boolean requierePiezaGrafica = false;

    @Column(name = "requiere_servicios_generales", nullable = false)
    private Boolean requiereServiciosGenerales = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "frecuencia_recurrencia", length = 20)
    private FrecuenciaRecurrencia frecuenciaRecurrencia = FrecuenciaRecurrencia.NINGUNA;

    @Column(name = "fecha_fin_recurrencia")
    private LocalDate fechaFinRecurrencia;

    @Column(name = "id_grupo_recurrencia", length = 50)
    private String idGrupoRecurrencia;

    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = false;

    // @BatchSize por el mismo motivo que lugaresFisicos arriba: evita N+1 al
    // recorrer varias solicitudes (ej. AgendaPdfService) sin romper paginación
    // SQL con un JOIN FETCH sobre una colección to-many.
    @OneToMany(mappedBy = "solicitudEvento", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 500)
    @lombok.Setter(lombok.AccessLevel.NONE)
    private List<SolicitudEventoParticipante> participantes = new ArrayList<>();
}

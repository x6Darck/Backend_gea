package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

/**
 * Registro público de un evento aprobado y listo para ser visto por el público.
 *
 * <p>Almacenado en la tabla {@code publicaciones_evento}. Se crea cuando
 * Comunicaciones ejecuta la acción {@code publicar} sobre una
 * {@link SolicitudEvento} en estado {@code APROBADA}. El campo
 * {@code visible} permite ocultar temporalmente la publicación sin
 * eliminarla. Los consumidores de la app Flutter y el sitio público
 * leen únicamente de esta tabla, no de {@link SolicitudEvento}.</p>
 */
@Entity
@Table(name = "publicaciones_evento", indexes = {
    @jakarta.persistence.Index(name = "idx_publicacion_evento_visible", columnList = "visible"),
    @jakarta.persistence.Index(name = "idx_publicacion_evento_solicitud", columnList = "id_solicitud_evento")
})
@Data
@Audited
public class PublicacionEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_publicacion_evento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud_evento", nullable = false)
    private SolicitudEvento solicitudEvento;

    @Column(name = "titulo_visible", nullable = false, length = 160)
    private String tituloVisible;

    @Column(name = "descripcion_visible", length = 2000)
    private String descripcionVisible;

    @Column(name = "pieza_grafica_url", length = 500)
    private String piezaGraficaUrl;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion;

    @Column(nullable = false)
    private Boolean visible;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_publicador", nullable = false)
    private Usuario usuarioPublicador;
}

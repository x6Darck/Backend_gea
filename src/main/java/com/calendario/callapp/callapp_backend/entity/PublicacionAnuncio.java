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
 * Versión pública de un anuncio aprobado, visible en la app y el sitio.
 *
 * <p>Almacenada en la tabla {@code publicaciones_anuncio}. Se crea cuando
 * Comunicaciones publica una {@link SolicitudAnuncio} aprobada. El campo
 * {@code visible} permite ocultarla sin eliminarla. El título y descripción
 * visibles pueden diferir del original si Comunicaciones los ajusta durante
 * la publicación.</p>
 */
@Entity
@Table(name = "publicaciones_anuncio")
@Data
@Audited
public class PublicacionAnuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_publicacion_anuncio")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud_anuncio", nullable = false)
    private SolicitudAnuncio solicitudAnuncio;

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

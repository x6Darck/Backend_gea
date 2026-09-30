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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro histórico de un reporte generado por un usuario.
 *
 * <p>Almacenado en la tabla {@code reportes_generados}. Cuando
 * {@code ReporteServiceImpl} genera un informe (PDF o Excel), persiste aquí
 * los parámetros usados (rango de fechas, alcance, filtros de oficina y tipo
 * de evento) junto con el usuario que lo solicitó. Sirve para auditoría y para
 * regenerar el mismo reporte sin re-ingresar los parámetros.</p>
 */
@Entity
@Table(name = "reportes_generados")
@Data
public class ReporteGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte_generado")
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String formato;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    @Column(nullable = false, length = 30)
    private String alcance;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "id_oficina")
    private Long idOficina;

    @Column(name = "id_tipo_evento")
    private Long idTipoEvento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_generador", nullable = false)
    private Usuario usuarioGenerador;
}

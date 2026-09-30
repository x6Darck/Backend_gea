package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface HistorialLoginRepository extends JpaRepository<HistorialLogin, Long> {

    /**
     * Busca registros aplicando solo los filtros no nulos, ordenados por fecha
     * descendente (más reciente primero). Todos los parámetros son opcionales:
     * un null desactiva ese filtro.
     */
    @Query("""
            SELECT h FROM HistorialLogin h
            WHERE (:correo IS NULL OR LOWER(h.correoIntentado) LIKE LOWER(CONCAT('%', :correo, '%')))
              AND (:ip IS NULL OR h.ip = :ip)
              AND (:exito IS NULL OR h.exito = :exito)
              AND (:desde IS NULL OR h.fecha >= :desde)
              AND (:hasta IS NULL OR h.fecha < :hasta)
            ORDER BY h.fecha DESC
            """)
    Page<HistorialLogin> buscar(@Param("correo") String correo,
                                @Param("ip") String ip,
                                @Param("exito") Boolean exito,
                                @Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta,
                                Pageable pageable);

    /** Borra los registros anteriores a la fecha de corte (purga por retención). */
    @Modifying
    @Query("DELETE FROM HistorialLogin h WHERE h.fecha < :corte")
    int deleteByFechaBefore(@Param("corte") LocalDateTime corte);
}

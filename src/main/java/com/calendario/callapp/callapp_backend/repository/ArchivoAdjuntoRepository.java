package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.ArchivoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de {@link ArchivoAdjunto} — metadatos de archivos subidos al sistema.
 *
 * <p>Usado por {@code ArchivoServiceImpl} para persistir y recuperar los metadatos
 * de archivos almacenados en el directorio {@code uploads/} del servidor.</p>
 */
public interface ArchivoAdjuntoRepository extends JpaRepository<ArchivoAdjunto, Long> {

    /**
     * Busca un archivo público por su token de acceso.
     * Usado para servir archivos sin autenticación en {@code GET /archivos/public/{filename}}.
     *
     * @param tokenAcceso token único del archivo
     * @return el archivo si existe y es público, vacío en caso contrario
     */
    Optional<ArchivoAdjunto> findByTokenAccesoAndPublicoTrue(String tokenAcceso);

    /**
     * Busca un archivo por su nombre original de subida.
     *
     * @param nombreOriginal nombre tal como lo envió el cliente
     * @return el archivo si existe, vacío en caso contrario
     */
    Optional<ArchivoAdjunto> findByNombreOriginal(String nombreOriginal);
}

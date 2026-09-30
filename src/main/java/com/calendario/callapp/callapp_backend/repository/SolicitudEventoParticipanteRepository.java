package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.SolicitudEventoParticipante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de {@link SolicitudEventoParticipante} — ponentes, invitados y patrocinadores de un evento.
 *
 * <p>Usado por {@code SolicitudEventoServiceImpl} para reemplazar la lista de participantes
 * al actualizar una solicitud.</p>
 */
public interface SolicitudEventoParticipanteRepository extends JpaRepository<SolicitudEventoParticipante, Long> {

    /** Devuelve todos los participantes de una solicitud de evento por su ID. */
    List<SolicitudEventoParticipante> findBySolicitudEventoId(Long solicitudEventoId);

    /**
     * Elimina en bloque todos los participantes de una solicitud.
     * Se llama antes de insertar la nueva lista al actualizar una solicitud,
     * dado que los participantes no son reemplazados por diff sino por borrado total y re-inserción.
     *
     * @param solicitudEventoId ID de la solicitud cuyos participantes se eliminan
     */
    void deleteBySolicitudEventoId(Long solicitudEventoId);
}

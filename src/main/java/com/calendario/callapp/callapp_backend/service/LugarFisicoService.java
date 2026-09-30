package com.calendario.callapp.callapp_backend.service;

import com.calendario.callapp.callapp_backend.dto.response.LugarFisicoResponse;
import java.util.List;

/**
 * Contrato de consulta de lugares físicos disponibles para eventos.
 *
 * <p>Solo expone operaciones de lectura; el CRUD completo de lugares está
 * disponible para administradores en {@code LugarFisicoServiceImpl} (que también
 * implementa esta interfaz). Usado por {@code LugarFisicoController}.</p>
 */
public interface LugarFisicoService {

    /**
     * Devuelve todos los lugares físicos activos ordenados alfabéticamente.
     * Usado para poblar los selectores de lugar al crear una solicitud.
     *
     * @return lista de lugares activos como DTOs de respuesta
     */
    List<LugarFisicoResponse> listarActivos();

    /**
     * Obtiene un lugar físico por su ID.
     *
     * @param id identificador del lugar
     * @return el lugar como DTO de respuesta
     * @throws org.springframework.web.server.ResponseStatusException 404 si no existe
     */
    LugarFisicoResponse obtenerPorId(Long id);
}

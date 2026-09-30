package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.response.LugarFisicoResponse;
import com.calendario.callapp.callapp_backend.repository.LugarFisicoRepository;
import com.calendario.callapp.callapp_backend.service.LugarFisicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Implementación de {@link com.calendario.callapp.callapp_backend.service.LugarFisicoService}.
 *
 * <p>Delega la conversión entity→DTO al {@code LugarFisicoMapper} (MapStruct).
 * No requiere {@code @Transactional} porque todas las operaciones son de lectura simple
 * gestionadas por JPA en modo auto-commit.</p>
 */
public class LugarFisicoServiceImpl implements LugarFisicoService {

    private final LugarFisicoRepository repository;
    private final com.calendario.callapp.callapp_backend.mapper.LugarFisicoMapper mapper;

    @Override
    public List<LugarFisicoResponse> listarActivos() {
        return mapper.toResponseList(repository.findByActivoTrueOrderByNombreAsc());
    }

    @Override
    public LugarFisicoResponse obtenerPorId(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de lugar físico es obligatorio");
        }
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lugar físico no encontrado"));
    }
}

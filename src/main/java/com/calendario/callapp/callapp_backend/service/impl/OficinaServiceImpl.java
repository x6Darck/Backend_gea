package com.calendario.callapp.callapp_backend.service.impl;

import org.springframework.transaction.annotation.Transactional;

import com.calendario.callapp.callapp_backend.dto.request.OficinaRequest;
import com.calendario.callapp.callapp_backend.dto.response.OficinaResponse;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.repository.OficinaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * CRUD básico de oficinas (unidades académicas/administrativas).
 *
 * <p>Las oficinas son el principal criterio de segmentación de solicitudes:
 * cada usuario pertenece a una oficina y sólo puede gestionar eventos/anuncios
 * de la suya (salvo roles con acceso global). La conversión entity→DTO se realiza
 * en el método privado {@code toResponse} sin depender de MapStruct.</p>
 */
public class OficinaServiceImpl {

    private final OficinaRepository oficinaRepository;

    @Transactional
    public OficinaResponse crear(OficinaRequest request) {
        Oficina oficina = new Oficina();
        oficina.setNombre(request.getNombre());
        oficina.setProgramaAcademico(request.getProgramaAcademico());
        oficina.setDescripcion(request.getDescripcion());
        oficina.setActiva(request.getActiva() == null || request.getActiva());

        return toResponse(oficinaRepository.save(oficina));
    }

    @Transactional(readOnly = true)
    public List<OficinaResponse> listar() {
        return oficinaRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private OficinaResponse toResponse(Oficina oficina) {
        return OficinaResponse.builder()
                .id(oficina.getId())
                .nombre(oficina.getNombre())
                .programaAcademico(oficina.getProgramaAcademico())
                .descripcion(oficina.getDescripcion())
                .activa(oficina.getActiva())
                .build();
    }
}

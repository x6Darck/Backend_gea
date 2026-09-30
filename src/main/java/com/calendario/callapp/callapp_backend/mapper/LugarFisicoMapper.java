package com.calendario.callapp.callapp_backend.mapper;

import com.calendario.callapp.callapp_backend.dto.response.LugarFisicoResponse;
import com.calendario.callapp.callapp_backend.entity.LugarFisico;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
/**
 * MapStruct mapper para conversiones entre {@link com.calendario.callapp.callapp_backend.entity.LugarFisico}
 * y {@link com.calendario.callapp.callapp_backend.dto.response.LugarFisicoResponse}.
 *
 * <p>MapStruct genera la implementación en tiempo de compilación; el bean se inyecta por Spring.</p>
 */
public interface LugarFisicoMapper {
    LugarFisicoResponse toResponse(LugarFisico entity);
    List<LugarFisicoResponse> toResponseList(List<LugarFisico> entities);
}

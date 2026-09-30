package com.calendario.callapp.callapp_backend.mapper;

import com.calendario.callapp.callapp_backend.dto.response.SolicitudAnuncioResponse;
import com.calendario.callapp.callapp_backend.entity.SolicitudAnuncio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
/**
 * MapStruct mapper para {@link com.calendario.callapp.callapp_backend.entity.SolicitudAnuncio}
 * → {@link com.calendario.callapp.callapp_backend.dto.response.SolicitudAnuncioResponse}.
 *
 * <p>Las expresiones condicionales manejan el caso de solicitudes sin {@code usuarioSolicitante}
 * (anuncios creados directamente desde la app con sólo correo de contacto).</p>
 */
public interface SolicitudAnuncioMapper {

    @Mapping(target = "usuarioSolicitanteId", source = "usuarioSolicitante.id")
    @Mapping(target = "usuarioSolicitanteCorreo", expression = "java(entity.getUsuarioSolicitante() != null ? entity.getUsuarioSolicitante().getCorreo() : entity.getCorreoContacto())")
    @Mapping(target = "usuarioSolicitanteNombre", expression = "java(entity.getUsuarioSolicitante() != null ? entity.getUsuarioSolicitante().getNombre() : entity.getResponsableAnuncio())")
    @Mapping(target = "oficinaId", expression = "java(entity.getOficina() != null ? entity.getOficina().getId() : (entity.getUsuarioSolicitante() != null && entity.getUsuarioSolicitante().getRol() != com.calendario.callapp.callapp_backend.entity.Rol.USUARIO_AUTENTICADO_APP && entity.getUsuarioSolicitante().getOficina() != null ? entity.getUsuarioSolicitante().getOficina().getId() : null))")
    @Mapping(target = "oficinaNombre", expression = "java(entity.getOficina() != null ? entity.getOficina().getNombre() : (entity.getUsuarioSolicitante() != null && entity.getUsuarioSolicitante().getRol() != com.calendario.callapp.callapp_backend.entity.Rol.USUARIO_AUTENTICADO_APP && entity.getUsuarioSolicitante().getOficina() != null ? entity.getUsuarioSolicitante().getOficina().getNombre() : \"N/A\"))")
    @Mapping(target = "lugar", expression = "java(entity.getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getNombre).collect(java.util.stream.Collectors.joining(\", \")))")
    @Mapping(target = "lugares", expression = "java(entity.getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getNombre).toList())")
    @Mapping(target = "idsLugaresFisicos", expression = "java(entity.getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getId).toList())")
    @Mapping(target = "visible", expression = "java(false)")
    SolicitudAnuncioResponse toResponse(SolicitudAnuncio entity);

    List<SolicitudAnuncioResponse> toResponseList(List<SolicitudAnuncio> entities);
}

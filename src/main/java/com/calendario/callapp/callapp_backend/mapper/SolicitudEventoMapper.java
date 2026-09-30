package com.calendario.callapp.callapp_backend.mapper;

import com.calendario.callapp.callapp_backend.dto.response.SolicitudEventoResponse;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudEventoParticipanteResponse;
import com.calendario.callapp.callapp_backend.entity.SolicitudEventoParticipante;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
/**
 * MapStruct mapper para {@link com.calendario.callapp.callapp_backend.entity.SolicitudEvento}
 * → {@link com.calendario.callapp.callapp_backend.dto.response.SolicitudEventoResponse}.
 *
 * <p>Las asociaciones de lugares físicos se aplanan en un string separado por coma ({@code lugar})
 * y en listas ({@code lugares}, {@code idsLugaresFisicos}) usando expresiones Java inline.
 * El campo {@code visible} se fuerza a {@code false} porque es un campo de publicación,
 * no de solicitud.</p>
 */
public interface SolicitudEventoMapper {

    @Mapping(target = "tipoEvento", source = "tipoEventoCatalogo.nombre")
    @Mapping(target = "tipoEventoId", source = "tipoEventoCatalogo.id")
    @Mapping(target = "oficinaId", source = "oficina.id")
    @Mapping(target = "oficinaNombre", source = "oficina.nombre")
    @Mapping(target = "usuarioSolicitanteId", source = "usuarioSolicitante.id")
    @Mapping(target = "usuarioSolicitanteCorreo", source = "usuarioSolicitante.correo")
    @Mapping(target = "tipoEventoColorHex", source = "tipoEventoCatalogo.colorHex")
    @Mapping(target = "piezaGraficaUrl", source = "piezaGraficaUrl")
    @Mapping(target = "visible", expression = "java(false)")
    @Mapping(target = "lugar", expression = "java(entity.getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getNombre).collect(java.util.stream.Collectors.joining(\", \")))")
    @Mapping(target = "lugares", expression = "java(entity.getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getNombre).toList())")
    @Mapping(target = "idsLugaresFisicos", expression = "java(entity.getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getId).toList())")
    @Mapping(target = "participantes", source = "participantes")
    SolicitudEventoResponse toResponse(SolicitudEvento entity);

    SolicitudEventoParticipanteResponse toResponse(SolicitudEventoParticipante entity);

    List<SolicitudEventoResponse> toResponseList(List<SolicitudEvento> entities);
}

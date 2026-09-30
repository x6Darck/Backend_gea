package com.calendario.callapp.callapp_backend.mapper;

import com.calendario.callapp.callapp_backend.dto.response.PublicacionEventoResponse;
import com.calendario.callapp.callapp_backend.entity.PublicacionEvento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
/**
 * MapStruct mapper para {@link com.calendario.callapp.callapp_backend.entity.PublicacionEvento}
 * → {@link com.calendario.callapp.callapp_backend.dto.response.PublicacionEventoResponse}.
 *
 * <p>Proyecta campos de la solicitud anidada ({@code solicitudEvento.*}) directamente en el DTO
 * de publicación, evitando que el consumidor tenga que unir ambas entidades por su cuenta.</p>
 */
public interface PublicacionEventoMapper {

    @Mapping(target = "solicitudEventoId", source = "solicitudEvento.id")
    @Mapping(target = "fechaEvento", source = "solicitudEvento.fechaEvento")
    @Mapping(target = "horaInicio", source = "solicitudEvento.horaInicio")
    @Mapping(target = "horaFin", source = "solicitudEvento.horaFin")
    @Mapping(target = "lugar", expression = "java(entity.getSolicitudEvento().getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getNombre).collect(java.util.stream.Collectors.joining(\", \")))")
    @Mapping(target = "lugares", expression = "java(entity.getSolicitudEvento().getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getNombre).toList())")
    @Mapping(target = "idsLugaresFisicos", expression = "java(entity.getSolicitudEvento().getLugaresFisicos().stream().map(com.calendario.callapp.callapp_backend.entity.LugarFisico::getId).toList())")
    @Mapping(target = "linkConexion", source = "solicitudEvento.linkConexion")
    @Mapping(target = "ubicacionExterna", source = "solicitudEvento.ubicacionExterna")
    @Mapping(target = "tipoEventoId", source = "solicitudEvento.tipoEventoCatalogo.id")
    @Mapping(target = "tipoEvento", source = "solicitudEvento.tipoEventoCatalogo.nombre")
    @Mapping(target = "tipoEventoColorHex", source = "solicitudEvento.tipoEventoCatalogo.colorHex")
    @Mapping(target = "oficinaNombre", source = "solicitudEvento.oficina.nombre")
    @Mapping(target = "responsableEvento", source = "solicitudEvento.responsableEvento")
    @Mapping(target = "requiereTransmision", source = "solicitudEvento.requiereTransmision")
    @Mapping(target = "requiereCubrimiento", source = "solicitudEvento.requiereCubrimiento")
    @Mapping(target = "observaciones", source = "solicitudEvento.observaciones")
    @Mapping(target = "esImportante", source = "solicitudEvento.esImportante")
    @Mapping(target = "tipoIngreso", source = "solicitudEvento.tipoIngreso")
    @Mapping(target = "requierePiezaGrafica", source = "solicitudEvento.requierePiezaGrafica")
    @Mapping(target = "usuarioSolicitanteCorreo", source = "solicitudEvento.usuarioSolicitante.correo")
    PublicacionEventoResponse toResponse(PublicacionEvento entity);

    List<PublicacionEventoResponse> toResponseList(List<PublicacionEvento> entities);
}

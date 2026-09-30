package com.calendario.callapp.callapp_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** Resumen ligero de una solicitud (evento o anuncio) para listados en el dashboard. El campo {@code tipo} es {@code "EVENTO"} o {@code "ANUNCIO"}. */
public class SolicitudResumenDTO {
    private Long id;
    private String tipo; // EVENTO o ANUNCIO
    private String titulo;
    private String oficina;
    private String fechaRegistro;
    private String estado;
}

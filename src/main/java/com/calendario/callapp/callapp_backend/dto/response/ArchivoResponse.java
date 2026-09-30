package com.calendario.callapp.callapp_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/** Datos del archivo adjunto tras su subida. El {@code tokenAcceso} es el UUID público para descarga sin autenticación. */
public class ArchivoResponse {

    private Long id;
    private String nombreArchivo;
    private String nombreOriginal;
    private String tokenAcceso;
    private String url;
    private String contentType;
    private long tamano;
}

package com.calendario.callapp.callapp_backend.service;

import com.calendario.callapp.callapp_backend.dto.response.ArchivoResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrato para operaciones de almacenamiento de archivos adjuntos.
 *
 * <p>La implementación actual ({@code ArchivoServiceImpl}) usa el filesystem local
 * bajo el directorio {@code uploads/}. El uso de esta interfaz permite sustituir la
 * implementación por S3, Cloudinary u otro proveedor sin modificar los controladores
 * ni la lógica de negocio que depende de ella.</p>
 */
public interface ArchivoStorageService {

    /**
     * Guarda el archivo recibido en el almacenamiento y persiste sus metadatos.
     *
     * @param archivo archivo multipart enviado por el cliente
     * @return DTO con el ID, token de acceso, nombre original y URL del archivo guardado
     */
    ArchivoResponse guardar(MultipartFile archivo);

    /**
     * Carga un archivo público por su token de acceso y lo devuelve como {@code Resource} descargable.
     * Usado por {@code GET /archivos/public/{filename}} (endpoint sin autenticación).
     *
     * @param tokenAcceso token único del archivo ({@link com.calendario.callapp.callapp_backend.entity.ArchivoAdjunto#getTokenAcceso()})
     * @return el recurso del archivo listo para streamearse al cliente
     * @throws org.springframework.web.server.ResponseStatusException 404 si no existe o no es público
     */
    Resource cargarPublicoComoRecurso(String tokenAcceso);
}

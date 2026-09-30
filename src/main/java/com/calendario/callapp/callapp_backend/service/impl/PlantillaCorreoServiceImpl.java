package com.calendario.callapp.callapp_backend.service.impl;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
/**
 * Renderizador de plantillas HTML para correos electrónicos.
 *
 * <p>Lee archivos {@code .html} desde el classpath ({@code resources/mail-templates/}),
 * reemplaza variables con el patrón {@code ${clave}} y aplica
 * {@link org.springframework.web.util.HtmlUtils#htmlEscape} a cada valor
 * para prevenir inyección de HTML en el cuerpo del correo.</p>
 */
public class PlantillaCorreoServiceImpl {

    public String render(String nombrePlantilla, Map<String, String> variables) {
        try {
            String contenido = new String(
                    new ClassPathResource("mail-templates/" + nombrePlantilla).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            for (Map.Entry<String, String> variable : variables.entrySet()) {
                String valor = variable.getValue() != null ? variable.getValue() : "";
                // Escapar HTML para prevenir inyección en el contenido del correo
                String valorEscapado = HtmlUtils.htmlEscape(valor, StandardCharsets.UTF_8.name());
                contenido = contenido.replace("${" + variable.getKey() + "}", valorEscapado);
            }
            return contenido;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible cargar la plantilla de correo");
        }
    }
}

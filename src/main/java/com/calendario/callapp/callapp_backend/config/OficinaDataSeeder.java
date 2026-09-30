package com.calendario.callapp.callapp_backend.config;

import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.repository.OficinaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
/**
 * Seed idempotente de oficinas mínimas requeridas por el sistema.
 *
 * <p>Crea las oficinas «Comunicaciones» e «Integridad» si no existen al arrancar.
 * Usa {@code findByNombreIgnoreCase} para garantizar idempotencia: si ya existen
 * con cualquier capitalización, no inserta duplicados.</p>
 */
public class OficinaDataSeeder implements CommandLineRunner {

    private final OficinaRepository oficinaRepository;

    @Override
    public void run(String... args) throws Exception {
        crearOficinaSiNoExiste("Comunicaciones", "Oficina de Comunicaciones");
        crearOficinaSiNoExiste("Integridad", "Oficina de Integridad");
    }

    private void crearOficinaSiNoExiste(String nombre, String descripcion) {
        if (oficinaRepository.findByNombreIgnoreCase(nombre).isEmpty()) {
            Oficina oficina = new Oficina();
            oficina.setNombre(nombre);
            oficina.setDescripcion(descripcion);
            oficina.setActiva(true);
            oficina.setFechaCreacion(LocalDateTime.now());
            oficinaRepository.save(oficina);
            System.out.println("Oficina insertada automáticamente: " + nombre);
        }
    }
}

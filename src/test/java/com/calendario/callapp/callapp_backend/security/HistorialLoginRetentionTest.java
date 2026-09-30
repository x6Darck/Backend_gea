package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import com.calendario.callapp.callapp_backend.service.impl.HistorialLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HistorialLoginRetentionTest {

    @Autowired private HistorialLoginService service;
    @Autowired private HistorialLoginRepository repository;

    private HistorialLogin fila(LocalDateTime fecha) {
        HistorialLogin h = new HistorialLogin();
        h.setCorreoIntentado("ret@gea.edu.co");
        h.setExito(true);
        h.setMetodo(AuthProvider.LOCAL);
        h.setFecha(fecha);
        return h;
    }

    @Test
    void purga_borra_lo_mayor_a_90_dias_y_conserva_lo_reciente() {
        repository.deleteAll();
        repository.save(fila(LocalDateTime.now().minusDays(91))); // debe borrarse
        repository.save(fila(LocalDateTime.now().minusDays(10))); // debe quedar

        service.purgarAntiguos();

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getFecha()).isAfter(LocalDateTime.now().minusDays(90));
    }
}

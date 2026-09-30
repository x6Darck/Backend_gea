package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import com.calendario.callapp.callapp_backend.entity.MotivoFalloLogin;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import com.calendario.callapp.callapp_backend.service.impl.HistorialLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NO se anota @Transactional: registrar…() usa REQUIRES_NEW y commitea aparte;
 * un @Transactional de test haría rollback y ocultaría el commit real. Se limpia
 * la tabla a mano al inicio para aislamiento.
 */
@SpringBootTest
@ActiveProfiles("test")
class HistorialLoginServiceTest {

    @Autowired private HistorialLoginService service;
    @Autowired private HistorialLoginRepository repository;

    @Test
    void registrar_exito_persiste_fila_con_exito_true() {
        repository.deleteAll();
        String correo = "exito." + System.nanoTime() + "@gea.edu.co";

        service.registrarExito(correo, 42L, AuthProvider.LOCAL);

        List<HistorialLogin> filas = repository.findAll();
        assertThat(filas).hasSize(1);
        HistorialLogin h = filas.get(0);
        assertThat(h.getCorreoIntentado()).isEqualTo(correo);
        assertThat(h.getIdUsuario()).isEqualTo(42L);
        assertThat(h.getExito()).isTrue();
        assertThat(h.getMetodo()).isEqualTo(AuthProvider.LOCAL);
        assertThat(h.getMotivoFallo()).isNull();
        assertThat(h.getFecha()).isNotNull();
    }

    @Test
    void registrar_fallo_persiste_fila_con_motivo() {
        repository.deleteAll();
        String correo = "fallo." + System.nanoTime() + "@gea.edu.co";

        service.registrarFallo(correo, null, AuthProvider.LOCAL, MotivoFalloLogin.CREDENCIALES_INVALIDAS);

        List<HistorialLogin> filas = repository.findAll();
        assertThat(filas).hasSize(1);
        HistorialLogin h = filas.get(0);
        assertThat(h.getExito()).isFalse();
        assertThat(h.getMotivoFallo()).isEqualTo(MotivoFalloLogin.CREDENCIALES_INVALIDAS);
        assertThat(h.getIdUsuario()).isNull();
    }
}

package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.dto.request.SolicitudAnuncioRequest;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudAnuncioResponse;
import com.calendario.callapp.callapp_backend.entity.*;
import com.calendario.callapp.callapp_backend.repository.*;
import com.calendario.callapp.callapp_backend.service.impl.SolicitudAnuncioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariantes de seguridad de los que depende el historial de solicitudes en
 * la app móvil: un estudiante (rol USUARIO_AUTENTICADO_APP, sin oficina) solo
 * ve las SUYAS, y no puede editar la de otro usuario.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MisSolicitudesAnuncioTest {

    @Autowired private SolicitudAnuncioServiceImpl service;
    @Autowired private SolicitudAnuncioRepository solicitudAnuncioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;

    private Usuario estudianteA;
    private Usuario estudianteB;

    @BeforeEach
    void setUp() {
        RolEntity rol = rolRepository.findByNombre("Usuario Autenticado")
                .orElseGet(() -> {
                    RolEntity r = new RolEntity();
                    r.setNombre("Usuario Autenticado");
                    return rolRepository.save(r);
                });
        estudianteA = nuevoEstudiante(rol, "a");
        estudianteB = nuevoEstudiante(rol, "b");
    }

    private Usuario nuevoEstudiante(RolEntity rol, String suf) {
        Usuario u = new Usuario();
        u.setNombre("Estudiante " + suf);
        u.setCorreo("est." + suf + "." + System.currentTimeMillis() + "@unilibre.edu.co");
        u.setPassword("dummy");
        u.setRolEntity(rol);
        u.setOficina(null); // estudiante: sin oficina
        u.setEstado("ACTIVO");
        u.setAuthProvider(AuthProvider.LOCAL);
        return usuarioRepository.save(u);
    }

    private Authentication authDe(Usuario u) {
        return new UsernamePasswordAuthenticationToken(u.getCorreo(), null, List.of());
    }

    private SolicitudAnuncioRequest requestBasico(String titulo) {
        SolicitudAnuncioRequest req = new SolicitudAnuncioRequest();
        req.setTitulo(titulo);
        req.setDescripcion("Contenido " + titulo);
        req.setCategoria("Informativo");
        req.setFechaInicioPublicacion(LocalDate.now());
        req.setFechaFinPublicacion(LocalDate.now().plusDays(5));
        return req;
    }

    @Test
    void un_estudiante_solo_ve_sus_propias_solicitudes() {
        service.crear(requestBasico("De A - 1"), authDe(estudianteA));
        service.crear(requestBasico("De A - 2"), authDe(estudianteA));
        service.crear(requestBasico("De B - 1"), authDe(estudianteB));

        List<SolicitudAnuncioResponse> deA =
                service.listarPropias(authDe(estudianteA), null, null, null, null);

        assertThat(deA).hasSize(2);
        assertThat(deA).allMatch(r -> r.getUsuarioSolicitanteId().equals(estudianteA.getId()));
    }

    @Test
    void un_estudiante_no_puede_editar_la_solicitud_de_otro() {
        SolicitudAnuncioResponse deB = service.crear(requestBasico("De B"), authDe(estudianteB));

        SolicitudAnuncioRequest edit = requestBasico("Intento de secuestro");

        assertThatThrownBy(() -> service.actualizar(deB.getId(), edit, authDe(estudianteA)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No tienes permiso");
    }
}

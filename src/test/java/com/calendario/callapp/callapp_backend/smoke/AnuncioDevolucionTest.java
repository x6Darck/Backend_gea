package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.dto.request.DevolucionRequest;
import com.calendario.callapp.callapp_backend.dto.request.RechazoRequest;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnuncioDevolucionTest {

    @Autowired private SolicitudAnuncioServiceImpl service;
    @Autowired private SolicitudAnuncioRepository solicitudAnuncioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private RolRepository rolRepository;

    private Usuario solicitante;
    private SolicitudAnuncio solicitud;

    @BeforeEach
    void setUp() {
        RolEntity rol = rolRepository.findByNombre("Oficina")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("Oficina"); return rolRepository.save(r); });

        Oficina oficina = new Oficina();
        oficina.setNombre("Oficina AnunDev " + System.currentTimeMillis());
        oficina.setActiva(true);
        oficina = oficinaRepository.save(oficina);

        solicitante = new Usuario();
        solicitante.setNombre("Solicitante Anuncio");
        solicitante.setCorreo("anun." + System.currentTimeMillis() + "@gea.edu.co");
        solicitante.setPassword("dummy");
        solicitante.setRolEntity(rol);
        solicitante.setOficina(oficina);
        solicitante.setEstado("ACTIVO");
        solicitante.setAuthProvider(AuthProvider.LOCAL);
        solicitante = usuarioRepository.save(solicitante);

        solicitud = new SolicitudAnuncio();
        solicitud.setTitulo("Anuncio a devolver");
        solicitud.setDescripcion("Contenido");
        solicitud.setCategoria("Informativo");
        solicitud.setFechaInicioPublicacion(LocalDate.now());
        solicitud.setFechaFinPublicacion(LocalDate.now().plusDays(10));
        solicitud.setUsuarioSolicitante(solicitante);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud = solicitudAnuncioRepository.save(solicitud);
    }

    private Authentication authDe(Usuario u) {
        return new UsernamePasswordAuthenticationToken(u.getCorreo(), null, List.of());
    }

    @Test
    void devolver_pone_estado_en_revision_y_guarda_observaciones() {
        DevolucionRequest req = new DevolucionRequest();
        req.setObservaciones("Ajustar las fechas de publicación.");

        service.devolver(solicitud.getId(), req, authDe(solicitante));

        SolicitudAnuncio r = solicitudAnuncioRepository.findById(solicitud.getId()).orElseThrow();
        assertThat(r.getEstado()).isEqualTo(EstadoSolicitud.EN_REVISION);
        assertThat(r.getObservacionesRevision()).isEqualTo("Ajustar las fechas de publicación.");
        assertThat(r.getFechaRevision()).isNotNull();
    }

    @Test
    void editar_anuncio_en_revision_lo_regresa_a_pendiente_y_limpia_observaciones() {
        DevolucionRequest dev = new DevolucionRequest();
        dev.setObservaciones("Faltan fechas");
        service.devolver(solicitud.getId(), dev, authDe(solicitante));

        com.calendario.callapp.callapp_backend.dto.request.SolicitudAnuncioRequest edit =
                new com.calendario.callapp.callapp_backend.dto.request.SolicitudAnuncioRequest();
        edit.setTitulo("Anuncio corregido");

        service.actualizar(solicitud.getId(), edit, authDe(solicitante));

        SolicitudAnuncio r = solicitudAnuncioRepository.findById(solicitud.getId()).orElseThrow();
        assertThat(r.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(r.getObservacionesRevision()).isNull();
    }

    @Test
    void editar_anuncio_en_revision_por_admin_dueno_tambien_lo_regresa_a_pendiente() {
        RolEntity rolAdmin = rolRepository.findByNombre("SuperAdmin")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("SuperAdmin"); return rolRepository.save(r); });

        Usuario adminDueno = new Usuario();
        adminDueno.setNombre("Admin Dueño");
        adminDueno.setCorreo("admin.dueno." + System.currentTimeMillis() + "@gea.edu.co");
        adminDueno.setPassword("dummy");
        adminDueno.setRolEntity(rolAdmin);
        adminDueno.setOficina(solicitante.getOficina());
        adminDueno.setEstado("ACTIVO");
        adminDueno.setAuthProvider(AuthProvider.LOCAL);
        adminDueno = usuarioRepository.save(adminDueno);

        SolicitudAnuncio anuncioAdmin = new SolicitudAnuncio();
        anuncioAdmin.setTitulo("Anuncio de admin a devolver");
        anuncioAdmin.setDescripcion("Contenido");
        anuncioAdmin.setCategoria("Informativo");
        anuncioAdmin.setFechaInicioPublicacion(LocalDate.now());
        anuncioAdmin.setFechaFinPublicacion(LocalDate.now().plusDays(10));
        anuncioAdmin.setUsuarioSolicitante(adminDueno);
        anuncioAdmin.setEstado(EstadoSolicitud.PENDIENTE);
        anuncioAdmin = solicitudAnuncioRepository.save(anuncioAdmin);

        DevolucionRequest dev = new DevolucionRequest();
        dev.setObservaciones("Ajustar categoría");
        service.devolver(anuncioAdmin.getId(), dev, authDe(adminDueno));

        com.calendario.callapp.callapp_backend.dto.request.SolicitudAnuncioRequest edit =
                new com.calendario.callapp.callapp_backend.dto.request.SolicitudAnuncioRequest();
        edit.setTitulo("Anuncio de admin corregido");

        service.actualizar(anuncioAdmin.getId(), edit, authDe(adminDueno));

        SolicitudAnuncio r = solicitudAnuncioRepository.findById(anuncioAdmin.getId()).orElseThrow();
        assertThat(r.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(r.getObservacionesRevision()).isNull();
    }

    @Test
    void editar_anuncio_rechazado_por_el_dueno_lanza_excepcion_y_no_se_puede_reenviar() {
        RechazoRequest rechazo = new RechazoRequest();
        rechazo.setMotivo("No cumple los lineamientos.");
        service.rechazar(solicitud.getId(), rechazo, authDe(solicitante));

        com.calendario.callapp.callapp_backend.dto.request.SolicitudAnuncioRequest edit =
                new com.calendario.callapp.callapp_backend.dto.request.SolicitudAnuncioRequest();
        edit.setTitulo("Intento de reenvío");

        assertThatThrownBy(() -> service.actualizar(solicitud.getId(), edit, authDe(solicitante)))
                .isInstanceOf(ResponseStatusException.class);

        SolicitudAnuncio r = solicitudAnuncioRepository.findById(solicitud.getId()).orElseThrow();
        assertThat(r.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(r.getTitulo()).isEqualTo("Anuncio a devolver");
        assertThat(r.getMotivoRechazo()).isEqualTo("No cumple los lineamientos.");
    }
}

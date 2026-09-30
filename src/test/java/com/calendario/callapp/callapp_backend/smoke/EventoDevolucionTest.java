package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.dto.request.DevolucionRequest;
import com.calendario.callapp.callapp_backend.dto.request.RechazoRequest;
import com.calendario.callapp.callapp_backend.entity.*;
import com.calendario.callapp.callapp_backend.repository.*;
import com.calendario.callapp.callapp_backend.service.impl.SolicitudEventoServiceImpl;
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
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventoDevolucionTest {

    @Autowired private SolicitudEventoServiceImpl service;
    @Autowired private SolicitudEventoRepository solicitudEventoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private TipoEventoCatalogoRepository tipoEventoCatalogoRepository;

    private Usuario revisor;
    private SolicitudEvento solicitud;

    @BeforeEach
    void setUp() {
        RolEntity rol = rolRepository.findByNombre("Comunicaciones")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("Comunicaciones"); return rolRepository.save(r); });

        Oficina oficina = new Oficina();
        oficina.setNombre("Oficina Dev " + System.currentTimeMillis());
        oficina.setActiva(true);
        oficina = oficinaRepository.save(oficina);

        revisor = new Usuario();
        revisor.setNombre("Revisor Test");
        revisor.setCorreo("revisor." + System.currentTimeMillis() + "@gea.edu.co");
        revisor.setPassword("dummy");
        revisor.setRolEntity(rol);
        revisor.setOficina(oficina);
        revisor.setEstado("ACTIVO");
        revisor.setAuthProvider(AuthProvider.LOCAL);
        revisor = usuarioRepository.save(revisor);

        TipoEventoCatalogo tipo = new TipoEventoCatalogo();
        tipo.setNombre("Tipo Dev " + System.currentTimeMillis());
        tipo.setColorHex("#CE1126");
        tipo.setActivo(true);
        tipo = tipoEventoCatalogoRepository.save(tipo);

        solicitud = new SolicitudEvento();
        solicitud.setNombreEvento("Evento a devolver");
        solicitud.setFechaEvento(LocalDate.now().plusDays(5));
        solicitud.setHoraInicio(LocalTime.of(9, 0));
        solicitud.setHoraFin(LocalTime.of(10, 0));
        solicitud.setOficina(oficina);
        solicitud.setUsuarioSolicitante(revisor);
        solicitud.setTipoEventoCatalogo(tipo);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud = solicitudEventoRepository.save(solicitud);
    }

    private Authentication authDe(Usuario u) {
        return new UsernamePasswordAuthenticationToken(u.getCorreo(), null, List.of());
    }

    @Test
    void devolver_pone_estado_en_revision_y_guarda_observaciones() {
        DevolucionRequest req = new DevolucionRequest();
        req.setObservaciones("Corregir la fecha y agregar el lugar.");

        service.devolver(solicitud.getId(), req, authDe(revisor));

        SolicitudEvento actualizada = solicitudEventoRepository.findById(solicitud.getId()).orElseThrow();
        assertThat(actualizada.getEstado()).isEqualTo(EstadoSolicitud.EN_REVISION);
        assertThat(actualizada.getObservacionesRevision()).isEqualTo("Corregir la fecha y agregar el lugar.");
        assertThat(actualizada.getUsuarioRevisor()).isNotNull();
        assertThat(actualizada.getFechaRevision()).isNotNull();
    }

    @Test
    void editar_una_solicitud_en_revision_la_regresa_a_pendiente_y_limpia_observaciones() {
        // dejarla EN_REVISION
        DevolucionRequest dev = new DevolucionRequest();
        dev.setObservaciones("Falta el lugar");
        service.devolver(solicitud.getId(), dev, authDe(revisor));

        // el solicitante edita y reenvía
        com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoRequest edit =
                new com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoRequest();
        edit.setNombreEvento("Evento corregido");
        edit.setFechaEvento(LocalDate.now().plusDays(6));
        edit.setHoraInicio(LocalTime.of(9, 0));
        edit.setHoraFin(LocalTime.of(11, 0));
        edit.setTipoEvento(solicitud.getTipoEventoCatalogo().getNombre());

        service.actualizarPropia(solicitud.getId(), edit, authDe(revisor));

        SolicitudEvento r = solicitudEventoRepository.findById(solicitud.getId()).orElseThrow();
        assertThat(r.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(r.getObservacionesRevision()).isNull();
    }

    @Test
    void editar_evento_rechazado_por_oficina_lanza_excepcion_y_no_se_puede_reenviar() {
        final RolEntity rolOficina = rolRepository.findByNombre("Oficina")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("Oficina"); return rolRepository.save(r); });

        Usuario solicitanteOficina = new Usuario();
        solicitanteOficina.setNombre("Solicitante Oficina");
        solicitanteOficina.setCorreo("oficina." + System.currentTimeMillis() + "@gea.edu.co");
        solicitanteOficina.setPassword("dummy");
        solicitanteOficina.setRolEntity(rolOficina);
        solicitanteOficina.setOficina(solicitud.getOficina());
        solicitanteOficina.setEstado("ACTIVO");
        solicitanteOficina.setAuthProvider(AuthProvider.LOCAL);
        final Usuario solicitanteGuardado = usuarioRepository.save(solicitanteOficina);

        RechazoRequest rechazo = new RechazoRequest();
        rechazo.setMotivo("No cumple los lineamientos.");
        service.rechazar(solicitud.getId(), rechazo, authDe(revisor));

        com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoRequest edit =
                new com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoRequest();
        edit.setNombreEvento("Intento de reenvío");
        edit.setFechaEvento(LocalDate.now().plusDays(6));
        edit.setHoraInicio(LocalTime.of(9, 0));
        edit.setHoraFin(LocalTime.of(11, 0));
        edit.setTipoEvento(solicitud.getTipoEventoCatalogo().getNombre());

        assertThatThrownBy(() -> service.actualizarPropia(solicitud.getId(), edit, authDe(solicitanteGuardado)))
                .isInstanceOf(ResponseStatusException.class);

        SolicitudEvento r = solicitudEventoRepository.findById(solicitud.getId()).orElseThrow();
        assertThat(r.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(r.getNombreEvento()).isEqualTo("Evento a devolver");
        assertThat(r.getMotivoRechazo()).isEqualTo("No cumple los lineamientos.");
    }
}

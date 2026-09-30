package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.dto.request.RechazoRequest;
import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.LugarFisico;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import com.calendario.callapp.callapp_backend.entity.TipoEventoCatalogo;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.LugarFisicoRepository;
import com.calendario.callapp.callapp_backend.repository.OficinaRepository;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.SolicitudEventoRepository;
import com.calendario.callapp.callapp_backend.repository.TipoEventoCatalogoRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.service.impl.SolicitudEventoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Cubre la lógica añadida en la remediación de auditoría:
 * - Validación de estado previo en aprobar/rechazar
 * - Detección de conflictos que ahora incluye EN_REVISION
 * - Guarda de propiedad al eliminar una serie
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditoriaRemediacionTest {

    @Autowired private SolicitudEventoServiceImpl service;
    @Autowired private SolicitudEventoRepository solicitudEventoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private TipoEventoCatalogoRepository tipoEventoCatalogoRepository;
    @Autowired private LugarFisicoRepository lugarFisicoRepository;

    private Usuario revisor;     // Comunicaciones
    private Usuario duenoOficina; // OFICINA, dueño de la solicitud
    private Oficina oficina;
    private TipoEventoCatalogo tipo;

    @BeforeEach
    void setUp() {
        long t = System.nanoTime();

        RolEntity rolComs = rolRepository.findByNombre("Comunicaciones")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("Comunicaciones"); return rolRepository.save(r); });
        RolEntity rolOficina = rolRepository.findByNombre("OFICINA")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("OFICINA"); return rolRepository.save(r); });

        oficina = new Oficina();
        oficina.setNombre("Oficina Rem " + t);
        oficina.setActiva(true);
        oficina = oficinaRepository.save(oficina);

        revisor = new Usuario();
        revisor.setNombre("Revisor Rem");
        revisor.setCorreo("revisor.rem." + t + "@gea.edu.co");
        revisor.setPassword("dummy");
        revisor.setRolEntity(rolComs);
        revisor.setOficina(oficina);
        revisor.setEstado("ACTIVO");
        revisor.setAuthProvider(AuthProvider.LOCAL);
        revisor = usuarioRepository.save(revisor);

        duenoOficina = new Usuario();
        duenoOficina.setNombre("Dueno Rem");
        duenoOficina.setCorreo("dueno.rem." + t + "@gea.edu.co");
        duenoOficina.setPassword("dummy");
        duenoOficina.setRolEntity(rolOficina);
        duenoOficina.setOficina(oficina);
        duenoOficina.setEstado("ACTIVO");
        duenoOficina.setAuthProvider(AuthProvider.LOCAL);
        duenoOficina = usuarioRepository.save(duenoOficina);

        tipo = new TipoEventoCatalogo();
        tipo.setNombre("Tipo Rem " + t);
        tipo.setColorHex("#CE1126");
        tipo.setActivo(true);
        tipo = tipoEventoCatalogoRepository.save(tipo);
    }

    private Authentication authDe(Usuario u) {
        return new UsernamePasswordAuthenticationToken(u.getCorreo(), null, List.of());
    }

    private SolicitudEvento nuevaSolicitud(EstadoSolicitud estado, Usuario dueno) {
        SolicitudEvento s = new SolicitudEvento();
        s.setNombreEvento("Evento Rem");
        s.setFechaEvento(LocalDate.now().plusDays(7));
        s.setHoraInicio(LocalTime.of(9, 0));
        s.setHoraFin(LocalTime.of(11, 0));
        s.setOficina(oficina);
        s.setUsuarioSolicitante(dueno);
        s.setTipoEventoCatalogo(tipo);
        s.setEstado(estado);
        return solicitudEventoRepository.save(s);
    }

    @Test
    void aprobar_rechaza_una_solicitud_que_no_esta_pendiente() {
        SolicitudEvento yaAprobada = nuevaSolicitud(EstadoSolicitud.APROBADA, duenoOficina);

        Throwable t = catchThrowable(() -> service.aprobar(yaAprobada.getId(), authDe(revisor)));

        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // No cambió el estado
        assertThat(solicitudEventoRepository.findById(yaAprobada.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitud.APROBADA);
    }

    @Test
    void rechazar_rechaza_una_solicitud_que_no_esta_pendiente() {
        SolicitudEvento yaPublicada = nuevaSolicitud(EstadoSolicitud.PUBLICADA, duenoOficina);

        RechazoRequest req = new RechazoRequest();
        req.setMotivo("Motivo de prueba");

        Throwable t = catchThrowable(() -> service.rechazar(yaPublicada.getId(), req, authDe(revisor)));

        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(solicitudEventoRepository.findById(yaPublicada.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitud.PUBLICADA);
    }

    @Test
    void deteccion_de_conflictos_incluye_eventos_en_revision() {
        LugarFisico lugar = new LugarFisico();
        lugar.setNombre("Auditorio Rem " + System.nanoTime());
        lugar.setActivo(true);
        lugar = lugarFisicoRepository.save(lugar);

        SolicitudEvento enRevision = nuevaSolicitud(EstadoSolicitud.EN_REVISION, duenoOficina);
        enRevision.getLugaresFisicos().add(lugar);
        enRevision = solicitudEventoRepository.save(enRevision);

        // Otro evento en el mismo lugar/fecha con horario solapado (10-12 vs 9-11)
        List<SolicitudEvento> conflictos = solicitudEventoRepository.findConflictsBulk(
                List.of(enRevision.getFechaEvento()),
                List.of(lugar.getId()),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null);

        assertThat(conflictos).extracting(SolicitudEvento::getId).contains(enRevision.getId());
    }

    @Test
    void eliminar_serie_falla_si_el_usuario_no_es_dueno_ni_admin() {
        // Maestro de serie creado por 'duenoOficina'
        String grupo = "grupo-rem-" + System.nanoTime();
        SolicitudEvento maestro = nuevaSolicitud(EstadoSolicitud.PENDIENTE, duenoOficina);
        maestro.setIdGrupoRecurrencia(grupo);
        maestro.setEsPrincipal(true);
        solicitudEventoRepository.save(maestro);

        // Otro usuario de la MISMA oficina (pasa validarAccesoOficina) pero NO es el dueño
        Usuario otro = new Usuario();
        otro.setNombre("Otro Oficina");
        otro.setCorreo("otro.rem." + System.nanoTime() + "@gea.edu.co");
        otro.setPassword("dummy");
        otro.setRolEntity(rolRepository.findByNombre("OFICINA").orElseThrow());
        otro.setOficina(oficina);
        otro.setEstado("ACTIVO");
        otro.setAuthProvider(AuthProvider.LOCAL);
        otro = usuarioRepository.save(otro);
        final Authentication authOtro = authDe(otro);

        Throwable t = catchThrowable(() -> service.eliminarSerie(grupo, authOtro));

        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        // La serie sigue existiendo
        assertThat(solicitudEventoRepository.findAllByIdGrupoRecurrencia(grupo)).isNotEmpty();
    }
}

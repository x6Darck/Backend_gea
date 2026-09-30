package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import com.calendario.callapp.callapp_backend.entity.TipoEventoCatalogo;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.OficinaRepository;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.SolicitudEventoRepository;
import com.calendario.callapp.callapp_backend.repository.TipoEventoCatalogoRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventFlowTest {

    @Autowired private SolicitudEventoRepository solicitudEventoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private TipoEventoCatalogoRepository tipoEventoCatalogoRepository;

    private Usuario testUser;
    private Oficina testOficina;
    private TipoEventoCatalogo testTipo;

    @BeforeEach
    void setUp() {
        RolEntity rol = rolRepository.findByNombre("OFICINA")
                .orElseGet(() -> {
                    RolEntity r = new RolEntity();
                    r.setNombre("OFICINA");
                    return rolRepository.save(r);
                });

        testOficina = new Oficina();
        testOficina.setNombre("Oficina Test " + System.currentTimeMillis());
        testOficina.setActiva(true);
        testOficina = oficinaRepository.save(testOficina);

        testUser = new Usuario();
        testUser.setNombre("Test User Evento");
        testUser.setCorreo("evento.test." + System.currentTimeMillis() + "@gea.edu.co");
        testUser.setPassword("dummy");
        testUser.setRolEntity(rol);
        testUser.setOficina(testOficina);
        testUser.setEstado("ACTIVO");
        testUser.setAuthProvider(AuthProvider.LOCAL);
        testUser = usuarioRepository.save(testUser);

        testTipo = new TipoEventoCatalogo();
        testTipo.setNombre("Conferencia Test " + System.currentTimeMillis());
        testTipo.setColorHex("#CE1126");
        testTipo.setActivo(true);
        testTipo = tipoEventoCatalogoRepository.save(testTipo);
    }

    @Test
    void solicitud_creada_tiene_estado_pendiente() {
        SolicitudEvento solicitud = buildSolicitud();
        SolicitudEvento saved = solicitudEventoRepository.save(solicitud);

        SolicitudEvento found = solicitudEventoRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void solicitud_pendiente_puede_cambiar_a_aprobada() {
        SolicitudEvento saved = solicitudEventoRepository.save(buildSolicitud());

        saved.setEstado(EstadoSolicitud.APROBADA);
        solicitudEventoRepository.save(saved);

        assertThat(solicitudEventoRepository.findById(saved.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitud.APROBADA);
    }

    @Test
    void solicitud_aprobada_puede_cambiar_a_publicada() {
        SolicitudEvento s = buildSolicitud();
        s.setEstado(EstadoSolicitud.APROBADA);
        SolicitudEvento saved = solicitudEventoRepository.save(s);

        saved.setEstado(EstadoSolicitud.PUBLICADA);
        solicitudEventoRepository.save(saved);

        assertThat(solicitudEventoRepository.findById(saved.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitud.PUBLICADA);
    }

    private SolicitudEvento buildSolicitud() {
        SolicitudEvento s = new SolicitudEvento();
        s.setNombreEvento("Evento de Prueba");
        s.setDescripcionEvento("Descripcion de prueba");
        s.setFechaEvento(LocalDate.now().plusDays(7));
        s.setHoraInicio(LocalTime.of(9, 0));
        s.setHoraFin(LocalTime.of(11, 0));
        s.setOficina(testOficina);
        s.setUsuarioSolicitante(testUser);
        s.setTipoEventoCatalogo(testTipo);
        s.setEstado(EstadoSolicitud.PENDIENTE);
        return s;
    }
}

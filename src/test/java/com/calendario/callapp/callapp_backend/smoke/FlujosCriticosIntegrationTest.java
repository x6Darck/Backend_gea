package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.dto.request.DevolucionRequest;
import com.calendario.callapp.callapp_backend.dto.request.PublicacionEventoRequest;
import com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoRequest;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudEventoResponse;
import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.FrecuenciaRecurrencia;
import com.calendario.callapp.callapp_backend.entity.LugarFisico;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import com.calendario.callapp.callapp_backend.entity.TipoEventoCatalogo;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.LugarFisicoRepository;
import com.calendario.callapp.callapp_backend.repository.OficinaRepository;
import com.calendario.callapp.callapp_backend.repository.PublicacionEventoRepository;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.SolicitudEventoRepository;
import com.calendario.callapp.callapp_backend.repository.TipoEventoCatalogoRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.service.impl.NotificacionServiceImpl;
import com.calendario.callapp.callapp_backend.service.impl.SolicitudEventoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QA automatizado de los flujos críticos end-to-end a través del servicio real
 * (contexto Spring completo, H2). Las notificaciones se mockean para no tocar SMTP/push.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FlujosCriticosIntegrationTest {

    @Autowired private SolicitudEventoServiceImpl service;
    @Autowired private SolicitudEventoRepository solicitudEventoRepository;
    @Autowired private PublicacionEventoRepository publicacionEventoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private TipoEventoCatalogoRepository tipoEventoCatalogoRepository;
    @Autowired private LugarFisicoRepository lugarFisicoRepository;

    @MockBean private NotificacionServiceImpl notificacionService; // no-op: evita SMTP/push en tests

    private Usuario creador;  // OFICINA
    private Usuario revisor;  // Comunicaciones
    private Oficina oficina;
    private TipoEventoCatalogo tipo;

    @BeforeEach
    void setUp() {
        long t = System.nanoTime();

        RolEntity rolOficina = rolRepository.findByNombre("OFICINA")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("OFICINA"); return rolRepository.save(r); });
        RolEntity rolComs = rolRepository.findByNombre("Comunicaciones")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("Comunicaciones"); return rolRepository.save(r); });

        oficina = new Oficina();
        oficina.setNombre("Oficina Flujo " + t);
        oficina.setActiva(true);
        oficina = oficinaRepository.save(oficina);

        creador = guardarUsuario("creador.flujo." + t + "@gea.edu.co", rolOficina);
        revisor = guardarUsuario("revisor.flujo." + t + "@gea.edu.co", rolComs);

        tipo = new TipoEventoCatalogo();
        tipo.setNombre("Tipo Flujo " + t);
        tipo.setColorHex("#CE1126");
        tipo.setActivo(true);
        tipo = tipoEventoCatalogoRepository.save(tipo);
    }

    private Usuario guardarUsuario(String correo, RolEntity rol) {
        Usuario u = new Usuario();
        u.setNombre("Usuario " + correo);
        u.setCorreo(correo);
        u.setPassword("dummy");
        u.setRolEntity(rol);
        u.setOficina(oficina);
        u.setEstado("ACTIVO");
        u.setAuthProvider(AuthProvider.LOCAL);
        return usuarioRepository.save(u);
    }

    private Authentication authDe(Usuario u) {
        return new UsernamePasswordAuthenticationToken(u.getCorreo(), null, List.of());
    }

    private SolicitudEventoRequest baseRequest(String nombre) {
        SolicitudEventoRequest r = new SolicitudEventoRequest();
        r.setNombreEvento(nombre);
        r.setDescripcionEvento("Descripción de prueba");
        r.setFechaEvento(LocalDate.now().plusDays(7));
        r.setHoraInicio(LocalTime.of(9, 0));
        r.setHoraFin(LocalTime.of(11, 0));
        r.setTipoEvento(tipo.getNombre());
        return r;
    }

    @Test
    void ciclo_completo_crear_aprobar_publicar() {
        // CREAR
        SolicitudEventoResponse creada = service.crear(baseRequest("Evento Ciclo"), authDe(creador));
        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);

        // APROBAR
        service.aprobar(creada.getId(), authDe(revisor));
        assertThat(solicitudEventoRepository.findById(creada.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitud.APROBADA);

        // PUBLICAR
        PublicacionEventoRequest pub = new PublicacionEventoRequest();
        pub.setTituloVisible("Evento Ciclo (Público)");
        pub.setDescripcionVisible("Visible al público");
        pub.setFechaPublicacion(LocalDateTime.now());
        service.publicar(creada.getId(), pub, authDe(revisor));

        SolicitudEvento publicada = solicitudEventoRepository.findById(creada.getId()).orElseThrow();
        assertThat(publicada.getEstado()).isEqualTo(EstadoSolicitud.PUBLICADA);
        assertThat(publicacionEventoRepository.findBySolicitudEventoId(creada.getId()))
                .isPresent()
                .get()
                .satisfies(p -> {
                    assertThat(p.getVisible()).isTrue();
                    assertThat(p.getTituloVisible()).isEqualTo("Evento Ciclo (Público)");
                });
    }

    @Test
    void editar_serie_completa_propaga_a_todas_las_iteraciones() {
        // CREAR SERIE semanal (maestro + iteraciones)
        SolicitudEventoRequest req = baseRequest("Serie Original");
        req.setFrecuenciaRecurrencia(FrecuenciaRecurrencia.SEMANAL);
        req.setFechaFinRecurrencia(LocalDate.now().plusDays(28));
        SolicitudEventoResponse maestro = service.crear(req, authDe(creador));

        String idGrupo = maestro.getIdGrupoRecurrencia();
        assertThat(idGrupo).isNotBlank();
        List<SolicitudEvento> serieAntes = solicitudEventoRepository.findAllByIdGrupoRecurrencia(idGrupo);
        assertThat(serieAntes).hasSizeGreaterThan(1); // maestro + al menos una iteración

        // EDITAR TODA LA SERIE (el bug original: los cambios no se propagaban)
        SolicitudEventoRequest edit = baseRequest("Serie Editada");
        edit.setHoraInicio(LocalTime.of(14, 0));
        edit.setHoraFin(LocalTime.of(16, 0));
        service.actualizarSerie(idGrupo, edit, authDe(creador));

        // Verificar que TODAS las instancias recibieron los cambios
        List<SolicitudEvento> serieDespues = solicitudEventoRepository.findAllByIdGrupoRecurrencia(idGrupo);
        assertThat(serieDespues).isNotEmpty();
        assertThat(serieDespues).allSatisfy(s -> {
            assertThat(s.getNombreEvento()).isEqualTo("Serie Editada");
            assertThat(s.getHoraInicio()).isEqualTo(LocalTime.of(14, 0));
            assertThat(s.getHoraFin()).isEqualTo(LocalTime.of(16, 0));
        });
    }

    @Test
    void devolver_maestro_de_serie_propaga_en_revision_a_todas_las_iteraciones() {
        SolicitudEventoRequest req = baseRequest("Serie a Devolver");
        req.setFrecuenciaRecurrencia(FrecuenciaRecurrencia.SEMANAL);
        req.setFechaFinRecurrencia(LocalDate.now().plusDays(28));
        SolicitudEventoResponse maestro = service.crear(req, authDe(creador));
        String idGrupo = maestro.getIdGrupoRecurrencia();

        DevolucionRequest dev = new DevolucionRequest();
        dev.setObservaciones("Corregir toda la serie");
        service.devolver(maestro.getId(), dev, authDe(revisor));

        List<SolicitudEvento> serie = solicitudEventoRepository.findAllByIdGrupoRecurrencia(idGrupo);
        assertThat(serie).isNotEmpty();
        assertThat(serie).allSatisfy(s ->
                assertThat(s.getEstado()).isEqualTo(EstadoSolicitud.EN_REVISION));
    }

    @Test
    void toggle_visibilidad_serie_propaga_a_todas_las_instancias() {
        SolicitudEventoRequest req = baseRequest("Serie Visibilidad");
        req.setFrecuenciaRecurrencia(FrecuenciaRecurrencia.SEMANAL);
        req.setFechaFinRecurrencia(LocalDate.now().plusDays(28));
        SolicitudEventoResponse maestro = service.crear(req, authDe(creador));
        String idGrupo = maestro.getIdGrupoRecurrencia();

        service.aprobarSerie(idGrupo, authDe(revisor));

        PublicacionEventoRequest pub = new PublicacionEventoRequest();
        pub.setTituloVisible("Serie Visibilidad (Público)");
        pub.setDescripcionVisible("Visible al público");
        pub.setFechaPublicacion(LocalDateTime.now());
        service.publicarSerie(idGrupo, pub, authDe(revisor));

        List<SolicitudEvento> serie = solicitudEventoRepository.findAllByIdGrupoRecurrencia(idGrupo);
        assertThat(serie).hasSizeGreaterThan(1);
        List<Long> ids = serie.stream().map(SolicitudEvento::getId).toList();
        assertThat(publicacionEventoRepository.findBySolicitudEventoIdIn(ids)).hasSameSizeAs(serie);

        service.toggleVisibilidadSerie(idGrupo, false);

        assertThat(publicacionEventoRepository.findBySolicitudEventoIdIn(ids))
                .hasSameSizeAs(serie)
                .allSatisfy(p -> assertThat(p.getVisible()).isFalse());
    }

    @Test
    void actualizar_serie_detecta_conflicto_en_una_instancia_secundaria() {
        LugarFisico auditorio = new LugarFisico();
        auditorio.setNombre("Auditorio Conflicto " + System.nanoTime());
        auditorio.setActivo(true);
        auditorio = lugarFisicoRepository.save(auditorio);

        SolicitudEventoRequest req = baseRequest("Serie Con Posible Conflicto");
        req.setIdsLugaresFisicos(List.of(auditorio.getId()));
        req.setFrecuenciaRecurrencia(FrecuenciaRecurrencia.SEMANAL);
        req.setFechaFinRecurrencia(LocalDate.now().plusDays(28));
        SolicitudEventoResponse maestro = service.crear(req, authDe(creador));
        String idGrupo = maestro.getIdGrupoRecurrencia();

        List<SolicitudEvento> serie = solicitudEventoRepository.findAllByIdGrupoRecurrencia(idGrupo).stream()
                .sorted(Comparator.comparing(SolicitudEvento::getFechaEvento))
                .toList();
        assertThat(serie).hasSizeGreaterThan(1);
        // La fecha de una instancia SECUNDARIA (no la del maestro), para probar que el
        // conflicto se detecta en toda la serie y no solo en la primera instancia.
        LocalDate fechaInstanciaSecundaria = serie.get(1).getFechaEvento();

        SolicitudEventoRequest colisionReq = baseRequest("Evento Que Choca");
        colisionReq.setIdsLugaresFisicos(List.of(auditorio.getId()));
        colisionReq.setFechaEvento(fechaInstanciaSecundaria);
        colisionReq.setHoraInicio(LocalTime.of(10, 0));
        colisionReq.setHoraFin(LocalTime.of(12, 0));
        SolicitudEventoResponse colision = service.crear(colisionReq, authDe(creador));
        service.aprobar(colision.getId(), authDe(revisor));

        SolicitudEventoRequest edit = baseRequest("Serie Con Posible Conflicto (editada)");
        edit.setHoraInicio(LocalTime.of(9, 0));
        edit.setHoraFin(LocalTime.of(11, 0));

        assertThatThrownBy(() -> service.actualizarSerie(idGrupo, edit, authDe(creador)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }
}

package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.dto.request.PublicacionAnuncioRequest;
import com.calendario.callapp.callapp_backend.dto.request.PublicacionEventoRequest;
import com.calendario.callapp.callapp_backend.dto.request.SolicitudAnuncioRequest;
import com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoRequest;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudAnuncioResponse;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudEventoResponse;
import com.calendario.callapp.callapp_backend.entity.*;
import com.calendario.callapp.callapp_backend.repository.*;
import com.calendario.callapp.callapp_backend.service.impl.NotificacionServiceImpl;
import com.calendario.callapp.callapp_backend.service.impl.SolicitudAnuncioServiceImpl;
import com.calendario.callapp.callapp_backend.service.impl.SolicitudEventoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confirma que crear y publicar eventos/anuncios CON lugares físicos asociados
 * sigue funcionando end-to-end después del fix de N+1 (JOIN FETCH solo en
 * asociaciones to-one + {@code @BatchSize} en {@code lugaresFisicos} — ver
 * {@link PublicacionNMasUnoTest}). Ejercita tanto la escritura (crear con
 * {@code idsLugaresFisicos}) como la lectura por los métodos de listado ya
 * modificados ({@code getAllVisibleOptimized} / {@code findByVisibleTrueOrderByFechaPublicacionDesc}),
 * para confirmar que los lugares sobreviven el ciclo completo crear→aprobar→publicar→listar.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CreacionYPublicacionConLugaresTest {

    @Autowired private SolicitudEventoServiceImpl eventoService;
    @Autowired private SolicitudAnuncioServiceImpl anuncioService;
    @Autowired private PublicacionEventoRepository publicacionEventoRepository;
    @Autowired private PublicacionAnuncioRepository publicacionAnuncioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private LugarFisicoRepository lugarFisicoRepository;
    @Autowired private TipoEventoCatalogoRepository tipoEventoCatalogoRepository;

    @MockBean private NotificacionServiceImpl notificacionService; // no-op: evita SMTP/push en tests

    private Usuario creador;
    private Usuario revisor;
    private Oficina oficina;
    private TipoEventoCatalogo tipo;
    private LugarFisico lugar1;
    private LugarFisico lugar2;

    @BeforeEach
    void setUp() {
        long t = System.nanoTime();

        RolEntity rolOficina = rolRepository.findByNombre("OFICINA")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("OFICINA"); return rolRepository.save(r); });
        RolEntity rolComs = rolRepository.findByNombre("Comunicaciones")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("Comunicaciones"); return rolRepository.save(r); });

        oficina = new Oficina();
        oficina.setNombre("Oficina CreacionLugares " + t);
        oficina.setActiva(true);
        oficina = oficinaRepository.save(oficina);

        creador = guardarUsuario("creador.creacion." + t + "@gea.edu.co", rolOficina);
        revisor = guardarUsuario("revisor.creacion." + t + "@gea.edu.co", rolComs);

        tipo = new TipoEventoCatalogo();
        tipo.setNombre("Tipo CreacionLugares " + t);
        tipo.setColorHex("#CE1126");
        tipo.setActivo(true);
        tipo = tipoEventoCatalogoRepository.save(tipo);

        lugar1 = new LugarFisico();
        lugar1.setNombre("Lugar Creacion A " + t);
        lugar1.setActivo(true);
        lugar1 = lugarFisicoRepository.save(lugar1);

        lugar2 = new LugarFisico();
        lugar2.setNombre("Lugar Creacion B " + t);
        lugar2.setActivo(true);
        lugar2 = lugarFisicoRepository.save(lugar2);
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

    @Test
    void crear_evento_con_lugares_se_publica_y_se_lista_correctamente() {
        SolicitudEventoRequest req = new SolicitudEventoRequest();
        req.setNombreEvento("Evento Con Lugares");
        req.setDescripcionEvento("Descripcion de prueba");
        req.setFechaEvento(LocalDate.now().plusDays(7));
        req.setHoraInicio(LocalTime.of(9, 0));
        req.setHoraFin(LocalTime.of(11, 0));
        req.setTipoEvento(tipo.getNombre());
        req.setIdsLugaresFisicos(List.of(lugar1.getId(), lugar2.getId()));

        SolicitudEventoResponse creado = eventoService.crear(req, authDe(creador));
        assertThat(creado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(creado.getIdsLugaresFisicos()).containsExactlyInAnyOrder(lugar1.getId(), lugar2.getId());

        eventoService.aprobar(creado.getId(), authDe(revisor));

        PublicacionEventoRequest pub = new PublicacionEventoRequest();
        pub.setTituloVisible("Evento Con Lugares (Público)");
        pub.setDescripcionVisible("Visible al público");
        pub.setFechaPublicacion(LocalDateTime.now());
        eventoService.publicar(creado.getId(), pub, authDe(revisor));

        // Lee por el método de listado que se modificó con el fix de N+1
        // (JOIN FETCH to-one + @BatchSize en lugaresFisicos).
        List<PublicacionEvento> visibles = publicacionEventoRepository.getAllVisibleOptimized(PageRequest.of(0, 300));
        PublicacionEvento publicado = visibles.stream()
                .filter(p -> p.getSolicitudEvento().getId().equals(creado.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("El evento publicado no aparece en el listado visible"));

        assertThat(publicado.getVisible()).isTrue();
        assertThat(publicado.getSolicitudEvento().getLugaresFisicos())
                .extracting(LugarFisico::getId)
                .containsExactlyInAnyOrder(lugar1.getId(), lugar2.getId());
    }

    @Test
    void crear_anuncio_con_lugares_se_publica_y_se_lista_correctamente() {
        SolicitudAnuncioRequest req = new SolicitudAnuncioRequest();
        req.setTitulo("Anuncio Con Lugares");
        req.setDescripcion("Descripcion de prueba");
        req.setCategoria("Informativo");
        req.setFechaInicioPublicacion(LocalDate.now());
        req.setFechaFinPublicacion(LocalDate.now().plusDays(10));
        req.setIdsLugaresFisicos(List.of(lugar1.getId(), lugar2.getId()));

        SolicitudAnuncioResponse creado = anuncioService.crear(req, authDe(creador));
        assertThat(creado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(creado.getIdsLugaresFisicos()).containsExactlyInAnyOrder(lugar1.getId(), lugar2.getId());

        anuncioService.aprobar(creado.getId(), authDe(revisor));

        PublicacionAnuncioRequest pub = new PublicacionAnuncioRequest();
        pub.setTituloVisible("Anuncio Con Lugares (Público)");
        pub.setDescripcionVisible("Visible al público");
        pub.setFechaPublicacion(LocalDateTime.now());
        anuncioService.publicar(creado.getId(), pub, authDe(revisor));

        // Lee por el método de listado que se modificó con el fix de N+1.
        List<PublicacionAnuncio> visibles = publicacionAnuncioRepository.findByVisibleTrueOrderByFechaPublicacionDesc();
        PublicacionAnuncio publicado = visibles.stream()
                .filter(p -> p.getSolicitudAnuncio().getId().equals(creado.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("El anuncio publicado no aparece en el listado visible"));

        assertThat(publicado.getVisible()).isTrue();
        assertThat(publicado.getSolicitudAnuncio().getLugaresFisicos())
                .extracting(LugarFisico::getId)
                .containsExactlyInAnyOrder(lugar1.getId(), lugar2.getId());
    }
}

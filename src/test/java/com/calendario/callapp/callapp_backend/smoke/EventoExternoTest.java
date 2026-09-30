package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.dto.request.PublicacionEventoRequest;
import com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoRequest;
import com.calendario.callapp.callapp_backend.dto.response.PublicacionEventoResponse;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudEventoResponse;
import com.calendario.callapp.callapp_backend.entity.*;
import com.calendario.callapp.callapp_backend.mapper.PublicacionEventoMapper;
import com.calendario.callapp.callapp_backend.repository.*;
import com.calendario.callapp.callapp_backend.service.impl.NotificacionServiceImpl;
import com.calendario.callapp.callapp_backend.service.impl.SolicitudEventoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
 * Tests de lógica de negocio para eventos externos:
 *  - Campo esExterno en LugarFisico
 *  - Campo ubicacionExterna en SolicitudEvento
 *  - Propagación en series recurrentes
 *  - Inclusión de ubicacionExterna en PublicacionEventoResponse
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventoExternoTest {

    @Autowired private SolicitudEventoServiceImpl service;
    @Autowired private SolicitudEventoRepository solicitudEventoRepository;
    @Autowired private PublicacionEventoRepository publicacionEventoRepository;
    @Autowired private LugarFisicoRepository lugarFisicoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private TipoEventoCatalogoRepository tipoEventoCatalogoRepository;
    @Autowired private PublicacionEventoMapper publicacionEventoMapper;

    @MockBean private NotificacionServiceImpl notificacionService;

    private Usuario creador;
    private Usuario revisor;
    private Oficina oficina;
    private TipoEventoCatalogo tipo;
    private LugarFisico lugarExterno;
    private LugarFisico lugarNormal;

    @BeforeEach
    void setUp() {
        long t = System.nanoTime();

        RolEntity rolOficina = rolRepository.findByNombre("OFICINA")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("OFICINA"); return rolRepository.save(r); });
        RolEntity rolComs = rolRepository.findByNombre("Comunicaciones")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("Comunicaciones"); return rolRepository.save(r); });

        oficina = new Oficina();
        oficina.setNombre("Oficina Externo " + t);
        oficina.setActiva(true);
        oficina = oficinaRepository.save(oficina);

        creador = guardarUsuario("creador.ext." + t + "@gea.edu.co", rolOficina);
        revisor = guardarUsuario("revisor.ext." + t + "@gea.edu.co", rolComs);

        tipo = new TipoEventoCatalogo();
        tipo.setNombre("Tipo Externo " + t);
        tipo.setColorHex("#3B82F6");
        tipo.setActivo(true);
        tipo = tipoEventoCatalogoRepository.save(tipo);

        lugarExterno = new LugarFisico();
        lugarExterno.setNombre("Externo " + t);
        lugarExterno.setDescripcion("Ubicación externa a la universidad");
        lugarExterno.setActivo(true);
        lugarExterno.setEsExterno(true);
        lugarExterno = lugarFisicoRepository.save(lugarExterno);

        lugarNormal = new LugarFisico();
        lugarNormal.setNombre("Auditorio Central " + t);
        lugarNormal.setDescripcion("Auditorio principal del campus");
        lugarNormal.setCapacidad(300);
        lugarNormal.setActivo(true);
        lugarNormal.setEsExterno(false);
        lugarNormal = lugarFisicoRepository.save(lugarNormal);
    }

    // ─── LugarFisico.esExterno ────────────────────────────────────────────────

    @Test
    void lugar_normal_tiene_es_externo_false_por_defecto() {
        LugarFisico nuevo = new LugarFisico();
        nuevo.setNombre("Sala de Juntas " + System.nanoTime());
        nuevo.setActivo(true);
        LugarFisico guardado = lugarFisicoRepository.save(nuevo);

        LugarFisico recuperado = lugarFisicoRepository.findById(guardado.getId()).orElseThrow();
        assertThat(recuperado.getEsExterno()).isFalse();
    }

    @Test
    void lugar_marcado_como_externo_persiste_es_externo_true() {
        LugarFisico recuperado = lugarFisicoRepository.findById(lugarExterno.getId()).orElseThrow();
        assertThat(recuperado.getEsExterno()).isTrue();
    }

    @Test
    void lugar_normal_no_es_externo() {
        LugarFisico recuperado = lugarFisicoRepository.findById(lugarNormal.getId()).orElseThrow();
        assertThat(recuperado.getEsExterno()).isFalse();
    }

    // ─── SolicitudEvento.ubicacionExterna — creación ─────────────────────────

    @Test
    void crear_evento_externo_persiste_ubicacion_externa() {
        SolicitudEventoRequest req = baseRequest("Congreso en Hotel");
        req.setIdsLugaresFisicos(List.of(lugarExterno.getId()));
        req.setUbicacionExterna("Hotel Ramada, Cúcuta");

        SolicitudEventoResponse resp = service.crear(req, authDe(creador));

        SolicitudEvento guardado = solicitudEventoRepository.findById(resp.getId()).orElseThrow();
        assertThat(guardado.getUbicacionExterna()).isEqualTo("Hotel Ramada, Cúcuta");
    }

    @Test
    void respuesta_de_crear_evento_incluye_ubicacion_externa() {
        SolicitudEventoRequest req = baseRequest("Feria Empresarial");
        req.setIdsLugaresFisicos(List.of(lugarExterno.getId()));
        req.setUbicacionExterna("Parque Santander");

        SolicitudEventoResponse resp = service.crear(req, authDe(creador));

        assertThat(resp.getUbicacionExterna()).isEqualTo("Parque Santander");
    }

    @Test
    void evento_sin_lugar_externo_tiene_ubicacion_externa_null() {
        SolicitudEventoRequest req = baseRequest("Taller Interno");
        req.setIdsLugaresFisicos(List.of(lugarNormal.getId()));
        // No se setea ubicacionExterna

        SolicitudEventoResponse resp = service.crear(req, authDe(creador));

        assertThat(resp.getUbicacionExterna()).isNull();
    }

    @Test
    void ubicacion_externa_con_lugar_normal_se_ignora_correctamente() {
        // El backend no impide setear ubicacionExterna con un lugar normal,
        // pero el flujo estándar no lo envía. Se verifica que no explota.
        SolicitudEventoRequest req = baseRequest("Evento Mixto");
        req.setIdsLugaresFisicos(List.of(lugarNormal.getId()));
        req.setUbicacionExterna(null);

        SolicitudEventoResponse resp = service.crear(req, authDe(creador));
        assertThat(resp.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(resp.getUbicacionExterna()).isNull();
    }

    // ─── SolicitudEvento.ubicacionExterna — actualización ────────────────────

    @Test
    void actualizar_evento_cambia_ubicacion_externa() {
        SolicitudEventoRequest req = baseRequest("Evento Actualizable");
        req.setIdsLugaresFisicos(List.of(lugarExterno.getId()));
        req.setUbicacionExterna("Lugar inicial");
        SolicitudEventoResponse creado = service.crear(req, authDe(creador));

        SolicitudEventoRequest edit = baseRequest("Evento Actualizable");
        edit.setIdsLugaresFisicos(List.of(lugarExterno.getId()));
        edit.setUbicacionExterna("Centro Comercial Unicentro");
        service.actualizarPropia(creado.getId(), edit, authDe(creador));

        SolicitudEvento actualizado = solicitudEventoRepository.findById(creado.getId()).orElseThrow();
        assertThat(actualizado.getUbicacionExterna()).isEqualTo("Centro Comercial Unicentro");
    }

    // ─── Series recurrentes ───────────────────────────────────────────────────

    @Test
    void actualizar_serie_propaga_ubicacion_externa_a_todas_las_instancias() {
        SolicitudEventoRequest req = baseRequest("Serie Externa");
        req.setIdsLugaresFisicos(List.of(lugarExterno.getId()));
        req.setUbicacionExterna("Sede inicial");
        req.setFrecuenciaRecurrencia(FrecuenciaRecurrencia.SEMANAL);
        req.setFechaFinRecurrencia(LocalDate.now().plusDays(28));
        SolicitudEventoResponse maestro = service.crear(req, authDe(creador));

        String idGrupo = maestro.getIdGrupoRecurrencia();
        assertThat(idGrupo).isNotBlank();

        SolicitudEventoRequest edit = baseRequest("Serie Externa");
        edit.setIdsLugaresFisicos(List.of(lugarExterno.getId()));
        edit.setUbicacionExterna("Nueva sede para toda la serie");
        service.actualizarSerie(idGrupo, edit, authDe(creador));

        List<SolicitudEvento> serie = solicitudEventoRepository.findAllByIdGrupoRecurrencia(idGrupo);
        assertThat(serie).hasSizeGreaterThan(1);
        assertThat(serie).allSatisfy(s ->
                assertThat(s.getUbicacionExterna()).isEqualTo("Nueva sede para toda la serie"));
    }

    @Test
    void instancias_recurrentes_heredan_ubicacion_externa_al_crearse() {
        SolicitudEventoRequest req = baseRequest("Serie con Sede");
        req.setIdsLugaresFisicos(List.of(lugarExterno.getId()));
        req.setUbicacionExterna("Teatro Municipal");
        req.setFrecuenciaRecurrencia(FrecuenciaRecurrencia.SEMANAL);
        req.setFechaFinRecurrencia(LocalDate.now().plusDays(21));
        service.crear(req, authDe(creador));

        // Todas las instancias (incluidas las recurrentes) deben tener la misma ubicacion
        List<SolicitudEvento> todas = solicitudEventoRepository.findAll().stream()
                .filter(s -> "Teatro Municipal".equals(s.getUbicacionExterna()))
                .toList();
        assertThat(todas).hasSizeGreaterThan(1);
    }

    // ─── PublicacionEventoMapper — ubicacionExterna en respuesta pública ─────

    @Test
    void publicacion_evento_response_incluye_ubicacion_externa() {
        // Crear
        SolicitudEventoRequest req = baseRequest("Evento Público Externo");
        req.setIdsLugaresFisicos(List.of(lugarExterno.getId()));
        req.setUbicacionExterna("Coliseo Mayor");
        SolicitudEventoResponse creado = service.crear(req, authDe(creador));

        // Aprobar
        service.aprobar(creado.getId(), authDe(revisor));

        // Publicar
        PublicacionEventoRequest pub = new PublicacionEventoRequest();
        pub.setTituloVisible("Evento Público Externo");
        pub.setDescripcionVisible("Descripción visible");
        pub.setFechaPublicacion(LocalDateTime.now());
        service.publicar(creado.getId(), pub, authDe(revisor));

        // Verificar que el mapper propaga ubicacionExterna
        PublicacionEvento publicacion = publicacionEventoRepository
                .findBySolicitudEventoId(creado.getId())
                .orElseThrow();

        PublicacionEventoResponse resp = publicacionEventoMapper.toResponse(publicacion);
        assertThat(resp.getUbicacionExterna()).isEqualTo("Coliseo Mayor");
    }

    @Test
    void publicacion_evento_response_ubicacion_externa_null_cuando_no_es_externo() {
        SolicitudEventoRequest req = baseRequest("Evento Interno Publicado");
        req.setIdsLugaresFisicos(List.of(lugarNormal.getId()));
        SolicitudEventoResponse creado = service.crear(req, authDe(creador));

        service.aprobar(creado.getId(), authDe(revisor));

        PublicacionEventoRequest pub = new PublicacionEventoRequest();
        pub.setTituloVisible("Evento Interno Publicado");
        pub.setDescripcionVisible("Descripción visible");
        pub.setFechaPublicacion(LocalDateTime.now());
        service.publicar(creado.getId(), pub, authDe(revisor));

        PublicacionEvento publicacion = publicacionEventoRepository
                .findBySolicitudEventoId(creado.getId())
                .orElseThrow();

        PublicacionEventoResponse resp = publicacionEventoMapper.toResponse(publicacion);
        assertThat(resp.getUbicacionExterna()).isNull();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

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
}

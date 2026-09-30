package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.entity.*;
import com.calendario.callapp.callapp_backend.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduce el N+1 confirmado con pruebas de carga reales en
 * {@code /app/anuncios/publicados} y {@code /app/eventos/publicados}:
 * acceder a {@code solicitud.getLugaresFisicos()} / {@code getUsuarioSolicitante()}
 * fila por fila dispara una consulta SQL adicional por cada publicación visible,
 * en vez de una sola consulta con JOIN FETCH.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PublicacionNMasUnoTest {

    @Autowired private PublicacionAnuncioRepository publicacionAnuncioRepository;
    @Autowired private PublicacionEventoRepository publicacionEventoRepository;
    @Autowired private SolicitudAnuncioRepository solicitudAnuncioRepository;
    @Autowired private SolicitudEventoRepository solicitudEventoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private LugarFisicoRepository lugarFisicoRepository;
    @Autowired private TipoEventoCatalogoRepository tipoEventoCatalogoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int CANTIDAD = 5;

    @BeforeEach
    void setUp() {
        RolEntity rol = rolRepository.findByNombre("Oficina")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("Oficina"); return rolRepository.save(r); });

        Oficina oficina = new Oficina();
        oficina.setNombre("Oficina NMasUno " + System.currentTimeMillis());
        oficina.setActiva(true);
        oficina = oficinaRepository.save(oficina);

        Usuario solicitante = new Usuario();
        solicitante.setNombre("Solicitante NMasUno");
        solicitante.setCorreo("nmasuno." + System.currentTimeMillis() + "@gea.edu.co");
        solicitante.setPassword("dummy");
        solicitante.setRolEntity(rol);
        solicitante.setOficina(oficina);
        solicitante.setEstado("ACTIVO");
        solicitante.setAuthProvider(AuthProvider.LOCAL);
        solicitante = usuarioRepository.save(solicitante);

        LugarFisico lugar1 = new LugarFisico();
        lugar1.setNombre("Lugar NMasUno A " + System.currentTimeMillis());
        lugar1.setActivo(true);
        lugar1 = lugarFisicoRepository.save(lugar1);

        LugarFisico lugar2 = new LugarFisico();
        lugar2.setNombre("Lugar NMasUno B " + System.currentTimeMillis());
        lugar2.setActivo(true);
        lugar2 = lugarFisicoRepository.save(lugar2);

        TipoEventoCatalogo tipo = new TipoEventoCatalogo();
        tipo.setNombre("NMasUno " + System.currentTimeMillis());
        tipo.setColorHex("#CE1126");
        tipo.setActivo(true);
        tipo = tipoEventoCatalogoRepository.save(tipo);

        for (int i = 0; i < CANTIDAD; i++) {
            SolicitudAnuncio anuncio = new SolicitudAnuncio();
            anuncio.setTitulo("Anuncio NMasUno " + i);
            anuncio.setDescripcion("Descripcion " + i);
            anuncio.setCategoria("Informativo");
            anuncio.setFechaInicioPublicacion(LocalDate.now());
            anuncio.setFechaFinPublicacion(LocalDate.now().plusDays(10));
            anuncio.setUsuarioSolicitante(solicitante);
            anuncio.setOficina(oficina);
            anuncio.setEstado(EstadoSolicitud.PUBLICADA);
            anuncio.getLugaresFisicos().add(lugar1);
            anuncio.getLugaresFisicos().add(lugar2);
            anuncio = solicitudAnuncioRepository.save(anuncio);

            PublicacionAnuncio pubAnuncio = new PublicacionAnuncio();
            pubAnuncio.setSolicitudAnuncio(anuncio);
            pubAnuncio.setVisible(true);
            pubAnuncio.setFechaPublicacion(java.time.LocalDateTime.now());
            pubAnuncio.setTituloVisible(anuncio.getTitulo());
            pubAnuncio.setDescripcionVisible(anuncio.getDescripcion());
            pubAnuncio.setUsuarioPublicador(solicitante);
            publicacionAnuncioRepository.save(pubAnuncio);

            SolicitudEvento evento = new SolicitudEvento();
            evento.setNombreEvento("Evento NMasUno " + i);
            evento.setDescripcionEvento("Descripcion " + i);
            evento.setFechaEvento(LocalDate.now().plusDays(i));
            evento.setHoraInicio(LocalTime.of(9, 0));
            evento.setHoraFin(LocalTime.of(11, 0));
            evento.setOficina(oficina);
            evento.setUsuarioSolicitante(solicitante);
            evento.setTipoEventoCatalogo(tipo);
            evento.setEstado(EstadoSolicitud.PUBLICADA);
            evento.setEsPrincipal(true);
            evento.getLugaresFisicos().add(lugar1);
            evento.getLugaresFisicos().add(lugar2);
            evento = solicitudEventoRepository.save(evento);

            PublicacionEvento pubEvento = new PublicacionEvento();
            pubEvento.setSolicitudEvento(evento);
            pubEvento.setVisible(true);
            pubEvento.setFechaPublicacion(java.time.LocalDateTime.now());
            pubEvento.setTituloVisible(evento.getNombreEvento());
            pubEvento.setDescripcionVisible(evento.getDescripcionEvento());
            pubEvento.setUsuarioPublicador(solicitante);
            publicacionEventoRepository.save(pubEvento);
        }

        entityManager.flush();
        entityManager.clear();
    }

    private long prepStatementCount() {
        SessionFactory sf = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        return sf.getStatistics().getPrepareStatementCount();
    }

    private void clearStats() {
        SessionFactory sf = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        sf.getStatistics().clear();
    }

    @Test
    void listar_anuncios_publicados_no_dispara_n_mas_uno() {
        clearStats();

        List<PublicacionAnuncio> resultado = publicacionAnuncioRepository.findByVisibleTrueOrderByFechaPublicacionDesc();
        // Simula exactamente lo que hace PublicacionAnuncioMapper fila por fila.
        resultado.forEach(p -> {
            p.getSolicitudAnuncio().getLugaresFisicos().size();
            p.getSolicitudAnuncio().getOficina();
            p.getSolicitudAnuncio().getUsuarioSolicitante().getCorreo();
        });

        long queries = prepStatementCount();
        assertThat(resultado).hasSizeGreaterThanOrEqualTo(CANTIDAD);
        assertThat(queries)
                .as("Una sola consulta con JOIN FETCH debe bastar, sin importar cuántas publicaciones haya")
                .isLessThanOrEqualTo(2);
    }

    @Test
    void listar_eventos_publicados_no_dispara_n_mas_uno() {
        clearStats();

        List<PublicacionEvento> resultado = publicacionEventoRepository.getAllVisibleOptimized(PageRequest.of(0, 300));
        // Simula exactamente lo que hace PublicacionEventoMapper fila por fila.
        resultado.forEach(p -> {
            p.getSolicitudEvento().getLugaresFisicos().size();
            p.getSolicitudEvento().getUsuarioSolicitante().getCorreo();
        });

        long queries = prepStatementCount();
        assertThat(resultado).hasSizeGreaterThanOrEqualTo(CANTIDAD);
        assertThat(queries)
                .as("Una sola consulta con JOIN FETCH debe bastar, sin importar cuántas publicaciones haya")
                .isLessThanOrEqualTo(2);
    }
}

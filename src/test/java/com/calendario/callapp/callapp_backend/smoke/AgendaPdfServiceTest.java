package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.dto.request.PublicacionEventoRequest;
import com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoParticipanteRequest;
import com.calendario.callapp.callapp_backend.dto.request.SolicitudEventoRequest;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudEventoResponse;
import com.calendario.callapp.callapp_backend.entity.*;
import com.calendario.callapp.callapp_backend.repository.*;
import com.calendario.callapp.callapp_backend.service.impl.AgendaPdfService;
import com.calendario.callapp.callapp_backend.service.impl.NotificacionServiceImpl;
import com.calendario.callapp.callapp_backend.service.impl.SolicitudEventoServiceImpl;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
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
 * Cubre dos correcciones reportadas en la agenda PDF exportable:
 * <ul>
 *   <li>la descripción del evento y el/los organizador(es) deben aparecer en
 *   cada entrada, y no solo hora/lugar/oficina;</li>
 *   <li>cuando la solicitud tiene un lugar del catálogo llamado "Externo"
 *   (placeholder) MÁS {@code ubicacionExterna}, el PDF debe mostrar el lugar
 *   real en su lugar (ej. "Su casa"), no concatenar ambos como
 *   "Plaza de Banderas, Externo, Su casa" — mismo criterio que ya aplica
 *   CalendarView.jsx en el frontend.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgendaPdfServiceTest {

    @Autowired private AgendaPdfService agendaPdfService;
    @Autowired private SolicitudEventoServiceImpl eventoService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private LugarFisicoRepository lugarFisicoRepository;
    @Autowired private TipoEventoCatalogoRepository tipoEventoCatalogoRepository;

    @MockBean private NotificacionServiceImpl notificacionService; // no-op: evita SMTP/push en tests

    private Usuario creador;
    private Usuario revisor;
    private LocalDate fechaEvento;

    @BeforeEach
    void setUp() {
        long t = System.nanoTime();

        RolEntity rolOficina = rolRepository.findByNombre("OFICINA")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("OFICINA"); return rolRepository.save(r); });
        RolEntity rolComs = rolRepository.findByNombre("Comunicaciones")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("Comunicaciones"); return rolRepository.save(r); });

        Oficina oficina = new Oficina();
        oficina.setNombre("Oficina AgendaPdf " + t);
        oficina.setActiva(true);
        oficina = oficinaRepository.save(oficina);

        creador = guardarUsuario("creador.agendapdf." + t + "@gea.edu.co", rolOficina, oficina);
        revisor = guardarUsuario("revisor.agendapdf." + t + "@gea.edu.co", rolComs, oficina);

        fechaEvento = LocalDate.now().plusDays(7);
    }

    private Usuario guardarUsuario(String correo, RolEntity rol, Oficina oficina) {
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
    void pdf_incluye_descripcion_organizador_y_resuelve_el_lugar_externo() throws Exception {
        long t = System.nanoTime();

        TipoEventoCatalogo tipo = new TipoEventoCatalogo();
        tipo.setNombre("Tipo AgendaPdf " + t);
        tipo.setColorHex("#CE1126");
        tipo.setActivo(true);
        tipo = tipoEventoCatalogoRepository.save(tipo);

        LugarFisico plazaBanderas = new LugarFisico();
        plazaBanderas.setNombre("Plaza de Banderas " + t);
        plazaBanderas.setActivo(true);
        plazaBanderas = lugarFisicoRepository.save(plazaBanderas);

        // Entrada placeholder del catálogo (esExterno=true) que se usa junto
        // con ubicacionExterna para marcar "el evento es fuera del campus" —
        // es justo la combinación que produjo el bug reportado. "nombre" es
        // único en la tabla, así que se reutiliza si el seed de datos ya trae
        // una fila "Externo" (algo que asume la app).
        LugarFisico externoPlaceholder = lugarFisicoRepository.findByNombreIgnoreCase("Externo")
                .orElseGet(() -> {
                    LugarFisico l = new LugarFisico();
                    l.setNombre("Externo");
                    l.setActivo(true);
                    l.setEsExterno(true);
                    return lugarFisicoRepository.save(l);
                });
        if (!Boolean.TRUE.equals(externoPlaceholder.getEsExterno())) {
            externoPlaceholder.setEsExterno(true);
            externoPlaceholder = lugarFisicoRepository.save(externoPlaceholder);
        }

        SolicitudEventoRequest req = new SolicitudEventoRequest();
        req.setNombreEvento("Seminario de prueba " + t);
        req.setDescripcionEvento("Descripcion unica de prueba " + t);
        req.setUbicacionExterna("Su casa " + t);
        req.setFechaEvento(fechaEvento);
        req.setHoraInicio(LocalTime.of(8, 0));
        req.setHoraFin(LocalTime.of(10, 0));
        req.setTipoEvento(tipo.getNombre());
        req.setIdsLugaresFisicos(List.of(plazaBanderas.getId(), externoPlaceholder.getId()));

        SolicitudEventoParticipanteRequest organizador = new SolicitudEventoParticipanteRequest();
        organizador.setNombre("Organizador De Prueba " + t);
        organizador.setTipo(TipoParticipante.ORGANIZADOR);
        req.setParticipantes(List.of(organizador));

        SolicitudEventoResponse creado = eventoService.crear(req, authDe(creador));
        eventoService.aprobar(creado.getId(), authDe(revisor));

        PublicacionEventoRequest pub = new PublicacionEventoRequest();
        pub.setTituloVisible(req.getNombreEvento());
        pub.setDescripcionVisible("Visible al público");
        pub.setFechaPublicacion(LocalDateTime.now());
        eventoService.publicar(creado.getId(), pub, authDe(revisor));

        byte[] pdfBytes = agendaPdfService.exportarAgendaPdf(fechaEvento, fechaEvento);

        String texto;
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extractor.getTextFromPage(page));
            }
            texto = sb.toString();
        }

        assertThat(texto).contains(req.getDescripcionEvento());
        assertThat(texto).contains("Organiza: " + organizador.getNombre());
        // El lugar externo real debe aparecer...
        assertThat(texto).contains(req.getUbicacionExterna());
        // ...y el placeholder "Externo" del catálogo no debe sobrevivir como
        // palabra suelta: si aparece, es que se concatenó en vez de
        // sustituirse (el bug reportado: "Plaza de Banderas, Externo, Su casa").
        assertThat(texto).doesNotContainPattern("(?i)\\bExterno\\b");
    }
}

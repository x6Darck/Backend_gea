package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.request.ActualizarReporteRequest;
import com.calendario.callapp.callapp_backend.dto.request.GenerarReporteRequest;
import com.calendario.callapp.callapp_backend.dto.common.GrupoDatoDTO;
import com.calendario.callapp.callapp_backend.dto.response.ReporteDashboardDTO;
import com.calendario.callapp.callapp_backend.dto.response.ReporteGeneradoResponse;
import com.calendario.callapp.callapp_backend.dto.response.ReporteSolicitudesResponse;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudResumenDTO;
import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.entity.ReporteGenerado;
import com.calendario.callapp.callapp_backend.entity.SolicitudAnuncio;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.ReporteGeneradoRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.calendario.callapp.callapp_backend.repository.SolicitudAnuncioRepository;
import com.calendario.callapp.callapp_backend.repository.SolicitudEventoRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * Generación de reportes estadísticos en PDF (OpenPDF/iText) y Excel (Apache POI).
 *
 * <p>El alcance de los datos depende del rol del usuario autenticado:
 * {@code SUPER_ADMIN} y {@code ADMIN} reciben totales globales; los demás roles
 * reciben datos filtrados a su oficina. Los reportes generados se persisten como
 * {@link com.calendario.callapp.callapp_backend.entity.ReporteGenerado} para auditoría y descarga posterior.
 * Los documentos PDF usan la paleta de marca GEA y se construyen con celdas
 * {@code PdfPTable}/{@code PdfPCell}. Los Excel siguen el mismo esquema pero con
 * {@code XSSFWorkbook} de Apache POI.</p>
 */
public class ReporteServiceImpl {

    private final SolicitudEventoRepository solicitudEventoRepository;
    private final SolicitudAnuncioRepository solicitudAnuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReporteGeneradoRepository reporteGeneradoRepository;
    
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    // ─── Paleta de marca GEA (PDF) ───────────────────────────────────────────
    private static final java.awt.Color GEA_ROJO = java.awt.Color.decode("#CE1126");
    private static final java.awt.Color GEA_GRIS_OSCURO = java.awt.Color.decode("#1e293b");
    private static final java.awt.Color GEA_GRIS_CLARO = java.awt.Color.decode("#f1f5f9");
    private static final java.awt.Color GEA_BORDE = java.awt.Color.decode("#e2e8f0");
    private static final java.awt.Color GEA_ZEBRA = java.awt.Color.decode("#f8fafc");

    public ReporteGeneradoResponse crearReporte(GenerarReporteRequest request, Authentication authentication) {
        Usuario usuario = obtenerUsuario(authentication);
        validarFechas(request.getDesde(), request.getHasta());

        String formato = normalizarFormato(request.getFormato());

        ReporteGenerado reporte = new ReporteGenerado();
        reporte.setNombre(request.getNombre());
        reporte.setDescripcion(request.getDescripcion());
        reporte.setFormato(formato);
        reporte.setFechaDesde(request.getDesde());
        reporte.setFechaHasta(request.getHasta());
        reporte.setAlcance(request.getAlcance());
        reporte.setFechaCreacion(LocalDateTime.now());
        reporte.setUsuarioGenerador(usuario);
        reporte.setIdOficina(request.getIdOficina());
        reporte.setIdTipoEvento(request.getIdTipoEvento());

        return toReporteGeneradoResponse(reporteGeneradoRepository.save(reporte));
    }

    @Transactional(readOnly = true)
    public List<ReporteGeneradoResponse> listarReportes(
            Long filterOficinaId,
            LocalDate filterFechaDesde,
            LocalDate filterFechaHasta,
            Authentication authentication) {

        Usuario usuario = obtenerUsuario(authentication);
        List<ReporteGenerado> reportes;

        if (usuario.getRol().tieneAccesoGlobal()) {
            reportes = reporteGeneradoRepository.findAll(
                    org.springframework.data.domain.PageRequest.of(0, 500,
                            org.springframework.data.domain.Sort.by(
                                    org.springframework.data.domain.Sort.Direction.DESC, "fechaCreacion"))
            ).getContent();
        } else if (usuario.getRol().esOficina() || usuario.getRol().name().equals("USUARIO_AUTENTICADO_APP")) {
            if (usuario.getOficina() == null) {
                reportes = reporteGeneradoRepository.findByUsuarioGeneradorIdOrderByFechaCreacionDesc(usuario.getId());
            } else {
                reportes = reporteGeneradoRepository
                        .findByUsuarioGeneradorOficinaIdOrderByFechaCreacionDesc(usuario.getOficina().getId());
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para listar reportes");
        }

        return reportes.stream()
                .filter(r -> {
                    if (filterOficinaId != null) {
                        if (r.getUsuarioGenerador().getOficina() == null) return false;
                        if (!r.getUsuarioGenerador().getOficina().getId().equals(filterOficinaId)) return false;
                    }
                    if (filterFechaDesde != null) {
                        if (r.getFechaCreacion().toLocalDate().isBefore(filterFechaDesde)) return false;
                    }
                    if (filterFechaHasta != null) {
                        if (r.getFechaCreacion().toLocalDate().isAfter(filterFechaHasta)) return false;
                    }
                    return true;
                })
                .sorted((a, b) -> b.getFechaCreacion().compareTo(a.getFechaCreacion()))
                .map(this::toReporteGeneradoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReporteGeneradoResponse> listarReportes(Authentication authentication) {
        return listarReportes(null, null, null, authentication);
    }

    @Transactional(readOnly = true)
    public byte[] exportarReporteGenerado(Long reporteId, Authentication authentication) {
        ReporteGenerado reporte = buscarReporte(reporteId, authentication);
        DatosReporte datos = obtenerDatosReporte(
                reporte.getFechaDesde(),
                reporte.getFechaHasta(),
                reporte.getAlcance(),
                reporte.getIdOficina(),
                reporte.getIdTipoEvento(),
                authentication);

        if ("PDF".equals(reporte.getFormato())) {
            return exportarPdfInterno(reporte.getFechaDesde(), reporte.getFechaHasta(), datos, authentication);
        } else {
            return exportarXlsxInterno(datos);
        }
    }

    @Transactional(readOnly = true)
    public ReporteDashboardDTO obtenerDashboardStats(Long oficinaId, LocalDate desde, LocalDate hasta, String tipo, Authentication authentication) {
        try {
            validarFechas(desde, hasta);
            LocalDateTime fechaDesde = desde.atStartOfDay();
            LocalDateTime fechaHasta = hasta.atTime(LocalTime.MAX);

            String tipoNorm = tipo != null ? tipo.trim().toUpperCase() : "GLOBAL";
            boolean incluirEventos = !"ANUNCIOS".equals(tipoNorm);
            boolean incluirAnuncios = !"EVENTOS".equals(tipoNorm);

            List<Object[]> porTipo = incluirEventos ? solicitudEventoRepository.countEventosByTipoFiltered(oficinaId, fechaDesde, fechaHasta) : List.of();
            List<Object[]> evOficina = incluirEventos ? solicitudEventoRepository.countEventosByOficinaFiltered(oficinaId, fechaDesde, fechaHasta) : List.of();
            List<Object[]> evEstado = incluirEventos ? solicitudEventoRepository.countEventosByEstadoFiltered(oficinaId, fechaDesde, fechaHasta) : List.of();
            List<Object[]> evMes = incluirEventos ? solicitudEventoRepository.countEventosByMesFiltered(oficinaId, fechaDesde, fechaHasta) : List.of();
            List<Object[]> anOficina = incluirAnuncios ? solicitudAnuncioRepository.countAnunciosByOficinaFiltered(oficinaId, fechaDesde, fechaHasta) : List.of();
            List<Object[]> anEstado = incluirAnuncios ? solicitudAnuncioRepository.countAnunciosByEstadoFiltered(oficinaId, fechaDesde, fechaHasta) : List.of();
            List<Object[]> anMes = incluirAnuncios ? solicitudAnuncioRepository.countAnunciosByMesFiltered(oficinaId, fechaDesde, fechaHasta) : List.of();

            long totalEventos = incluirEventos ? solicitudEventoRepository.countFiltered(oficinaId, fechaDesde, fechaHasta) : 0L;
            long totalAnuncios = incluirAnuncios ? solicitudAnuncioRepository.countFiltered(oficinaId, fechaDesde, fechaHasta) : 0L;
            long total = totalEventos + totalAnuncios;

            // Optimización: Extraer conteos de los mapas de estado ya obtenidos para evitar ráfaga de consultas extra
            java.util.Map<EstadoSolicitud, Long> evEstadoMap = new java.util.HashMap<>();
            evEstado.forEach(r -> evEstadoMap.put((EstadoSolicitud)r[0], (Long)r[1]));
            
            java.util.Map<EstadoSolicitud, Long> anEstadoMap = new java.util.HashMap<>();
            anEstado.forEach(r -> anEstadoMap.put((EstadoSolicitud)r[0], (Long)r[1]));

            long aprobados = evEstadoMap.getOrDefault(EstadoSolicitud.APROBADA, 0L) + evEstadoMap.getOrDefault(EstadoSolicitud.PUBLICADA, 0L)
                           + anEstadoMap.getOrDefault(EstadoSolicitud.APROBADA, 0L) + anEstadoMap.getOrDefault(EstadoSolicitud.PUBLICADA, 0L);
            
            long pendientes = evEstadoMap.getOrDefault(EstadoSolicitud.PENDIENTE, 0L) + anEstadoMap.getOrDefault(EstadoSolicitud.PENDIENTE, 0L);
                
            long rechazados = evEstadoMap.getOrDefault(EstadoSolicitud.RECHAZADA, 0L) + anEstadoMap.getOrDefault(EstadoSolicitud.RECHAZADA, 0L);

            double tasa = total > 0 ? (double) aprobados / total * 100 : 0;

            List<SolicitudEvento> evList = !incluirEventos ? List.of()
                : (oficinaId != null)
                    ? solicitudEventoRepository.findByOficinaIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(oficinaId, fechaDesde, fechaHasta)
                    : solicitudEventoRepository.findByFechaCreacionBetweenOrderByFechaCreacionDesc(fechaDesde, fechaHasta);

            List<SolicitudAnuncio> anList = !incluirAnuncios ? List.of()
                : (oficinaId != null)
                    ? solicitudAnuncioRepository.findByOficinaIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(oficinaId, fechaDesde, fechaHasta)
                    : solicitudAnuncioRepository.findByFechaCreacionBetweenOrderByFechaCreacionDesc(fechaDesde, fechaHasta);

            List<SolicitudResumenDTO> solicitudesDetalle = new java.util.ArrayList<>();
            evList.stream().limit(10).forEach(e -> solicitudesDetalle.add(mapToResumen(e)));
            anList.stream().limit(10).forEach(a -> solicitudesDetalle.add(mapToResumen(a)));
            solicitudesDetalle.sort((a, b) -> b.getFechaRegistro().compareTo(a.getFechaRegistro()));

            ReporteDashboardDTO dto = ReporteDashboardDTO.builder()
                    .totalSolicitudes(total)
                    .totalAprobados(aprobados)
                    .totalPendientes(pendientes)
                    .totalRechazados(rechazados)
                    .tasaAprobacion(Math.round(tasa * 100.0) / 100.0)
                    .eventosPorTipo(mapToGrupoDato(porTipo).stream()
                            .filter(g -> !g.getEtiqueta().trim().equals("-") && !g.getEtiqueta().trim().isEmpty() && !g.getEtiqueta().equals("Sin Tipo"))
                            .toList())
                    .solicitudesPorOficina(mapToGrupoDato(combinarResultados(evOficina, anOficina)))
                    .tendenciaEstado(mapToGrupoDato(combinarResultados(evEstado, anEstado)))
                    .solicitudesPorMes(fillMonthGaps(combinarResultados(evMes, anMes)))
                    .solicitudes(solicitudesDetalle)
                    .build();
            
            entityManager.flush();
            return dto;
        } catch (Exception e) {
            log.error("Error generando estadísticas de dashboard", e);
            return ReporteDashboardDTO.builder()
                    .totalSolicitudes(0L).totalAprobados(0L).totalPendientes(0L).totalRechazados(0L)
                    .tasaAprobacion(0.0)
                    .eventosPorTipo(new java.util.ArrayList<>())
                    .solicitudesPorOficina(new java.util.ArrayList<>())
                    .tendenciaEstado(new java.util.ArrayList<>())
                    .solicitudesPorMes(new java.util.ArrayList<>())
                    .build();
        }
    }

    private List<Object[]> combinarResultados(List<Object[]> lista1, List<Object[]> lista2) {
        java.util.Map<Object, Object[]> mapa = new java.util.HashMap<>();
        if (lista1 != null) {
            for (Object[] r : lista1) {
                if (r != null && r.length >= 2 && r[0] != null) {
                    long valor = r[1] instanceof Number ? ((Number) r[1]).longValue() : 0L;
                    String color = r.length >= 3 && r[2] != null ? r[2].toString() : null;
                    mapa.put(r[0], new Object[]{valor, color});
                }
            }
        }
        if (lista2 != null) {
            for (Object[] r : lista2) {
                if (r != null && r.length >= 2 && r[0] != null) {
                    Object key = r[0];
                    long valor = r[1] instanceof Number ? ((Number) r[1]).longValue() : 0L;
                    String color = r.length >= 3 && r[2] != null ? r[2].toString() : null;
                    if (mapa.containsKey(key)) {
                        Object[] exist = mapa.get(key);
                        exist[0] = ((Long)exist[0]) + valor;
                        if (exist[1] == null && color != null) exist[1] = color;
                    } else {
                        mapa.put(key, new Object[]{valor, color});
                    }
                }
            }
        }
        List<Object[]> resultado = new java.util.ArrayList<>();
        mapa.forEach((k, v) -> resultado.add(new Object[]{k, v[0], v[1]}));
        return resultado;
    }

    private List<GrupoDatoDTO> mapToGrupoDato(List<Object[]> raw) {
        if (raw == null) return new java.util.ArrayList<>();
        return raw.stream()
                .filter(r -> r != null && r.length >= 2 && r[0] != null)
                .map(r -> {
                    GrupoDatoDTO dto = new GrupoDatoDTO(
                        r[0].toString(), 
                        r[1] instanceof Number ? ((Number) r[1]).longValue() : 0L
                    );
                    if (r.length >= 3 && r[2] != null) dto.setColor(r[2].toString());
                    return dto;
                })
                .sorted((a, b) -> b.getValor().compareTo(a.getValor()))
                .toList();
    }

    private List<GrupoDatoDTO> fillMonthGaps(List<Object[]> raw) {
        String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        java.util.Map<Integer, Long> dataMap = new java.util.HashMap<>();
        for (int i = 1; i <= 12; i++) dataMap.put(i, 0L);
        if (raw != null) {
            for (Object[] r : raw) {
                int m;
                if (r[0] instanceof Number) m = ((Number) r[0]).intValue();
                else {
                    try { m = Integer.parseInt(r[0].toString()); } catch (Exception e) { continue; }
                }
                dataMap.put(m, r[1] instanceof Number ? ((Number) r[1]).longValue() : 0L);
            }
        }
        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(i -> new GrupoDatoDTO(meses[i - 1], dataMap.get(i)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReporteSolicitudesResponse generarResumen(LocalDate desde, LocalDate hasta, Authentication authentication) {
        DatosReporte datos = obtenerDatosReporte(desde, hasta, "GLOBAL", null, null, authentication);
        List<SolicitudEvento> eventos = datos.eventos();
        List<SolicitudAnuncio> anuncios = datos.anuncios();

        return ReporteSolicitudesResponse.builder()
                .alcance(datos.alcance())
                .desde(desde)
                .hasta(hasta)
                .totalSolicitudesEvento(eventos.size())
                .totalSolicitudesAnuncio(anuncios.size())
                .eventosPendientes(contarEventos(eventos, EstadoSolicitud.PENDIENTE))
                .eventosAprobados(contarEventos(eventos, EstadoSolicitud.APROBADA))
                .eventosRechazados(contarEventos(eventos, EstadoSolicitud.RECHAZADA))
                .eventosPublicados(contarEventos(eventos, EstadoSolicitud.PUBLICADA))
                .anunciosPendientes(contarAnuncios(anuncios, EstadoSolicitud.PENDIENTE))
                .anunciosAprobados(contarAnuncios(anuncios, EstadoSolicitud.APROBADA))
                .anunciosRechazados(contarAnuncios(anuncios, EstadoSolicitud.RECHAZADA))
                .anunciosPublicados(contarAnuncios(anuncios, EstadoSolicitud.PUBLICADA))
                .solicitudes(mapearSolicitudes(eventos, anuncios))
                .build();
    }

    private List<SolicitudResumenDTO> mapearSolicitudes(List<SolicitudEvento> eventos, List<SolicitudAnuncio> anuncios) {
        List<SolicitudResumenDTO> lista = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        eventos.forEach(e -> lista.add(SolicitudResumenDTO.builder()
                .id(e.getId())
                .tipo("EVENTO")
                .titulo(e.getNombreEvento())
                .oficina(e.getOficina() != null ? e.getOficina().getNombre() : "N/A")
                .fechaRegistro(e.getFechaCreacion() != null ? e.getFechaCreacion().format(dtf) : "-")
                .estado(String.valueOf(e.getEstado()))
                .build()));
        anuncios.forEach(a -> lista.add(SolicitudResumenDTO.builder()
                .id(a.getId())
                .tipo("ANUNCIO")
                .titulo(a.getTitulo())
                .oficina(a.getOficina() != null ? a.getOficina().getNombre() : (a.getCategoria() != null ? a.getCategoria() : "N/A"))
                .fechaRegistro(a.getFechaCreacion() != null ? a.getFechaCreacion().format(dtf) : "-")
                .estado(String.valueOf(a.getEstado()))
                .build()));
        lista.sort((a, b) -> b.getFechaRegistro().compareTo(a.getFechaRegistro()));
        return lista;
    }

    @Transactional(readOnly = true)
    public byte[] exportarXlsx(LocalDate desde, LocalDate hasta, Authentication authentication) {
        DatosReporte datos = obtenerDatosReporte(desde, hasta, "GLOBAL", null, null, authentication);
        return exportarXlsxInterno(datos);
    }

    @Transactional(readOnly = true)
    public byte[] exportarPdf(LocalDate desde, LocalDate hasta, Authentication authentication) {
        DatosReporte datos = obtenerDatosReporte(desde, hasta, "GLOBAL", null, null, authentication);
        return exportarPdfInterno(desde, hasta, datos, authentication);
    }

    private byte[] exportarPdfInterno(LocalDate desde, LocalDate hasta, DatosReporte datos,
            Authentication authentication) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 48, 54);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PieDePagina());
            document.open();

            agregarPortada(document, desde, hasta, datos, authentication);
            agregarTarjetasKpi(document, datos);
            agregarGraficos(document, datos);
            agregarTablaDetalle(document, datos);

            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            log.error("Error crítico al generar PDF de reporte: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al generar PDF");
        }
    }

    // ─── PDF: Portada / encabezado de marca ──────────────────────────────────
    private void agregarPortada(Document document, LocalDate desde, LocalDate hasta, DatosReporte datos,
            Authentication authentication) throws Exception {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[] { 1f, 3.4f });

        // LOGO: encabezado tipográfico. Para usar imagen, reemplazar este bloque por
        // Image.getInstance(...) cargando el PNG del logo GEA y añadirlo a logoCell.
        PdfPCell logoCell = new PdfPCell(new Phrase("GEA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 42, GEA_ROJO)));
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph t1 = new Paragraph("Sistema de Gestión de Eventos y Anuncios",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, GEA_GRIS_OSCURO));
        Paragraph t2 = new Paragraph("Reporte de Auditoría — " + datos.alcance(),
                FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.GRAY));
        titleCell.addElement(t1);
        titleCell.addElement(t2);
        headerTable.addCell(titleCell);
        document.add(headerTable);

        com.lowagie.text.pdf.draw.LineSeparator linea =
                new com.lowagie.text.pdf.draw.LineSeparator(2.5f, 100, GEA_ROJO, Element.ALIGN_CENTER, -4);
        document.add(new Chunk(linea));

        com.lowagie.text.Font etiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GEA_GRIS_OSCURO);
        com.lowagie.text.Font valor = FontFactory.getFont(FontFactory.HELVETICA, 9, GEA_GRIS_OSCURO);

        PdfPTable metaTable = new PdfPTable(1);
        metaTable.setWidthPercentage(100);
        metaTable.setSpacingBefore(14f);
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBackgroundColor(GEA_GRIS_CLARO);
        metaCell.setPadding(12);
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.addElement(lineaMeta("Periodo consultado:", desde + " al " + hasta, etiqueta, valor));
        metaCell.addElement(lineaMeta("Alcance:", datos.alcance(), etiqueta, valor));
        metaCell.addElement(lineaMeta("Generado el:", LocalDateTime.now().withNano(0).toString(), etiqueta, valor));
        metaCell.addElement(lineaMeta("Responsable:", authentication.getName(), etiqueta, valor));
        metaTable.addCell(metaCell);
        document.add(metaTable);
    }

    private Paragraph lineaMeta(String label, String value, com.lowagie.text.Font boldF, com.lowagie.text.Font normF) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", boldF));
        p.add(new Chunk(value, normF));
        p.setLeading(15f);
        return p;
    }

    // ─── PDF: Tarjetas KPI ───────────────────────────────────────────────────
    private void agregarTarjetasKpi(Document document, DatosReporte datos) throws Exception {
        long total = datos.eventos().size() + datos.anuncios().size();
        long aprob = contarEventos(datos.eventos(), EstadoSolicitud.APROBADA) + contarEventos(datos.eventos(), EstadoSolicitud.PUBLICADA)
                + contarAnuncios(datos.anuncios(), EstadoSolicitud.APROBADA) + contarAnuncios(datos.anuncios(), EstadoSolicitud.PUBLICADA);
        long pend = contarEventos(datos.eventos(), EstadoSolicitud.PENDIENTE) + contarAnuncios(datos.anuncios(), EstadoSolicitud.PENDIENTE);
        long rech = contarEventos(datos.eventos(), EstadoSolicitud.RECHAZADA) + contarAnuncios(datos.anuncios(), EstadoSolicitud.RECHAZADA);
        double tasa = total > 0 ? (double) aprob / total * 100 : 0;

        PdfPTable kpi = new PdfPTable(5);
        kpi.setWidthPercentage(100);
        kpi.setSpacingBefore(18f);
        kpi.setSpacingAfter(6f);
        kpi.addCell(tarjetaKpi("TOTAL", String.valueOf(total), java.awt.Color.decode("#334155")));
        kpi.addCell(tarjetaKpi("APROBADAS", String.valueOf(aprob), java.awt.Color.decode("#16a34a")));
        kpi.addCell(tarjetaKpi("PENDIENTES", String.valueOf(pend), java.awt.Color.decode("#f59e0b")));
        kpi.addCell(tarjetaKpi("RECHAZADAS", String.valueOf(rech), GEA_ROJO));
        kpi.addCell(tarjetaKpi("TASA APROB.", Math.round(tasa) + "%", java.awt.Color.decode("#0ea5e9")));
        document.add(kpi);
    }

    private PdfPCell tarjetaKpi(String label, String value, java.awt.Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(mezclarConBlanco(color, 0.13f));
        Paragraph v = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, color));
        v.setAlignment(Element.ALIGN_CENTER);
        Paragraph l = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, java.awt.Color.decode("#64748b")));
        l.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(v);
        cell.addElement(l);
        return cell;
    }

    private java.awt.Color mezclarConBlanco(java.awt.Color c, float ratio) {
        int r = Math.round(c.getRed() * ratio + 255 * (1 - ratio));
        int g = Math.round(c.getGreen() * ratio + 255 * (1 - ratio));
        int b = Math.round(c.getBlue() * ratio + 255 * (1 - ratio));
        return new java.awt.Color(r, g, b);
    }

    // ─── PDF: Gráfico embebido (JFreeChart → PNG) ────────────────────────────
    private void agregarGraficos(Document document, DatosReporte datos) throws Exception {
        java.util.Map<String, Long> porEstado = new java.util.LinkedHashMap<>();
        for (SolicitudEvento e : datos.eventos()) {
            porEstado.merge(String.valueOf(e.getEstado()), 1L, Long::sum);
        }
        for (SolicitudAnuncio a : datos.anuncios()) {
            porEstado.merge(String.valueOf(a.getEstado()), 1L, Long::sum);
        }

        if (porEstado.isEmpty()) return;

        Paragraph sec = new Paragraph("Resumen Gráfico", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, GEA_GRIS_OSCURO));
        sec.setSpacingBefore(18f);
        sec.setSpacingAfter(8f);
        document.add(sec);

        Paragraph titulo = new Paragraph("Distribución por estado", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GEA_GRIS_OSCURO));
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(4f);
        document.add(titulo);

        byte[] png = graficoPastelEstado(porEstado);
        if (png != null) {
            Image img = Image.getInstance(png);
            img.scaleToFit(320, 230);
            img.setAlignment(Element.ALIGN_CENTER);
            document.add(img);
        }
    }

    private byte[] graficoPastelEstado(java.util.Map<String, Long> data) {
        if (data == null || data.isEmpty()) return null;
        try {
            org.jfree.data.general.DefaultPieDataset<String> dataset = new org.jfree.data.general.DefaultPieDataset<>();
            data.forEach(dataset::setValue);
            org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createPieChart(null, dataset, false, true, false);
            chart.setBackgroundPaint(java.awt.Color.WHITE);
            @SuppressWarnings("unchecked")
            org.jfree.chart.plot.PiePlot<String> plot = (org.jfree.chart.plot.PiePlot<String>) chart.getPlot();
            plot.setBackgroundPaint(java.awt.Color.WHITE);
            plot.setOutlineVisible(false);
            plot.setLabelFont(new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 11));
            plot.setLabelBackgroundPaint(java.awt.Color.WHITE);
            data.keySet().forEach(k -> plot.setSectionPaint(k, colorEstado(k)));
            return chartToPng(chart, 360, 260);
        } catch (Exception e) {
            log.warn("No se pudo generar gráfico de pastel: {}", e.getMessage());
            return null;
        }
    }

    private byte[] chartToPng(org.jfree.chart.JFreeChart chart, int w, int h) throws IOException {
        java.awt.image.BufferedImage image = chart.createBufferedImage(w, h);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.jfree.chart.ChartUtils.writeBufferedImageAsPNG(baos, image);
        return baos.toByteArray();
    }

    // ─── PDF: Tabla de detalle con zebra y badges ────────────────────────────
    private void agregarTablaDetalle(Document document, DatosReporte datos) throws Exception {
        Paragraph sec = new Paragraph("Detalle de Solicitudes", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, GEA_GRIS_OSCURO));
        sec.setSpacingBefore(18f);
        sec.setSpacingAfter(8f);
        document.add(sec);

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1.5f, 3.5f, 2f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f });
        table.setHeaderRows(1);
        agregarHeader(table, "TIPO");
        agregarHeader(table, "TÍTULO");
        agregarHeader(table, "ESTADO");
        agregarHeader(table, "CREADO EL");
        agregarHeader(table, "REVISADO EL");
        agregarHeader(table, "FECHA EV/PUB");
        agregarHeader(table, "GESTOR");
        agregarHeader(table, "OFICINA/CAT");

        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int fila = 0;
        for (SolicitudEvento e : datos.eventos()) {
            java.awt.Color bg = (fila++ % 2 == 0) ? java.awt.Color.WHITE : GEA_ZEBRA;
            table.addCell(celdaDetalle("EVENTO", bg));
            table.addCell(celdaDetalle(nullToEmpty(e.getNombreEvento()), bg));
            table.addCell(celdaBadge(String.valueOf(e.getEstado())));
            table.addCell(celdaDetalle(formatDateTimeSafe(e.getFechaCreacion(), dtf), bg));
            table.addCell(celdaDetalle(formatDateTimeSafe(e.getFechaRevision(), dtf), bg));
            table.addCell(celdaDetalle(formatDateSafe(e.getFechaEvento(), dtf), bg));
            table.addCell(celdaDetalle(e.getUsuarioRevisor() != null ? e.getUsuarioRevisor().getCorreo() : "N/A", bg));
            table.addCell(celdaDetalle(e.getOficina() != null ? nullToEmpty(e.getOficina().getNombre()) : "N/A", bg));
        }
        for (SolicitudAnuncio a : datos.anuncios()) {
            java.awt.Color bg = (fila++ % 2 == 0) ? java.awt.Color.WHITE : GEA_ZEBRA;
            table.addCell(celdaDetalle("ANUNCIO", bg));
            table.addCell(celdaDetalle(nullToEmpty(a.getTitulo()), bg));
            table.addCell(celdaBadge(String.valueOf(a.getEstado())));
            table.addCell(celdaDetalle(formatDateTimeSafe(a.getFechaCreacion(), dtf), bg));
            table.addCell(celdaDetalle(formatDateTimeSafe(a.getFechaRevision(), dtf), bg));
            table.addCell(celdaDetalle(formatDateSafe(a.getFechaInicioPublicacion(), dtf), bg));
            table.addCell(celdaDetalle(a.getUsuarioRevisor() != null ? a.getUsuarioRevisor().getCorreo() : "N/A", bg));
            String of = a.getOficina() != null ? a.getOficina().getNombre() : (a.getCategoria() != null ? a.getCategoria() : "N/A");
            table.addCell(celdaDetalle(of, bg));
        }
        if (datos.eventos().isEmpty() && datos.anuncios().isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("No hay solicitudes en el periodo seleccionado",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, java.awt.Color.GRAY)));
            vacio.setColspan(8);
            vacio.setPadding(16);
            vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(vacio);
        }
        document.add(table);
    }

    private PdfPCell celdaDetalle(String text, java.awt.Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 8, GEA_GRIS_OSCURO)));
        cell.setPadding(4);
        cell.setBackgroundColor(bg);
        cell.setBorderColor(GEA_BORDE);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell celdaBadge(String estado) {
        PdfPCell cell = new PdfPCell(new Phrase(estado, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, java.awt.Color.WHITE)));
        cell.setBackgroundColor(colorEstado(estado));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(java.awt.Color.WHITE);
        return cell;
    }

    private java.awt.Color colorEstado(String estado) {
        if (estado == null) return java.awt.Color.decode("#64748b");
        switch (estado.toUpperCase()) {
            case "APROBADA": return java.awt.Color.decode("#16a34a");
            case "PENDIENTE": return java.awt.Color.decode("#f59e0b");
            case "RECHAZADA": return GEA_ROJO;
            case "PUBLICADA": return java.awt.Color.decode("#0ea5e9");
            default: return java.awt.Color.decode("#64748b");
        }
    }

    // ─── PDF: Pie de página con paginación ───────────────────────────────────
    private static class PieDePagina extends PdfPageEventHelper {
        private final com.lowagie.text.Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, java.awt.Color.GRAY);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfPTable footer = new PdfPTable(2);
                footer.setWidths(new int[] { 6, 1 });
                footer.setTotalWidth(document.right() - document.left());
                PdfPCell left = new PdfPCell(new Phrase("GEA — Reporte generado automáticamente", footerFont));
                left.setBorder(Rectangle.TOP);
                left.setBorderColor(GEA_BORDE);
                left.setPadding(4);
                PdfPCell right = new PdfPCell(new Phrase("Página " + writer.getPageNumber(), footerFont));
                right.setBorder(Rectangle.TOP);
                right.setBorderColor(GEA_BORDE);
                right.setHorizontalAlignment(Element.ALIGN_RIGHT);
                right.setPadding(4);
                footer.addCell(left);
                footer.addCell(right);
                footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 2, writer.getDirectContent());
            } catch (Exception ex) {
                // No interrumpir la generación por un fallo en el pie de página
            }
        }
    }

    // ─── XLSX: Generador principal ───────────────────────────────────────────
    private byte[] exportarXlsxInterno(DatosReporte datos) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Reporte GEA");

            // Pre-construir estilos (reutilizados en todos los ciclos)
            XSSFCellStyle estMarca     = xlsxEstiloMarca(wb);
            XSSFCellStyle estSubtitulo = xlsxEstiloSubtitulo(wb);
            XSSFCellStyle[] estKpiLbl  = xlsxEstilosKpiLabel(wb);
            XSSFCellStyle[] estKpiVal  = xlsxEstilosKpiValor(wb);
            XSSFCellStyle estHeader    = xlsxEstiloHeader(wb);
            XSSFCellStyle estPar       = xlsxEstiloDato(wb, false);
            XSSFCellStyle estImpar     = xlsxEstiloDato(wb, true);
            XSSFCellStyle estAprobada  = xlsxEstiloStatus(wb, "APROBADA");
            XSSFCellStyle estPendiente = xlsxEstiloStatus(wb, "PENDIENTE");
            XSSFCellStyle estRechazada = xlsxEstiloStatus(wb, "RECHAZADA");
            XSSFCellStyle estPublicada = xlsxEstiloStatus(wb, "PUBLICADA");
            XSSFCellStyle estStatusDef = xlsxEstiloStatus(wb, "");

            int ri = 0;

            // ── Cabecera de marca ──────────────────────────────────────────
            {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(ri++);
                row.setHeightInPoints(38);
                XSSFCell cell = (XSSFCell) row.createCell(0);
                cell.setCellValue("GEA — Sistema de Gestión de Eventos y Anuncios");
                cell.setCellStyle(estMarca);
                sheet.addMergedRegion(new CellRangeAddress(ri - 1, ri - 1, 0, 7));
            }

            // ── Subtítulo con alcance y fecha ──────────────────────────────
            {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(ri++);
                row.setHeightInPoints(20);
                XSSFCell cell = (XSSFCell) row.createCell(0);
                cell.setCellValue("Reporte de Auditoría  ·  Alcance: " + datos.alcance()
                        + "  ·  Generado: " + LocalDateTime.now().withNano(0));
                cell.setCellStyle(estSubtitulo);
                sheet.addMergedRegion(new CellRangeAddress(ri - 1, ri - 1, 0, 7));
            }

            ri++; // fila en blanco
            sheet.createRow(ri - 1);

            // ── KPIs ──────────────────────────────────────────────────────
            long total = datos.eventos().size() + datos.anuncios().size();
            long aprob = contarEventos(datos.eventos(), EstadoSolicitud.APROBADA)
                       + contarEventos(datos.eventos(), EstadoSolicitud.PUBLICADA)
                       + contarAnuncios(datos.anuncios(), EstadoSolicitud.APROBADA)
                       + contarAnuncios(datos.anuncios(), EstadoSolicitud.PUBLICADA);
            long pend  = contarEventos(datos.eventos(), EstadoSolicitud.PENDIENTE)
                       + contarAnuncios(datos.anuncios(), EstadoSolicitud.PENDIENTE);
            long rech  = contarEventos(datos.eventos(), EstadoSolicitud.RECHAZADA)
                       + contarAnuncios(datos.anuncios(), EstadoSolicitud.RECHAZADA);
            double tasa = total > 0 ? (double) aprob / total * 100 : 0;

            String[] kpiLabels = { "TOTAL", "APROBADAS", "PENDIENTES", "RECHAZADAS", "TASA APROBACIÓN" };
            String[] kpiValues = { String.valueOf(total), String.valueOf(aprob),
                                   String.valueOf(pend), String.valueOf(rech), Math.round(tasa) + "%" };

            {
                org.apache.poi.ss.usermodel.Row lblRow = sheet.createRow(ri++);
                org.apache.poi.ss.usermodel.Row valRow = sheet.createRow(ri++);
                lblRow.setHeightInPoints(16);
                valRow.setHeightInPoints(34);
                for (int i = 0; i < kpiLabels.length; i++) {
                    XSSFCell lbl = (XSSFCell) lblRow.createCell(i);
                    lbl.setCellValue(kpiLabels[i]);
                    lbl.setCellStyle(estKpiLbl[i]);
                    XSSFCell val = (XSSFCell) valRow.createCell(i);
                    val.setCellValue(kpiValues[i]);
                    val.setCellStyle(estKpiVal[i]);
                }
            }

            ri++; // fila en blanco
            sheet.createRow(ri - 1);

            // ── Encabezado de tabla ────────────────────────────────────────
            String[] headers = { "TIPO", "TÍTULO", "ESTADO", "CREADO EL", "REVISADO EL",
                                  "FECHA EV/PUB", "GESTOR", "OFICINA / CAT." };
            int tableStartRow = ri;
            {
                org.apache.poi.ss.usermodel.Row hdrRow = sheet.createRow(ri++);
                hdrRow.setHeightInPoints(20);
                for (int i = 0; i < headers.length; i++) {
                    XSSFCell cell = (XSSFCell) hdrRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(estHeader);
                }
            }

            // ── Filas de datos ─────────────────────────────────────────────
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
            int filaIdx = 0;

            for (SolicitudEvento e : datos.eventos()) {
                XSSFCellStyle[] rowStyles = (filaIdx++ % 2 == 0)
                        ? new XSSFCellStyle[]{ estPar, estPar, null, estPar, estPar, estPar, estPar, estPar }
                        : new XSSFCellStyle[]{ estImpar, estImpar, null, estImpar, estImpar, estImpar, estImpar, estImpar };
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(ri++);
                row.setHeightInPoints(18);
                xlsxCelda(row, 0, "EVENTO",                                             rowStyles[0]);
                xlsxCelda(row, 1, nullToEmpty(e.getNombreEvento()),                      rowStyles[1]);
                xlsxCelda(row, 2, String.valueOf(e.getEstado()),                         xlsxStatusStyle(e.getEstado(), estAprobada, estPendiente, estRechazada, estPublicada, estStatusDef));
                xlsxCelda(row, 3, formatDateTimeSafe(e.getFechaCreacion(), dtf),         rowStyles[3]);
                xlsxCelda(row, 4, formatDateTimeSafe(e.getFechaRevision(), dtf),         rowStyles[4]);
                xlsxCelda(row, 5, formatDateSafe(e.getFechaEvento(), dtf),               rowStyles[5]);
                xlsxCelda(row, 6, e.getUsuarioRevisor() != null ? e.getUsuarioRevisor().getCorreo() : "N/A", rowStyles[6]);
                xlsxCelda(row, 7, e.getOficina() != null ? nullToEmpty(e.getOficina().getNombre()) : "N/A", rowStyles[7]);
            }

            for (SolicitudAnuncio a : datos.anuncios()) {
                XSSFCellStyle[] rowStyles = (filaIdx++ % 2 == 0)
                        ? new XSSFCellStyle[]{ estPar, estPar, null, estPar, estPar, estPar, estPar, estPar }
                        : new XSSFCellStyle[]{ estImpar, estImpar, null, estImpar, estImpar, estImpar, estImpar, estImpar };
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(ri++);
                row.setHeightInPoints(18);
                String ofNombre = a.getOficina() != null ? a.getOficina().getNombre()
                                : (a.getCategoria() != null ? a.getCategoria() : "N/A");
                xlsxCelda(row, 0, "ANUNCIO",                                              rowStyles[0]);
                xlsxCelda(row, 1, nullToEmpty(a.getTitulo()),                             rowStyles[1]);
                xlsxCelda(row, 2, String.valueOf(a.getEstado()),                          xlsxStatusStyle(a.getEstado(), estAprobada, estPendiente, estRechazada, estPublicada, estStatusDef));
                xlsxCelda(row, 3, formatDateTimeSafe(a.getFechaCreacion(), dtf),          rowStyles[3]);
                xlsxCelda(row, 4, formatDateTimeSafe(a.getFechaRevision(), dtf),          rowStyles[4]);
                xlsxCelda(row, 5, formatDateSafe(a.getFechaInicioPublicacion(), dtf),     rowStyles[5]);
                xlsxCelda(row, 6, a.getUsuarioRevisor() != null ? a.getUsuarioRevisor().getCorreo() : "N/A", rowStyles[6]);
                xlsxCelda(row, 7, ofNombre,                                               rowStyles[7]);
            }

            // ── Freeze pane sobre la fila de encabezado ────────────────────
            sheet.createFreezePane(0, tableStartRow + 1);

            // ── AutoFilter sobre los encabezados ───────────────────────────
            sheet.setAutoFilter(new CellRangeAddress(tableStartRow, tableStartRow, 0, headers.length - 1));

            // ── Ajuste automático de columnas ──────────────────────────────
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3500) sheet.setColumnWidth(i, 3500);
                if (sheet.getColumnWidth(i) > 16000) sheet.setColumnWidth(i, 16000);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible generar el Excel");
        }
    }

    // ─── XLSX: Helpers de celdas y estilos ───────────────────────────────────
    private void xlsxCelda(org.apache.poi.ss.usermodel.Row row, int col, String value, XSSFCellStyle style) {
        XSSFCell cell = (XSSFCell) row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        if (style != null) cell.setCellStyle(style);
    }

    private XSSFCellStyle xlsxStatusStyle(EstadoSolicitud estado,
            XSSFCellStyle estAprobada, XSSFCellStyle estPendiente,
            XSSFCellStyle estRechazada, XSSFCellStyle estPublicada, XSSFCellStyle estDefault) {
        if (estado == null) return estDefault;
        return switch (estado) {
            case APROBADA  -> estAprobada;
            case PENDIENTE -> estPendiente;
            case RECHAZADA -> estRechazada;
            case PUBLICADA -> estPublicada;
            default        -> estDefault;
        };
    }

    private XSSFColor xlsxHex(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new XSSFColor(new byte[]{ (byte) r, (byte) g, (byte) b }, null);
    }

    private XSSFCellStyle xlsxEstiloMarca(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 17);
        f.setColor(xlsxHex("FFFFFF"));
        s.setFont(f);
        s.setFillForegroundColor(xlsxHex("CE1126"));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private XSSFCellStyle xlsxEstiloSubtitulo(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(xlsxHex("475569"));
        s.setFont(f);
        s.setFillForegroundColor(xlsxHex("f1f5f9"));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private static final String[] KPI_HEX = { "334155", "16a34a", "f59e0b", "CE1126", "0ea5e9" };

    private XSSFCellStyle[] xlsxEstilosKpiLabel(XSSFWorkbook wb) {
        XSSFCellStyle[] styles = new XSSFCellStyle[KPI_HEX.length];
        for (int i = 0; i < KPI_HEX.length; i++) {
            XSSFCellStyle s = wb.createCellStyle();
            XSSFFont f = wb.createFont();
            f.setBold(true);
            f.setFontHeightInPoints((short) 8);
            f.setColor(xlsxHex(KPI_HEX[i]));
            s.setFont(f);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.BOTTOM);
            styles[i] = s;
        }
        return styles;
    }

    private XSSFCellStyle[] xlsxEstilosKpiValor(XSSFWorkbook wb) {
        XSSFCellStyle[] styles = new XSSFCellStyle[KPI_HEX.length];
        for (int i = 0; i < KPI_HEX.length; i++) {
            XSSFCellStyle s = wb.createCellStyle();
            XSSFFont f = wb.createFont();
            f.setBold(true);
            f.setFontHeightInPoints((short) 18);
            f.setColor(xlsxHex(KPI_HEX[i]));
            s.setFont(f);
            s.setFillForegroundColor(xlsxHex(KPI_HEX[i]));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            // Tint the background (light pastel) — we override fill with white-tinted color
            // To achieve tint: blend color with white at 88%
            int r = Integer.parseInt(KPI_HEX[i].substring(0, 2), 16);
            int g = Integer.parseInt(KPI_HEX[i].substring(2, 4), 16);
            int bv = Integer.parseInt(KPI_HEX[i].substring(4, 6), 16);
            int tr = Math.min(255, r + (int)((255 - r) * 0.88));
            int tg = Math.min(255, g + (int)((255 - g) * 0.88));
            int tb = Math.min(255, bv + (int)((255 - bv) * 0.88));
            s.setFillForegroundColor(new XSSFColor(new byte[]{ (byte) tr, (byte) tg, (byte) tb }, null));
            styles[i] = s;
        }
        return styles;
    }

    private XSSFCellStyle xlsxEstiloHeader(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 9);
        f.setColor(xlsxHex("FFFFFF"));
        s.setFont(f);
        s.setFillForegroundColor(xlsxHex("CE1126"));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private XSSFCellStyle xlsxEstiloDato(XSSFWorkbook wb, boolean zebra) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 9);
        f.setColor(xlsxHex("334155"));
        s.setFont(f);
        if (zebra) {
            s.setFillForegroundColor(xlsxHex("f8fafc"));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private XSSFCellStyle xlsxEstiloStatus(XSSFWorkbook wb, String estado) {
        String bgHex = switch (estado.toUpperCase()) {
            case "APROBADA"  -> "16a34a";
            case "PENDIENTE" -> "f59e0b";
            case "RECHAZADA" -> "CE1126";
            case "PUBLICADA" -> "0ea5e9";
            default          -> "64748b";
        };
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 8);
        f.setColor(xlsxHex("FFFFFF"));
        s.setFont(f);
        s.setFillForegroundColor(xlsxHex(bgHex));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private DatosReporte obtenerDatosReporte(LocalDate desde, LocalDate hasta, String alcanceParam, Long idOficina, Long idTipoEvento, Authentication authentication) {
        validarFechas(desde, hasta);
        Usuario usuario = obtenerUsuario(authentication);
        LocalDateTime fechaDesde = desde.atStartOfDay();
        LocalDateTime fechaHasta = hasta.atTime(LocalTime.MAX);
        List<SolicitudEvento> eventos;
        List<SolicitudAnuncio> anuncios;
        String alcanceFinal = alcanceParam != null ? alcanceParam.toUpperCase() : "GLOBAL";

        if (usuario.getRol().tieneAccesoGlobal()) {
            if ("ANUNCIOS".equals(alcanceFinal)) {
                eventos = List.of();
            } else if (idOficina != null && idTipoEvento != null) {
                eventos = solicitudEventoRepository.findByOficinaIdAndFechaCreacionBetweenAndTipoEventoCatalogoId(idOficina, fechaDesde, fechaHasta, idTipoEvento);
            } else if (idOficina != null) {
                eventos = solicitudEventoRepository.findByOficinaIdAndFechaCreacionBetween(idOficina, fechaDesde, fechaHasta);
            } else if (idTipoEvento != null) {
                eventos = solicitudEventoRepository.findByFechaCreacionBetweenAndTipoEventoCatalogoId(fechaDesde, fechaHasta, idTipoEvento);
            } else {
                eventos = solicitudEventoRepository.findByFechaCreacionBetween(fechaDesde, fechaHasta);
            }

            if ("EVENTOS".equals(alcanceFinal)) {
                anuncios = List.of();
            } else if (idOficina != null) {
                anuncios = solicitudAnuncioRepository.findByUsuarioSolicitanteOficinaIdAndFechaCreacionBetween(idOficina, fechaDesde, fechaHasta);
            } else {
                anuncios = solicitudAnuncioRepository.findByFechaCreacionBetween(fechaDesde, fechaHasta);
            }
        } else if (usuario.getRol().esOficina() || usuario.getRol().name().equals("USUARIO_AUTENTICADO_APP")) {
            Oficina oficina = usuario.getOficina();
            if (oficina == null) {
                eventos = solicitudEventoRepository.findByFechaCreacionBetween(fechaDesde, fechaHasta).stream().filter(e -> e.getUsuarioSolicitante().getId().equals(usuario.getId())).toList();
                anuncios = solicitudAnuncioRepository.findByFechaCreacionBetween(fechaDesde, fechaHasta).stream().filter(a -> a.getUsuarioSolicitante().getId().equals(usuario.getId())).toList();
                alcanceFinal = "PERSONAL";
            } else {
                if ("ANUNCIOS".equals(alcanceFinal)) {
                    eventos = List.of();
                } else if (idTipoEvento != null) {
                    eventos = solicitudEventoRepository.findByOficinaIdAndFechaCreacionBetweenAndTipoEventoCatalogoId(oficina.getId(), fechaDesde, fechaHasta, idTipoEvento);
                } else {
                    eventos = solicitudEventoRepository.findByOficinaIdAndFechaCreacionBetween(oficina.getId(), fechaDesde, fechaHasta);
                }
                if ("EVENTOS".equals(alcanceFinal)) {
                    anuncios = List.of();
                } else {
                    anuncios = solicitudAnuncioRepository.findByUsuarioSolicitanteOficinaIdAndFechaCreacionBetween(oficina.getId(), fechaDesde, fechaHasta);
                }
                alcanceFinal = "OFICINA";
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos");
        }
        return new DatosReporte(eventos, anuncios, alcanceFinal);
    }

    private long contarEventos(List<SolicitudEvento> eventos, EstadoSolicitud estado) {
        return eventos.stream().filter(evento -> evento.getEstado() == estado).count();
    }

    private long contarAnuncios(List<SolicitudAnuncio> anuncios, EstadoSolicitud estado) {
        return anuncios.stream().filter(anuncio -> anuncio.getEstado() == estado).count();
    }

    private Usuario obtenerUsuario(Authentication authentication) {
        return usuarioRepository.getByCorreoOptimized(authentication.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private void validarFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar fechas");
        if (hasta.isBefore(desde)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha inválida");
    }

    private String normalizarFormato(String formato) {
        if (formato == null || formato.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato requerido");
        String normalizado = formato.trim().toUpperCase();
        if (!normalizado.equals("PDF") && !normalizado.equals("XLSX")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato no válido");
        return normalizado;
    }

    private ReporteGenerado buscarReporte(Long reporteId, Authentication authentication) {
        Usuario usuario = obtenerUsuario(authentication);
        ReporteGenerado reporte = reporteGeneradoRepository.findById(Objects.requireNonNull(reporteId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrado"));
        if (usuario.getRol().tieneAccesoGlobal()) return reporte;
        boolean esMismoUsuario = reporte.getUsuarioGenerador().getId().equals(usuario.getId());
        boolean esMismaOficina = usuario.getOficina() != null && reporte.getUsuarioGenerador().getOficina() != null && usuario.getOficina().getId().equals(reporte.getUsuarioGenerador().getOficina().getId());
        if ((usuario.getRol().esOficina() || usuario.getRol().name().equals("USUARIO_AUTENTICADO_APP")) && (esMismoUsuario || esMismaOficina)) return reporte;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
    }

    @SuppressWarnings("null")
    @Transactional
    public ReporteGeneradoResponse actualizarReporte(Long id, ActualizarReporteRequest request, Authentication authentication) {
        ReporteGenerado reporte = buscarReporte(id, authentication);
        if (request.getNombre() != null && !request.getNombre().isBlank()) reporte.setNombre(request.getNombre());
        if (request.getDescripcion() != null) reporte.setDescripcion(request.getDescripcion());
        return toReporteGeneradoResponse(reporteGeneradoRepository.save(reporte));
    }

    private ReporteGeneradoResponse toReporteGeneradoResponse(ReporteGenerado reporte) {
        return ReporteGeneradoResponse.builder()
                .id(reporte.getId())
                .nombre(reporte.getNombre())
                .descripcion(reporte.getDescripcion())
                .formato(reporte.getFormato())
                .desde(reporte.getFechaDesde())
                .hasta(reporte.getFechaHasta())
                .alcance(reporte.getAlcance())
                .fechaCreacion(reporte.getFechaCreacion())
                .usuarioGeneradorId(reporte.getUsuarioGenerador() != null ? reporte.getUsuarioGenerador().getId() : null)
                .usuarioGeneradorCorreo(reporte.getUsuarioGenerador() != null ? reporte.getUsuarioGenerador().getCorreo() : "SISTEMA")
                .usuarioGeneradorOficina(reporte.getUsuarioGenerador() != null && reporte.getUsuarioGenerador().getOficina() != null ? reporte.getUsuarioGenerador().getOficina().getNombre() : "N/A")
                .idOficina(reporte.getIdOficina())
                .idTipoEvento(reporte.getIdTipoEvento())
                .build();
    }

    private void agregarHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE)));
        cell.setBackgroundColor(java.awt.Color.decode("#CE1126"));
        cell.setPadding(6);
        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatDateSafe(LocalDate date, java.time.format.DateTimeFormatter dtf) {
        return date != null ? date.format(dtf) : "-";
    }

    private String formatDateTimeSafe(LocalDateTime dateTime, java.time.format.DateTimeFormatter dtf) {
        return dateTime != null ? dateTime.format(dtf) : "-";
    }

    private record DatosReporte(List<SolicitudEvento> eventos, List<SolicitudAnuncio> anuncios, String alcance) {}

    private SolicitudResumenDTO mapToResumen(SolicitudEvento e) {
        return SolicitudResumenDTO.builder()
                .id(e.getId())
                .tipo("EVENTO")
                .titulo(e.getNombreEvento() != null ? e.getNombreEvento() : "Sin Título")
                .oficina(e.getOficina() != null ? e.getOficina().getNombre() : "N/A")
                .fechaRegistro(e.getFechaCreacion() != null ? e.getFechaCreacion().toString().split("T")[0] : "-")
                .estado(e.getEstado() != null ? e.getEstado().toString() : "PENDIENTE")
                .build();
    }

    private SolicitudResumenDTO mapToResumen(SolicitudAnuncio a) {
        return SolicitudResumenDTO.builder()
                .id(a.getId())
                .tipo("ANUNCIO")
                .titulo(a.getTitulo() != null ? a.getTitulo() : "Sin Título")
                .oficina(a.getOficina() != null ? a.getOficina().getNombre() : "N/A")
                .fechaRegistro(a.getFechaCreacion() != null ? a.getFechaCreacion().toString().split("T")[0] : "-")
                .estado(a.getEstado() != null ? a.getEstado().toString() : "PENDIENTE")
                .build();
    }
}

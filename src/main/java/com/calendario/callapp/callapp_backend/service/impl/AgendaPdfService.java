package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.entity.LugarFisico;
import com.calendario.callapp.callapp_backend.entity.PublicacionEvento;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import com.calendario.callapp.callapp_backend.entity.SolicitudEventoParticipante;
import com.calendario.callapp.callapp_backend.entity.TipoEventoCatalogo;
import com.calendario.callapp.callapp_backend.entity.TipoParticipante;
import com.calendario.callapp.callapp_backend.repository.PublicacionEventoRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * Generación de agendas en PDF con la paleta de marca GEA usando OpenPDF (com.lowagie).
 *
 * <p>Consulta las publicaciones de evento en un rango de fechas y las agrupa por día
 * en una tabla multicolumna. Los colores corporativos se definen como constantes estáticas
 * ({@code GEA_ROJO}, {@code GEA_GRIS_OSCURO}, etc.) para mantener coherencia visual.
 * El PDF resultante se devuelve como {@code byte[]} en memoria para que el controller
 * lo sirva directamente sin escribir en disco.</p>
 */
public class AgendaPdfService {

    private final PublicacionEventoRepository publicacionEventoRepository;

    // ─── Paleta de marca GEA ────────────────────────────────────────────────
    private static final java.awt.Color GEA_ROJO       = java.awt.Color.decode("#CE1126");
    private static final java.awt.Color GEA_GRIS_OSCURO = java.awt.Color.decode("#1e293b");
    private static final java.awt.Color GEA_GRIS_CLARO  = java.awt.Color.decode("#f8fafc");
    private static final java.awt.Color GEA_TEXTO       = java.awt.Color.decode("#334155");
    private static final java.awt.Color GEA_BORDE       = java.awt.Color.decode("#e2e8f0");

    private static final Locale LOCALE_ES = new Locale("es", "CO");

    // ─── Punto de entrada ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] exportarAgendaPdf(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las fechas son requeridas");
        if (hasta.isBefore(desde))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha 'hasta' no puede ser anterior a 'desde'");
        if (ChronoUnit.DAYS.between(desde, hasta) > 366)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango no puede superar 366 días");

        // findPublicadasEnRango usa fin exclusivo → plusDays(1) hace 'hasta' inclusivo
        List<PublicacionEvento> pubs = publicacionEventoRepository.findPublicadasEnRango(desde, hasta.plusDays(1));

        // Agrupar por fecha conservando el orden de la consulta (esImportante DESC, fecha ASC, hora ASC)
        Map<LocalDate, List<PublicacionEvento>> porDia = new LinkedHashMap<>();
        for (PublicacionEvento p : pubs) {
            porDia.computeIfAbsent(p.getSolicitudEvento().getFechaEvento(), k -> new ArrayList<>()).add(p);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 58);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PieDeAgenda());
            document.open();

            agregarEncabezado(document, desde, hasta);

            if (porDia.isEmpty()) {
                agregarSinEventos(document);
            } else {
                for (Map.Entry<LocalDate, List<PublicacionEvento>> entry : porDia.entrySet()) {
                    agregarBloqueDia(document, entry.getKey(), entry.getValue());
                }
            }

            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            log.error("Error generando PDF de agenda: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar la agenda PDF");
        }
    }

    // ─── Encabezado de marca ─────────────────────────────────────────────────
    private void agregarEncabezado(Document doc, LocalDate desde, LocalDate hasta) throws Exception {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{ 1f, 4.5f });
        headerTable.setSpacingAfter(4f);

        // Columna izquierda: logo tipográfico GEA
        PdfPCell logoCell = new PdfPCell(new Phrase("GEA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, GEA_ROJO)));
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(logoCell);

        // Columna derecha: título + rango de fechas
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph agendaTitulo = new Paragraph("Agenda",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, GEA_GRIS_OSCURO));
        agendaTitulo.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(agendaTitulo);

        DateTimeFormatter dfRango = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_ES);
        String rango;
        if (desde.getYear() == hasta.getYear()) {
            rango = "del " + desde.format(dfRango) + " al " + hasta.format(dfRango) + " de " + hasta.getYear();
        } else {
            DateTimeFormatter dfFull = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", LOCALE_ES);
            rango = "del " + desde.format(dfFull) + " al " + hasta.format(dfFull);
        }
        Paragraph subtitulo = new Paragraph(rango,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GEA_TEXTO));
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(subtitulo);

        headerTable.addCell(titleCell);
        doc.add(headerTable);

        // Línea separadora roja
        com.lowagie.text.pdf.draw.LineSeparator linea =
                new com.lowagie.text.pdf.draw.LineSeparator(2f, 100, GEA_ROJO, Element.ALIGN_CENTER, -6);
        doc.add(new Chunk(linea));
    }

    // ─── Bloque de un día ────────────────────────────────────────────────────
    private void agregarBloqueDia(Document doc, LocalDate fecha, List<PublicacionEvento> eventos) throws Exception {
        DateTimeFormatter dfDia = DateTimeFormatter.ofPattern("EEEE", LOCALE_ES);
        DateTimeFormatter dfMes = DateTimeFormatter.ofPattern("MMMM", LOCALE_ES);

        String encabezadoDia = dfDia.format(fecha).toUpperCase(LOCALE_ES)
                + " / " + fecha.getDayOfMonth()
                + " DE " + dfMes.format(fecha).toUpperCase(LOCALE_ES);

        Paragraph diaParrafo = new Paragraph(encabezadoDia,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GEA_ROJO));
        diaParrafo.setSpacingBefore(16f);
        diaParrafo.setSpacingAfter(4f);
        doc.add(diaParrafo);

        for (PublicacionEvento pub : eventos) {
            agregarEntradaEvento(doc, pub);
        }
    }

    // ─── Entrada individual de evento ────────────────────────────────────────
    private void agregarEntradaEvento(Document doc, PublicacionEvento pub) throws Exception {
        SolicitudEvento sol = pub.getSolicitudEvento();
        TipoEventoCatalogo tipo = sol.getTipoEventoCatalogo();
        java.awt.Color colorEvento = parseColor(tipo != null ? tipo.getColorHex() : null);

        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(96);
        tabla.setSpacingBefore(3f);
        tabla.setSpacingAfter(3f);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell cell = new PdfPCell();
        // Solo borde izquierdo con el color del tipo de evento
        cell.setBorderWidthTop(0);
        cell.setBorderWidthBottom(0);
        cell.setBorderWidthRight(0);
        cell.setBorderWidthLeft(4f);
        cell.setBorderColorLeft(colorEvento);
        cell.setPadding(8f);
        cell.setPaddingLeft(12f);
        cell.setBackgroundColor(GEA_GRIS_CLARO);

        // Badge de oficina (">> Nombre Oficina")
        String oficina = sol.getOficina() != null ? sol.getOficina().getNombre() : "GEA";
        Paragraph badgePara = new Paragraph(">>  " + oficina,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, colorEvento));
        badgePara.setSpacingAfter(3f);
        cell.addElement(badgePara);

        // Título del evento (con ★ si es importante)
        String titulo = pub.getTituloVisible() != null && !pub.getTituloVisible().isBlank()
                ? pub.getTituloVisible()
                : sol.getNombreEvento();
        if (Boolean.TRUE.equals(sol.getEsImportante())) titulo = "★ " + titulo;

        Paragraph tituloPara = new Paragraph(titulo,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GEA_GRIS_OSCURO));
        tituloPara.setSpacingAfter(4f);
        cell.addElement(tituloPara);

        // Descripción del evento (si tiene)
        String descripcion = sol.getDescripcionEvento();
        if (descripcion != null && !descripcion.isBlank()) {
            Paragraph descPara = new Paragraph(descripcion,
                    FontFactory.getFont(FontFactory.HELVETICA, 9, GEA_TEXTO));
            descPara.setSpacingAfter(4f);
            cell.addElement(descPara);
        }

        // Organizador(es): participantes con tipo ORGANIZADOR
        String organizadores = sol.getParticipantes().stream()
                .filter(p -> p.getTipo() == TipoParticipante.ORGANIZADOR)
                .map(SolicitudEventoParticipante::getNombre)
                .collect(Collectors.joining(", "));
        if (!organizadores.isBlank()) {
            Paragraph orgPara = new Paragraph("Organiza: " + organizadores,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, GEA_TEXTO));
            orgPara.setSpacingAfter(4f);
            cell.addElement(orgPara);
        }

        // Hora y Lugar en una sola línea
        StringBuilder metaSb = new StringBuilder();
        if (sol.getHoraInicio() != null) {
            metaSb.append("Hora: ").append(formatHora12h(sol.getHoraInicio()));
            if (sol.getHoraFin() != null) {
                metaSb.append(" a ").append(formatHora12h(sol.getHoraFin()));
            }
        }

        // LugarFisico.esExterno=true es la entrada placeholder del catálogo
        // ("Externo") que solo marca "este evento es fuera del campus" — no es
        // un nombre de lugar real, así que no debe imprimirse junto a los
        // lugares reales. El lugar real ya viene en ubicacionExterna.
        String lugarStr = sol.getLugaresFisicos().stream()
                .filter(l -> !Boolean.TRUE.equals(l.getEsExterno()))
                .map(LugarFisico::getNombre)
                .collect(Collectors.joining(", "));

        String ubicExt = sol.getUbicacionExterna();
        if (ubicExt != null && !ubicExt.isBlank()) {
            lugarStr = lugarStr.isEmpty() ? ubicExt : lugarStr + ", " + ubicExt;
        }

        if (!lugarStr.isEmpty()) {
            if (metaSb.length() > 0) metaSb.append("    ");
            metaSb.append("Lugar: ").append(lugarStr);
        }

        if (metaSb.length() > 0) {
            Paragraph metaPara = new Paragraph(metaSb.toString(),
                    FontFactory.getFont(FontFactory.HELVETICA, 9, GEA_TEXTO));
            cell.addElement(metaPara);
        }

        tabla.addCell(cell);
        doc.add(tabla);
    }

    // ─── Placeholder sin eventos ─────────────────────────────────────────────
    private void agregarSinEventos(Document doc) throws Exception {
        Paragraph p = new Paragraph(
                "No hay eventos publicados en el periodo seleccionado.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11, java.awt.Color.GRAY));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(40f);
        doc.add(p);
    }

    // ─── Pie de página paginado ──────────────────────────────────────────────
    private static class PieDeAgenda extends PdfPageEventHelper {
        private final com.lowagie.text.Font footerFont =
                FontFactory.getFont(FontFactory.HELVETICA, 8, java.awt.Color.GRAY);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfPTable footer = new PdfPTable(2);
                footer.setWidths(new int[]{ 6, 1 });
                footer.setTotalWidth(document.right() - document.left());

                PdfPCell left = new PdfPCell(new Phrase("GEA — Agenda Institucional", footerFont));
                left.setBorder(Rectangle.TOP);
                left.setBorderColor(java.awt.Color.decode("#e2e8f0"));
                left.setPadding(4);

                PdfPCell right = new PdfPCell(new Phrase("Página " + writer.getPageNumber(), footerFont));
                right.setBorder(Rectangle.TOP);
                right.setBorderColor(java.awt.Color.decode("#e2e8f0"));
                right.setHorizontalAlignment(Element.ALIGN_RIGHT);
                right.setPadding(4);

                footer.addCell(left);
                footer.addCell(right);
                footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 2, writer.getDirectContent());
            } catch (Exception ignored) {}
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private String formatHora12h(LocalTime hora) {
        int h = hora.getHour();
        int m = hora.getMinute();
        String ap = h >= 12 ? "p.m." : "a.m.";
        int h12 = h % 12;
        if (h12 == 0) h12 = 12;
        return h12 + ":" + String.format("%02d", m) + " " + ap;
    }

    private java.awt.Color parseColor(String hex) {
        if (hex == null || hex.isBlank()) return GEA_ROJO;
        try {
            String h = hex.trim();
            if (!h.startsWith("#")) h = "#" + h;
            return java.awt.Color.decode(h);
        } catch (Exception e) {
            return GEA_ROJO;
        }
    }
}

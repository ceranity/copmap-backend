package in.copmap.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import in.copmap.domain.Assignment;
import in.copmap.domain.Bandobast;
import in.copmap.domain.Patrol;
import in.copmap.exception.ApiException;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Minimal PDF generator using OpenPDF. Produces two artefacts:
 *   - Bandobast/Nakabandi deployment plan (pre-operation briefing).
 *   - Patrol closure report (post-operation — roster, checkpoints, closure notes).
 */
@Service
public class PdfService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").withZone(ZoneId.of("Asia/Kolkata"));

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
    private static final Font H2    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
    private static final Font BODY  = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

    public byte[] patrolReport(Patrol p, List<Assignment> assignments) {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("CopMap — Patrol Report", TITLE));
            doc.add(new Paragraph("Generated: " + FMT.format(java.time.Instant.now()), SMALL));
            doc.add(new Paragraph(" "));

            doc.add(row("Title", p.getTitle()));
            doc.add(row("Beat", p.getBeatName()));
            doc.add(row("Status", String.valueOf(p.getStatus())));
            doc.add(row("Window", FMT.format(p.getStartAt()) + "  →  " + FMT.format(p.getEndAt())));
            if (p.getClosedAt() != null) doc.add(row("Closed at", FMT.format(p.getClosedAt())));
            if (p.getDescription() != null) doc.add(row("Description", p.getDescription()));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Checkpoints", H2));
            PdfPTable cp = new PdfPTable(new float[]{1f, 4f, 3f, 2f, 2f});
            cp.setWidthPercentage(100);
            head(cp, "#", "Name", "Lat,Lng", "Radius (m)", "Due (min)");
            p.getCheckpoints().forEach(c -> {
                cell(cp, String.valueOf(c.getSequence()));
                cell(cp, c.getName());
                cell(cp, c.getLatitude() + ", " + c.getLongitude());
                cell(cp, String.valueOf(c.getRadiusMeters()));
                cell(cp, c.getDueOffsetMinutes() == null ? "-" : c.getDueOffsetMinutes().toString());
            });
            doc.add(cp);
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Roster", H2));
            doc.add(rosterTable(assignments));

            if (p.getClosureNotes() != null) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Closure notes", H2));
                doc.add(new Paragraph(p.getClosureNotes(), BODY));
            }
            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw ApiException.badRequest("PDF generation failed: " + e.getMessage());
        }
    }

    public byte[] bandobastPlan(Bandobast b, List<Assignment> assignments) {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("CopMap — " + b.getKind() + " Deployment Plan", TITLE));
            doc.add(new Paragraph("Generated: " + FMT.format(java.time.Instant.now()), SMALL));
            doc.add(new Paragraph(" "));

            doc.add(row("Title", b.getTitle()));
            doc.add(row("Venue", b.getVenue()));
            doc.add(row("Location", b.getLatitude() + ", " + b.getLongitude()));
            doc.add(row("Cordon radius", b.getRadiusMeters() + " m"));
            doc.add(row("Window", FMT.format(b.getStartAt()) + "  →  " + FMT.format(b.getEndAt())));
            doc.add(row("Expected crowd", b.getExpectedCrowd() == null ? "-" : b.getExpectedCrowd().toString()));
            doc.add(row("Threat level", b.getThreatLevel()));
            doc.add(row("Status", String.valueOf(b.getStatus())));
            if (b.getDescription() != null) doc.add(row("Briefing", b.getDescription()));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Roster", H2));
            doc.add(rosterTable(assignments));

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw ApiException.badRequest("PDF generation failed: " + e.getMessage());
        }
    }

    private PdfPTable rosterTable(List<Assignment> assignments) {
        PdfPTable t = new PdfPTable(new float[]{3f, 3f, 2f, 3f});
        t.setWidthPercentage(100);
        head(t, "Officer ID", "Role", "Status", "Assigned at");
        for (Assignment a : assignments) {
            cell(t, String.valueOf(a.getOfficerId()));
            cell(t, a.getRole() == null ? "-" : a.getRole());
            cell(t, String.valueOf(a.getStatus()));
            cell(t, FMT.format(a.getAssignedAt()));
        }
        return t;
    }

    private Paragraph row(String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", H2));
        p.add(new Chunk(value == null ? "-" : value, BODY));
        return p;
    }

    private void head(PdfPTable t, String... hs) {
        for (String h : hs) {
            PdfPCell c = new PdfPCell(new Phrase(h, H2));
            c.setBackgroundColor(new Color(230, 230, 230));
            t.addCell(c);
        }
    }

    private void cell(PdfPTable t, String s) { t.addCell(new PdfPCell(new Phrase(s == null ? "-" : s, BODY))); }
}

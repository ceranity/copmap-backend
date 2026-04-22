package in.copmap.web;

import in.copmap.domain.Assignment;
import in.copmap.domain.Bandobast;
import in.copmap.domain.OperationStatus;
import in.copmap.pdf.PdfService;
import in.copmap.security.UserPrincipal;
import in.copmap.service.BandobastService;
import in.copmap.web.dto.BandobastDtos.*;
import in.copmap.web.dto.PatrolDtos.AssignReq;
import in.copmap.web.dto.PatrolDtos.CloseReq;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bandobast")
public class BandobastController {

    private final BandobastService service;
    private final PdfService pdf;

    public BandobastController(BandobastService service, PdfService pdf) {
        this.service = service; this.pdf = pdf;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public Resp create(@Valid @RequestBody CreateReq r, @AuthenticationPrincipal UserPrincipal me) {
        Bandobast b = Bandobast.builder()
                .kind(r.kind()).title(r.title()).description(r.description())
                .venue(r.venue()).latitude(r.latitude()).longitude(r.longitude())
                .radiusMeters(r.radiusMeters())
                .startAt(r.startAt()).endAt(r.endAt())
                .expectedCrowd(r.expectedCrowd()).threatLevel(r.threatLevel())
                .stationId(r.stationId()).createdBy(me.id())
                .build();
        return Resp.of(service.create(b));
    }

    @GetMapping
    public List<Resp> list(@RequestParam(required = false) Bandobast.OperationKind kind) {
        List<Bandobast> found = kind == null ? service.list() : service.listByKind(kind);
        return found.stream().map(Resp::of).toList();
    }

    @GetMapping("/{id}")
    public Resp get(@PathVariable UUID id) { return Resp.of(service.get(id)); }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public Assignment assign(@PathVariable UUID id, @Valid @RequestBody AssignReq req,
                             @AuthenticationPrincipal UserPrincipal me) {
        return service.assignOfficer(id, req.officerId(), me.id(), req.role());
    }

    @GetMapping("/{id}/assignments")
    public List<Assignment> assignments(@PathVariable UUID id) { return service.listAssignments(id); }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public Resp start(@PathVariable UUID id) {
        return Resp.of(service.transition(id, OperationStatus.ACTIVE, null));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public Resp close(@PathVariable UUID id, @RequestBody(required = false) CloseReq req) {
        return Resp.of(service.transition(id, OperationStatus.CLOSED, req == null ? null : req.notes()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public Resp cancel(@PathVariable UUID id, @RequestBody(required = false) CloseReq req) {
        return Resp.of(service.transition(id, OperationStatus.CANCELLED, req == null ? null : req.notes()));
    }

    @GetMapping(value = "/{id}/plan", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> plan(@PathVariable UUID id) {
        byte[] pdfBytes = pdf.bandobastPlan(service.get(id), service.listAssignments(id));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=bandobast-" + id + ".pdf")
                .body(pdfBytes);
    }
}

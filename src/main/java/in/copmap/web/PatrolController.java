package in.copmap.web;

import in.copmap.domain.Assignment;
import in.copmap.domain.Checkpoint;
import in.copmap.domain.OperationStatus;
import in.copmap.domain.Patrol;
import in.copmap.pdf.PdfService;
import in.copmap.security.UserPrincipal;
import in.copmap.service.PatrolService;
import in.copmap.web.dto.PatrolDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patrols")
public class PatrolController {

    private final PatrolService service;
    private final PdfService pdf;

    public PatrolController(PatrolService service, PdfService pdf) {
        this.service = service; this.pdf = pdf;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public PatrolResp create(@Valid @RequestBody CreatePatrolReq req,
                             @AuthenticationPrincipal UserPrincipal me) {
        Patrol p = Patrol.builder()
                .title(req.title()).description(req.description()).beatName(req.beatName())
                .startAt(req.startAt()).endAt(req.endAt()).stationId(req.stationId())
                .createdBy(me.id())
                .build();
        p.setCheckpoints(req.checkpoints().stream().map(c -> Checkpoint.builder()
                .sequence(c.sequence()).name(c.name())
                .latitude(c.latitude()).longitude(c.longitude())
                .radiusMeters(c.radiusMeters()).dueOffsetMinutes(c.dueOffsetMinutes())
                .patrol(p).build()).collect(Collectors.toList()));
        return PatrolResp.of(service.create(p));
    }

    @GetMapping
    public List<PatrolResp> list(@RequestParam(required = false) OperationStatus status) {
        List<Patrol> found = status == null ? service.list() : service.listByStatus(status);
        return found.stream().map(PatrolResp::of).toList();
    }

    @GetMapping("/{id}")
    public PatrolResp get(@PathVariable UUID id) { return PatrolResp.of(service.get(id)); }

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
    public PatrolResp start(@PathVariable UUID id) {
        return PatrolResp.of(service.transition(id, OperationStatus.ACTIVE, null));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public PatrolResp close(@PathVariable UUID id, @RequestBody(required = false) CloseReq req) {
        return PatrolResp.of(service.transition(id, OperationStatus.CLOSED, req == null ? null : req.notes()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public PatrolResp cancel(@PathVariable UUID id, @RequestBody(required = false) CloseReq req) {
        return PatrolResp.of(service.transition(id, OperationStatus.CANCELLED, req == null ? null : req.notes()));
    }

    @GetMapping(value = "/{id}/report", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> report(@PathVariable UUID id) {
        byte[] pdfBytes = pdf.patrolReport(service.get(id), service.listAssignments(id));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=patrol-" + id + ".pdf")
                .body(pdfBytes);
    }
}

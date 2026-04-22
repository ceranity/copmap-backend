package in.copmap.web;

import in.copmap.domain.Alert;
import in.copmap.security.UserPrincipal;
import in.copmap.service.AlertService;
import in.copmap.web.dto.AlertDtos.RaiseReq;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService service;

    public AlertController(AlertService service) { this.service = service; }

    @PostMapping
    public Alert raise(@Valid @RequestBody RaiseReq r, @AuthenticationPrincipal UserPrincipal me) {
        Alert a = Alert.builder()
                .type(r.type())
                .severity(r.severity() == null ? Alert.Severity.WARNING : r.severity())
                .officerId(r.officerId() == null ? me.id() : r.officerId())
                .patrolId(r.patrolId())
                .bandobastId(r.bandobastId())
                .message(r.message())
                .latitude(r.latitude())
                .longitude(r.longitude())
                .build();
        return service.raise(a);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public Page<Alert> list(Pageable pageable) { return service.list(pageable); }

    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public Page<Alert> open(Pageable pageable) { return service.listOpen(pageable); }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public Alert acknowledge(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal me) {
        return service.acknowledge(id, me.id());
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public Alert resolve(@PathVariable UUID id) { return service.resolve(id); }
}

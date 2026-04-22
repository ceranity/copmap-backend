package in.copmap.web;

import in.copmap.domain.LocationPing;
import in.copmap.security.UserPrincipal;
import in.copmap.service.LiveLocation;
import in.copmap.service.LocationService;
import in.copmap.service.UserService;
import in.copmap.web.dto.LocationDtos.PingReq;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService service;
    private final UserService users;

    public LocationController(LocationService service, UserService users) {
        this.service = service; this.users = users;
    }

    /** Officer → server: emit a GPS ping. */
    @PostMapping("/ping")
    @PreAuthorize("hasRole('OFFICER')")
    public LocationPing ping(@Valid @RequestBody PingReq req,
                             @AuthenticationPrincipal UserPrincipal me) {
        LocationPing p = LocationPing.builder()
                .officerId(me.id())
                .assignmentId(req.assignmentId())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .accuracyMeters(req.accuracyMeters())
                .speedMps(req.speedMps())
                .batteryPct(req.batteryPct())
                .recordedAt(req.recordedAt() == null ? Instant.now() : req.recordedAt())
                .build();
        return service.record(p);
    }

    /** Planner map view — current position of every active officer. */
    @GetMapping("/live")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public List<LiveLocation> live() {
        List<UUID> ids = users.listOfficers().stream().map(u -> u.getId()).toList();
        return service.getLiveForOfficers(ids);
    }

    @GetMapping("/officers/{id}/history")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public List<LocationPing> history(@PathVariable UUID id,
                                       @RequestParam Instant from,
                                       @RequestParam Instant to) {
        return service.history(id, from, to);
    }
}

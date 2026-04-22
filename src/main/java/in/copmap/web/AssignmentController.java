package in.copmap.web;

import in.copmap.domain.Assignment;
import in.copmap.security.UserPrincipal;
import in.copmap.service.AssignmentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService service;

    public AssignmentController(AssignmentService service) { this.service = service; }

    @GetMapping("/mine")
    public List<Assignment> mine(@AuthenticationPrincipal UserPrincipal me) {
        return service.myAssignments(me.id());
    }

    @PostMapping("/{id}/acknowledge")
    public Assignment ack(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal me) {
        return service.acknowledge(id, me.id());
    }

    @PostMapping("/{id}/check-in")
    public Assignment checkIn(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal me) {
        return service.checkIn(id, me.id());
    }

    @PostMapping("/{id}/check-out")
    public Assignment checkOut(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal me) {
        return service.checkOut(id, me.id());
    }
}

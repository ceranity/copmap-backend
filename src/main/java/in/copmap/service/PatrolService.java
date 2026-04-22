package in.copmap.service;

import in.copmap.audit.AuditService;
import in.copmap.domain.*;
import in.copmap.exception.ApiException;
import in.copmap.repository.AssignmentRepository;
import in.copmap.repository.PatrolRepository;
import in.copmap.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PatrolService {

    private final PatrolRepository patrols;
    private final AssignmentRepository assignments;
    private final UserRepository users;
    private final AuditService audit;

    public PatrolService(PatrolRepository patrols, AssignmentRepository assignments,
                         UserRepository users, AuditService audit) {
        this.patrols = patrols; this.assignments = assignments;
        this.users = users; this.audit = audit;
    }

    @Transactional
    public Patrol create(Patrol p) {
        if (p.getEndAt().isBefore(p.getStartAt())) throw ApiException.badRequest("end_at must be after start_at");
        p.getCheckpoints().forEach(c -> c.setPatrol(p));
        Patrol saved = patrols.save(p);
        audit.record("CREATE", "Patrol", saved.getId(), "title=" + saved.getTitle());
        return saved;
    }

    public Patrol get(UUID id) { return patrols.findById(id).orElseThrow(() -> ApiException.notFound("Patrol")); }
    public List<Patrol> list() { return patrols.findAll(); }
    public List<Patrol> listByStatus(OperationStatus s) { return patrols.findByStatus(s); }

    @Transactional
    public Assignment assignOfficer(UUID patrolId, UUID officerId, UUID plannerId, String role) {
        Patrol p = get(patrolId);
        if (p.getStatus() == OperationStatus.CLOSED || p.getStatus() == OperationStatus.CANCELLED)
            throw ApiException.badRequest("Cannot assign to a closed/cancelled patrol");

        User officer = users.findById(officerId).orElseThrow(() -> ApiException.notFound("Officer"));
        if (officer.getRole() != Role.OFFICER) throw ApiException.badRequest("Target user is not an OFFICER");
        if (assignments.existsByOfficerIdAndPatrolId(officerId, patrolId))
            throw ApiException.conflict("Officer already assigned to this patrol");

        Assignment a = Assignment.builder()
                .officerId(officerId)
                .patrolId(patrolId)
                .role(role)
                .status(Assignment.AssignmentStatus.ASSIGNED)
                .assignedBy(plannerId)
                .build();
        Assignment saved = assignments.save(a);
        audit.record("ASSIGN", "Patrol", patrolId, "officerId=" + officerId);
        return saved;
    }

    @Transactional
    public Patrol transition(UUID patrolId, OperationStatus target, String closureNotes) {
        Patrol p = get(patrolId);
        if (!canTransition(p.getStatus(), target))
            throw ApiException.badRequest("Illegal transition " + p.getStatus() + " → " + target);
        p.setStatus(target);
        if (target == OperationStatus.CLOSED || target == OperationStatus.CANCELLED) {
            p.setClosedAt(Instant.now());
            p.setClosureNotes(closureNotes);
        }
        audit.record(target.name(), "Patrol", patrolId, closureNotes);
        return p;
    }

    public List<Assignment> listAssignments(UUID patrolId) { return assignments.findByPatrolId(patrolId); }

    private boolean canTransition(OperationStatus from, OperationStatus to) {
        return switch (from) {
            case PLANNED   -> to == OperationStatus.ACTIVE || to == OperationStatus.CANCELLED;
            case ACTIVE    -> to == OperationStatus.CLOSED || to == OperationStatus.CANCELLED;
            default        -> false;
        };
    }
}

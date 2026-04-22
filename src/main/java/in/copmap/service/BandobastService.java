package in.copmap.service;

import in.copmap.audit.AuditService;
import in.copmap.domain.*;
import in.copmap.exception.ApiException;
import in.copmap.repository.AssignmentRepository;
import in.copmap.repository.BandobastRepository;
import in.copmap.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BandobastService {

    private final BandobastRepository repo;
    private final AssignmentRepository assignments;
    private final UserRepository users;
    private final AuditService audit;

    public BandobastService(BandobastRepository repo, AssignmentRepository assignments,
                            UserRepository users, AuditService audit) {
        this.repo = repo; this.assignments = assignments;
        this.users = users; this.audit = audit;
    }

    @Transactional
    public Bandobast create(Bandobast b) {
        if (b.getEndAt().isBefore(b.getStartAt())) throw ApiException.badRequest("end_at must be after start_at");
        if (b.getRadiusMeters() <= 0) throw ApiException.badRequest("radius_meters must be positive");
        Bandobast saved = repo.save(b);
        audit.record("CREATE", "Bandobast", saved.getId(), b.getKind() + ": " + b.getTitle());
        return saved;
    }

    public Bandobast get(UUID id) { return repo.findById(id).orElseThrow(() -> ApiException.notFound("Bandobast")); }
    public List<Bandobast> list() { return repo.findAll(); }
    public List<Bandobast> listByKind(Bandobast.OperationKind k) { return repo.findByKind(k); }

    @Transactional
    public Assignment assignOfficer(UUID bandobastId, UUID officerId, UUID plannerId, String role) {
        Bandobast b = get(bandobastId);
        if (b.getStatus() == OperationStatus.CLOSED || b.getStatus() == OperationStatus.CANCELLED)
            throw ApiException.badRequest("Cannot assign to a closed/cancelled operation");

        User officer = users.findById(officerId).orElseThrow(() -> ApiException.notFound("Officer"));
        if (officer.getRole() != Role.OFFICER) throw ApiException.badRequest("Target user is not an OFFICER");
        if (assignments.existsByOfficerIdAndBandobastId(officerId, bandobastId))
            throw ApiException.conflict("Officer already assigned");

        Assignment saved = assignments.save(Assignment.builder()
                .officerId(officerId)
                .bandobastId(bandobastId)
                .role(role)
                .status(Assignment.AssignmentStatus.ASSIGNED)
                .assignedBy(plannerId)
                .build());
        audit.record("ASSIGN", "Bandobast", bandobastId, "officerId=" + officerId);
        return saved;
    }

    @Transactional
    public Bandobast transition(UUID id, OperationStatus target, String notes) {
        Bandobast b = get(id);
        if (!canTransition(b.getStatus(), target))
            throw ApiException.badRequest("Illegal transition " + b.getStatus() + " → " + target);
        b.setStatus(target);
        if (target == OperationStatus.CLOSED || target == OperationStatus.CANCELLED) {
            b.setClosedAt(Instant.now());
            b.setClosureNotes(notes);
        }
        audit.record(target.name(), "Bandobast", id, notes);
        return b;
    }

    public List<Assignment> listAssignments(UUID id) { return assignments.findByBandobastId(id); }

    private boolean canTransition(OperationStatus from, OperationStatus to) {
        return switch (from) {
            case PLANNED -> to == OperationStatus.ACTIVE || to == OperationStatus.CANCELLED;
            case ACTIVE  -> to == OperationStatus.CLOSED || to == OperationStatus.CANCELLED;
            default      -> false;
        };
    }
}

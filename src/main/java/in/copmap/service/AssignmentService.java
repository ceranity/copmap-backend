package in.copmap.service;

import in.copmap.audit.AuditService;
import in.copmap.domain.Assignment;
import in.copmap.exception.ApiException;
import in.copmap.repository.AssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AssignmentService {

    private final AssignmentRepository repo;
    private final AuditService audit;

    public AssignmentService(AssignmentRepository repo, AuditService audit) {
        this.repo = repo; this.audit = audit;
    }

    public List<Assignment> myAssignments(UUID officerId) { return repo.findByOfficerId(officerId); }

    @Transactional
    public Assignment acknowledge(UUID assignmentId, UUID officerId) {
        Assignment a = load(assignmentId, officerId);
        if (a.getStatus() != Assignment.AssignmentStatus.ASSIGNED)
            throw ApiException.badRequest("Only ASSIGNED can be acknowledged");
        a.setStatus(Assignment.AssignmentStatus.ACKNOWLEDGED);
        a.setAcknowledgedAt(Instant.now());
        audit.record("ACK", "Assignment", assignmentId, null);
        return a;
    }

    @Transactional
    public Assignment checkIn(UUID assignmentId, UUID officerId) {
        Assignment a = load(assignmentId, officerId);
        if (a.getStatus() == Assignment.AssignmentStatus.CHECKED_OUT)
            throw ApiException.badRequest("Already checked out");
        a.setStatus(Assignment.AssignmentStatus.CHECKED_IN);
        a.setCheckedInAt(Instant.now());
        audit.record("CHECK_IN", "Assignment", assignmentId, null);
        return a;
    }

    @Transactional
    public Assignment checkOut(UUID assignmentId, UUID officerId) {
        Assignment a = load(assignmentId, officerId);
        if (a.getStatus() != Assignment.AssignmentStatus.CHECKED_IN)
            throw ApiException.badRequest("Must be CHECKED_IN to check out");
        a.setStatus(Assignment.AssignmentStatus.CHECKED_OUT);
        a.setCheckedOutAt(Instant.now());
        audit.record("CHECK_OUT", "Assignment", assignmentId, null);
        return a;
    }

    private Assignment load(UUID id, UUID officerId) {
        Assignment a = repo.findById(id).orElseThrow(() -> ApiException.notFound("Assignment"));
        if (!a.getOfficerId().equals(officerId)) throw ApiException.forbidden("Not your assignment");
        return a;
    }
}

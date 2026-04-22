package in.copmap.service;

import in.copmap.audit.AuditService;
import in.copmap.domain.*;
import in.copmap.exception.ApiException;
import in.copmap.repository.AssignmentRepository;
import in.copmap.repository.BandobastRepository;
import in.copmap.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BandobastServiceTest {

    BandobastRepository repo;
    AssignmentRepository assignments;
    UserRepository users;
    AuditService audit;
    BandobastService service;

    @BeforeEach
    void setUp() {
        repo = mock(BandobastRepository.class);
        assignments = mock(AssignmentRepository.class);
        users = mock(UserRepository.class);
        audit = mock(AuditService.class);
        service = new BandobastService(repo, assignments, users, audit);
    }

    private Bandobast.BandobastBuilder sample() {
        return Bandobast.builder()
                .kind(Bandobast.OperationKind.BANDOBAST)
                .title("Ganeshotsav procession")
                .latitude(19.076).longitude(72.877)
                .radiusMeters(500)
                .startAt(Instant.parse("2026-05-01T10:00:00Z"))
                .endAt(Instant.parse("2026-05-01T22:00:00Z"))
                .createdBy(UUID.randomUUID());
    }

    @Test
    void createRejectsEndBeforeStart() {
        Bandobast b = sample()
                .startAt(Instant.parse("2026-05-01T22:00:00Z"))
                .endAt(Instant.parse("2026-05-01T10:00:00Z"))
                .build();
        assertThatThrownBy(() -> service.create(b))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("end_at");
    }

    @Test
    void createRejectsNonPositiveRadius() {
        Bandobast b = sample().radiusMeters(0).build();
        assertThatThrownBy(() -> service.create(b))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("radius_meters");
    }

    @Test
    void createHappyPathPersistsAndAudits() {
        Bandobast b = sample().build();
        when(repo.save(any(Bandobast.class))).thenAnswer(i -> {
            Bandobast v = i.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });
        Bandobast saved = service.create(b);
        assertThat(saved.getId()).isNotNull();
        verify(audit).record(eq("CREATE"), eq("Bandobast"), any(UUID.class), any());
    }

    @Test
    void transitionEnforcesStateMachine() {
        UUID id = UUID.randomUUID();
        Bandobast b = sample().status(OperationStatus.PLANNED).build();
        b.setId(id);
        when(repo.findById(id)).thenReturn(Optional.of(b));

        // PLANNED → CLOSED must go via ACTIVE
        assertThatThrownBy(() -> service.transition(id, OperationStatus.CLOSED, "x"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void transitionActiveToClosedStampsClosure() {
        UUID id = UUID.randomUUID();
        Bandobast b = sample().status(OperationStatus.ACTIVE).build();
        b.setId(id);
        when(repo.findById(id)).thenReturn(Optional.of(b));

        Bandobast out = service.transition(id, OperationStatus.CLOSED, "ops complete");
        assertThat(out.getStatus()).isEqualTo(OperationStatus.CLOSED);
        assertThat(out.getClosedAt()).isNotNull();
        assertThat(out.getClosureNotes()).isEqualTo("ops complete");
    }

    @Test
    void assignRejectsOnClosedOperation() {
        UUID bid = UUID.randomUUID();
        Bandobast b = sample().status(OperationStatus.CLOSED).build();
        b.setId(bid);
        when(repo.findById(bid)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.assignOfficer(bid, UUID.randomUUID(), UUID.randomUUID(), "CORDON"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void assignRejectsDuplicate() {
        UUID bid = UUID.randomUUID();
        UUID oid = UUID.randomUUID();
        Bandobast b = sample().status(OperationStatus.PLANNED).build();
        b.setId(bid);
        when(repo.findById(bid)).thenReturn(Optional.of(b));
        when(users.findById(oid)).thenReturn(Optional.of(
                User.builder().id(oid).role(Role.OFFICER).active(true).build()));
        when(assignments.existsByOfficerIdAndBandobastId(oid, bid)).thenReturn(true);

        assertThatThrownBy(() -> service.assignOfficer(bid, oid, UUID.randomUUID(), "CORDON"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already assigned");
    }

    @Test
    void assignHappyPath() {
        UUID bid = UUID.randomUUID();
        UUID oid = UUID.randomUUID();
        Bandobast b = sample().status(OperationStatus.PLANNED).build();
        b.setId(bid);
        when(repo.findById(bid)).thenReturn(Optional.of(b));
        when(users.findById(oid)).thenReturn(Optional.of(
                User.builder().id(oid).role(Role.OFFICER).active(true).build()));
        when(assignments.existsByOfficerIdAndBandobastId(oid, bid)).thenReturn(false);
        when(assignments.save(any(Assignment.class))).thenAnswer(i -> i.getArgument(0));

        Assignment a = service.assignOfficer(bid, oid, UUID.randomUUID(), "CORDON");
        assertThat(a.getBandobastId()).isEqualTo(bid);
        assertThat(a.getOfficerId()).isEqualTo(oid);
        assertThat(a.getStatus()).isEqualTo(Assignment.AssignmentStatus.ASSIGNED);
    }
}

package in.copmap.service;

import in.copmap.audit.AuditService;
import in.copmap.domain.*;
import in.copmap.exception.ApiException;
import in.copmap.repository.AssignmentRepository;
import in.copmap.repository.PatrolRepository;
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

class PatrolServiceTest {

    PatrolRepository patrols;
    AssignmentRepository assignments;
    UserRepository users;
    AuditService audit;
    PatrolService service;

    @BeforeEach
    void setUp() {
        patrols = mock(PatrolRepository.class);
        assignments = mock(AssignmentRepository.class);
        users = mock(UserRepository.class);
        audit = mock(AuditService.class);
        service = new PatrolService(patrols, assignments, users, audit);
    }

    @Test
    void createRejectsEndBeforeStart() {
        Patrol p = Patrol.builder()
                .title("Night beat")
                .startAt(Instant.parse("2026-04-23T22:00:00Z"))
                .endAt(Instant.parse("2026-04-23T20:00:00Z"))
                .createdBy(UUID.randomUUID())
                .build();
        assertThatThrownBy(() -> service.create(p)).isInstanceOf(ApiException.class);
    }

    @Test
    void transitionEnforcesStateMachine() {
        UUID id = UUID.randomUUID();
        Patrol p = Patrol.builder().id(id).status(OperationStatus.PLANNED).build();
        when(patrols.findById(id)).thenReturn(Optional.of(p));

        // PLANNED → CLOSED is not allowed; must go via ACTIVE
        assertThatThrownBy(() -> service.transition(id, OperationStatus.CLOSED, "done"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void assignRejectsNonOfficer() {
        UUID pid = UUID.randomUUID();
        UUID uid = UUID.randomUUID();
        when(patrols.findById(pid)).thenReturn(Optional.of(
                Patrol.builder().id(pid).status(OperationStatus.PLANNED).build()));
        when(users.findById(uid)).thenReturn(Optional.of(
                User.builder().id(uid).role(Role.PLANNER).active(true).build()));

        assertThatThrownBy(() -> service.assignOfficer(pid, uid, UUID.randomUUID(), "BEAT"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void assignHappyPath() {
        UUID pid = UUID.randomUUID();
        UUID uid = UUID.randomUUID();
        when(patrols.findById(pid)).thenReturn(Optional.of(
                Patrol.builder().id(pid).status(OperationStatus.PLANNED).build()));
        when(users.findById(uid)).thenReturn(Optional.of(
                User.builder().id(uid).role(Role.OFFICER).active(true).build()));
        when(assignments.existsByOfficerIdAndPatrolId(uid, pid)).thenReturn(false);
        when(assignments.save(any(Assignment.class))).thenAnswer(i -> i.getArgument(0));

        Assignment a = service.assignOfficer(pid, uid, UUID.randomUUID(), "BEAT");
        assertThat(a.getStatus()).isEqualTo(Assignment.AssignmentStatus.ASSIGNED);
        assertThat(a.getOfficerId()).isEqualTo(uid);
        verify(audit).record(eq("ASSIGN"), eq("Patrol"), eq(pid), any());
    }
}

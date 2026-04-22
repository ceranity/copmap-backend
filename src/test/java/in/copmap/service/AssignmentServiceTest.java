package in.copmap.service;

import in.copmap.audit.AuditService;
import in.copmap.domain.Assignment;
import in.copmap.exception.ApiException;
import in.copmap.repository.AssignmentRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssignmentServiceTest {

    @Test
    void onlyOwnerCanProgressAssignment() {
        AssignmentRepository repo = mock(AssignmentRepository.class);
        AuditService audit = mock(AuditService.class);
        AssignmentService service = new AssignmentService(repo, audit);

        UUID assignmentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID intruderId = UUID.randomUUID();
        when(repo.findById(assignmentId)).thenReturn(Optional.of(
                Assignment.builder().id(assignmentId).officerId(ownerId)
                        .status(Assignment.AssignmentStatus.ASSIGNED).build()));

        assertThatThrownBy(() -> service.acknowledge(assignmentId, intruderId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Not your assignment");

        Assignment a = service.acknowledge(assignmentId, ownerId);
        assertThat(a.getStatus()).isEqualTo(Assignment.AssignmentStatus.ACKNOWLEDGED);
    }
}

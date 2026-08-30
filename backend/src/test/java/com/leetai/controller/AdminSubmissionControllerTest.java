package com.leetai.controller;

import com.leetai.dto.SubmissionRecordResponse;
import com.leetai.model.Submission;
import com.leetai.repository.SubmissionRepository;
import com.leetai.service.SubmissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSubmissionController")
class AdminSubmissionControllerTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private SubmissionMapper submissionMapper;

    private AdminSubmissionController controller;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        controller = new AdminSubmissionController(submissionRepository, submissionMapper);
        adminAuth = new UsernamePasswordAuthenticationToken("admin@leetai.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("list() blanks out empty/whitespace-only filters to null before querying")
    void listNormalizesBlankFiltersToNull() {
        when(submissionRepository.search(isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        controller.list("  ", "", 0, 20);

        verify(submissionRepository).search(isNull(), isNull(), any());
    }

    @Test
    @DisplayName("list() trims and forwards non-blank filters as-is")
    void listTrimsFilters() {
        when(submissionRepository.search(eq("two-sum"), eq("alice"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        controller.list(" two-sum ", " alice ", 0, 20);

        verify(submissionRepository).search(eq("two-sum"), eq("alice"), any());
    }

    @Test
    @DisplayName("markSolved() sets solved=true and records which admin made the change")
    void markSolvedSetsSolvedTrueAndMarkedBy() {
        Submission submission = new Submission();
        submission.setId(5L);
        submission.setSolved(false);
        when(submissionRepository.findById(5L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));
        when(submissionMapper.toResponse(any())).thenReturn(new SubmissionRecordResponse());

        controller.markSolved(5L, adminAuth);

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(captor.capture());
        assertThat(captor.getValue().isSolved()).isTrue();
        assertThat(captor.getValue().getMarkedBy()).isEqualTo("admin@leetai.com");
    }

    @Test
    @DisplayName("unmarkSolved() sets solved=false and records which admin made the change")
    void unmarkSolvedSetsSolvedFalse() {
        Submission submission = new Submission();
        submission.setId(5L);
        submission.setSolved(true);
        when(submissionRepository.findById(5L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));
        when(submissionMapper.toResponse(any())).thenReturn(new SubmissionRecordResponse());

        controller.unmarkSolved(5L, adminAuth);

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(captor.capture());
        assertThat(captor.getValue().isSolved()).isFalse();
        assertThat(captor.getValue().getMarkedBy()).isEqualTo("admin@leetai.com");
    }

    @Test
    @DisplayName("markSolved() on an unknown submission id throws 404 rather than silently doing nothing")
    void markSolvedUnknownIdThrows404() {
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.markSolved(999L, adminAuth));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        verify(submissionRepository, never()).save(any());
    }
}

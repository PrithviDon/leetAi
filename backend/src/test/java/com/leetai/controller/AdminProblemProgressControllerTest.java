package com.leetai.controller;

import com.leetai.dto.ResetSolvedResponse;
import com.leetai.dto.SolvedUserResponse;
import com.leetai.model.Problem;
import com.leetai.model.Submission;
import com.leetai.model.User;
import com.leetai.repository.SolvedUserProjection;
import com.leetai.repository.SubmissionRepository;
import com.leetai.service.ProblemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProblemProgressController")
class AdminProblemProgressControllerTest {

    @Mock private ProblemService problemService;
    @Mock private SubmissionRepository submissionRepository;

    private AdminProblemProgressController controller;
    private Problem problem;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        controller = new AdminProblemProgressController(problemService, submissionRepository);
        problem = new Problem();
        problem.setId(1L);
        problem.setSlug("two-sum");
        adminAuth = new UsernamePasswordAuthenticationToken("admin@leetai.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("solvedUsers() maps every projection row returned by the repository")
    void solvedUsersMapsProjections() {
        when(problemService.getEntityBySlug("two-sum")).thenReturn(problem);
        SolvedUserProjection row = mock(SolvedUserProjection.class);
        when(row.getUserId()).thenReturn(1L);
        when(row.getUserEmail()).thenReturn("alice@example.com");
        when(row.getUserName()).thenReturn("Alice");
        when(row.getFirstSolvedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(row.getSubmissionCount()).thenReturn(2L);
        when(submissionRepository.findSolvedUsersForProblem("two-sum")).thenReturn(List.of(row));

        List<SolvedUserResponse> result = controller.solvedUsers("two-sum");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserEmail()).isEqualTo("alice@example.com");
        assertThat(result.get(0).getSubmissionCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("solvedUsers() 404s early via getEntityBySlug for an unknown problem, before ever querying submissions")
    void solvedUsersPropagates404ForUnknownProblem() {
        when(problemService.getEntityBySlug("ghost")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> controller.solvedUsers("ghost"));

        verifyNoInteractions(submissionRepository);
    }

    @ParameterizedTest
    @DisplayName("resetSolved() reports back exactly how many submissions the bulk update changed")
    @ValueSource(ints = {0, 1, 3})
    void resetSolvedReportsChangedCount(int changedCount) {
        when(problemService.getEntityBySlug("two-sum")).thenReturn(problem);
        when(submissionRepository.resetSolvedForUserAndProblem(42L, 1L, "admin@leetai.com"))
                .thenReturn(changedCount);

        ResetSolvedResponse response = controller.resetSolved("two-sum", 42L, adminAuth);

        assertThat(response.getSubmissionsReset()).isEqualTo(changedCount);
        assertThat(response.getUserId()).isEqualTo(42L);
        assertThat(response.getProblemSlug()).isEqualTo("two-sum");
    }

    @Test
    @DisplayName("markSolved() flips the user's most recent submission, leaving older ones as they were")
    void markSolvedFlipsMostRecentSubmission() {
        Submission older = buildSubmission(10L, false, Instant.parse("2026-01-01T00:00:00Z"));
        Submission mostRecent = buildSubmission(11L, false, Instant.parse("2026-01-02T00:00:00Z"));
        // Repository contract: newest first — mirror that ordering here.
        when(submissionRepository.findByUser_IdAndProblem_SlugOrderByCreatedAtDesc(42L, "two-sum"))
                .thenReturn(List.of(mostRecent, older));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        SolvedUserResponse response = controller.markSolved("two-sum", 42L, adminAuth);

        assertThat(mostRecent.isSolved()).isTrue();
        assertThat(mostRecent.getMarkedBy()).isEqualTo("admin@leetai.com");
        assertThat(older.isSolved()).isFalse();
        assertThat(response.getUserEmail()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("markSolved() refuses with 409 when the user has no submissions for the problem at all")
    void markSolvedRejectsWhenNoSubmissionsExist() {
        when(submissionRepository.findByUser_IdAndProblem_SlugOrderByCreatedAtDesc(42L, "two-sum"))
                .thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.markSolved("two-sum", 42L, adminAuth));

        assertThat(ex.getStatusCode().value()).isEqualTo(409);
        verify(submissionRepository, never()).save(any());
    }

    private Submission buildSubmission(Long id, boolean solved, Instant createdAt) {
        Submission s = new Submission();
        s.setId(id);
        s.setSolved(solved);
        s.setCreatedAt(createdAt);
        User user = new User();
        user.setId(42L);
        user.setEmail("bob@example.com");
        user.setName("Bob");
        s.setUser(user);
        return s;
    }
}

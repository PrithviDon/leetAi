package com.leetai.controller;

import com.leetai.dto.SubmissionRecordResponse;
import com.leetai.dto.SubmissionRequest;
import com.leetai.dto.SubmissionResponse;
import com.leetai.dto.TestCaseResult;
import com.leetai.model.Problem;
import com.leetai.model.Submission;
import com.leetai.model.User;
import com.leetai.repository.SubmissionRepository;
import com.leetai.repository.UserRepository;
import com.leetai.service.AiAssistantService;
import com.leetai.service.CodeExecutionService;
import com.leetai.service.ProblemService;
import com.leetai.service.RateLimitService;
import com.leetai.service.SubmissionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionController")
class SubmissionControllerTest {

    @Mock private ProblemService problemService;
    @Mock private CodeExecutionService codeExecutionService;
    @Mock private AiAssistantService aiAssistantService;
    @Mock private RateLimitService rateLimitService;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubmissionMapper submissionMapper;

    private SubmissionController controller;
    private Problem publishedProblem;
    private User user;
    private Authentication regularUserAuth;

    @BeforeEach
    void setUp() {
        controller = new SubmissionController(problemService, codeExecutionService, aiAssistantService,
                rateLimitService, submissionRepository, userRepository, submissionMapper);

        publishedProblem = new Problem();
        publishedProblem.setId(1L);
        publishedProblem.setSlug("two-sum");
        publishedProblem.setFunctionName("twoSum");
        publishedProblem.setStatus(Problem.Status.PUBLISHED);
        publishedProblem.setTestCases(List.of());

        user = new User();
        user.setId(7L);
        user.setEmail("alice@example.com");

        regularUserAuth = new UsernamePasswordAuthenticationToken("alice@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @AfterEach
    void tearDown() {
        controller = null;
    }

    private SubmissionRequest buildRequest() {
        SubmissionRequest req = new SubmissionRequest();
        req.setCode("function twoSum() {}");
        req.setLanguage("javascript");
        req.setApproach("hash map");
        return req;
    }

    @Test
    @DisplayName("submit() marks the submission solved when every test case passes")
    void submitMarksSolvedWhenAllTestsPass() {
        when(problemService.getEntityBySlug("two-sum")).thenReturn(publishedProblem);
        when(codeExecutionService.run(any(), any(), any(), any())).thenReturn(List.of(
                new TestCaseResult("in", "out", "out", true, null, 5)
        ));
        when(aiAssistantService.reviewSubmission(any(), any(), any(), any(), any())).thenReturn("Great job");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(123L);
            return s;
        });

        SubmissionResponse response = controller.submit("two-sum", buildRequest(), regularUserAuth);

        assertThat(response.isAllPassed()).isTrue();
        assertThat(response.isSolved()).isTrue();
        assertThat(response.getSubmissionId()).isEqualTo(123L);

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(captor.capture());
        assertThat(captor.getValue().isSolved()).isTrue();
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("submit() marks the submission unsolved when any test case fails")
    void submitMarksUnsolvedWhenATestFails() {
        when(problemService.getEntityBySlug("two-sum")).thenReturn(publishedProblem);
        when(codeExecutionService.run(any(), any(), any(), any())).thenReturn(List.of(
                new TestCaseResult("in", "out", "wrong", false, null, 5)
        ));
        when(aiAssistantService.reviewSubmission(any(), any(), any(), any(), any())).thenReturn("Try again");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmissionResponse response = controller.submit("two-sum", buildRequest(), regularUserAuth);

        assertThat(response.isAllPassed()).isFalse();
        assertThat(response.isSolved()).isFalse();
    }

    @Test
    @DisplayName("submit() checks the rate limit for a regular user")
    void submitChecksRateLimitForRegularUser() {
        when(problemService.getEntityBySlug("two-sum")).thenReturn(publishedProblem);
        when(codeExecutionService.run(any(), any(), any(), any())).thenReturn(List.of());
        when(aiAssistantService.reviewSubmission(any(), any(), any(), any(), any())).thenReturn("");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        controller.submit("two-sum", buildRequest(), regularUserAuth);

        verify(rateLimitService).checkSubmissionAllowed("alice@example.com");
    }

    @Test
    @DisplayName("submit() skips the rate limit entirely for an admin")
    void submitSkipsRateLimitForAdmin() {
        Authentication adminAuth = new UsernamePasswordAuthenticationToken("admin@leetai.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@leetai.com");

        when(problemService.getEntityBySlug("two-sum")).thenReturn(publishedProblem);
        when(codeExecutionService.run(any(), any(), any(), any())).thenReturn(List.of());
        when(aiAssistantService.reviewSubmission(any(), any(), any(), any(), any())).thenReturn("");
        when(userRepository.findByEmail("admin@leetai.com")).thenReturn(Optional.of(admin));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        controller.submit("two-sum", buildRequest(), adminAuth);

        verify(rateLimitService, never()).checkSubmissionAllowed(anyString());
    }

    @Test
    @DisplayName("submit() rejects a draft problem for a regular user with 404, without ever running their code")
    void submitRejectsDraftProblemForRegularUser() {
        Problem draft = new Problem();
        draft.setSlug("unreleased");
        draft.setStatus(Problem.Status.DRAFT);
        when(problemService.getEntityBySlug("unreleased")).thenReturn(draft);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.submit("unreleased", buildRequest(), regularUserAuth));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        verifyNoInteractions(codeExecutionService);
    }

    @Test
    @DisplayName("mySubmissions() maps each of the user's own submissions via SubmissionMapper")
    void mySubmissionsMapsEachRecord() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(submissionRepository.findByUser_IdAndProblem_SlugOrderByCreatedAtDesc(7L, "two-sum"))
                .thenReturn(List.of(new Submission(), new Submission()));
        when(submissionMapper.toResponse(any(Submission.class)))
                .thenReturn(new SubmissionRecordResponse());

        List<SubmissionRecordResponse> result = controller.mySubmissions("two-sum", regularUserAuth);

        assertThat(result).hasSize(2);
        verify(submissionMapper, times(2)).toResponse(any());
    }

    @Test
    @DisplayName("mySubmissions() rejects an unknown user with 401")
    void mySubmissionsRejectsUnknownUser() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.mySubmissions("two-sum", regularUserAuth));

        assertThat(ex.getStatusCode().value()).isEqualTo(401);
    }
}

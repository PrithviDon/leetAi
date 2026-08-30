package com.leetai.controller;

import com.leetai.dto.PagedResponse;
import com.leetai.dto.ProblemResponse;
import com.leetai.dto.ProblemSearchResult;
import com.leetai.dto.ProgressResponse;
import com.leetai.model.Problem;
import com.leetai.repository.ProblemRepository;
import com.leetai.repository.SubmissionRepository;
import com.leetai.service.ElasticsearchSearchService;
import com.leetai.service.ProblemService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemController")
class ProblemControllerTest {

    @Mock private ProblemService problemService;
    @Mock private ElasticsearchSearchService searchService;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private ProblemRepository problemRepository;

    private ProblemController controller;

    private static final Authentication AUTHENTICATED_ALICE =
            new UsernamePasswordAuthenticationToken("alice@example.com", null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
    private static final Authentication ANONYMOUS =
            new AnonymousAuthenticationToken("key", "anonymousUser",
                    List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

    @BeforeEach
    void setUp() {
        controller = new ProblemController(problemService, searchService, submissionRepository, problemRepository);
    }

    @AfterEach
    void tearDown() {
        // @Mock fields are already re-created fresh before every test by
        // MockitoExtension, so there's nothing to reset here. Deliberately
        // NOT calling Mockito.reset() — it can mask genuine
        // UnnecessaryStubbingException warnings from the strict-stubs
        // checker that runs after this method, which would hide real bugs
        // in the tests themselves. Just clearing our own reference instead.
        controller = null;
    }

    /** null (no Authentication object at all) and an anonymous principal should behave identically. */
    static Stream<Authentication> anonymousLikeAuthentications() {
        return Stream.of(null, ANONYMOUS);
    }

    @ParameterizedTest
    @DisplayName("an anonymous caller — no auth object at all, or an anonymous-principal token — never gets solved slugs")
    @MethodSource("anonymousLikeAuthentications")
    void anonymousCallersGetNoSolvedSlugs(Authentication authentication) {
        ProblemResponse response = new ProblemResponse();
        when(problemService.getPublishedBySlug("two-sum")).thenReturn(response);

        ProblemResponse result = controller.getProblem("two-sum", authentication);

        assertThat(result.isSolved()).isFalse();
        verifyNoInteractions(submissionRepository);
    }

    @Test
    @DisplayName("getProblem marks the problem solved when the authenticated user's solved-slug set contains it")
    void getProblemMarksSolvedForAuthenticatedUser() {
        when(problemService.getPublishedBySlug("two-sum")).thenReturn(new ProblemResponse());
        when(submissionRepository.findSolvedSlugsByUserEmail("alice@example.com"))
                .thenReturn(Set.of("two-sum", "valid-parentheses"));

        ProblemResponse result = controller.getProblem("two-sum", AUTHENTICATED_ALICE);

        assertThat(result.isSolved()).isTrue();
    }

    @Test
    @DisplayName("getProblem leaves solved false when the slug isn't in the user's solved set")
    void getProblemLeavesUnsolvedWhenNotInSet() {
        when(problemService.getPublishedBySlug("two-sum")).thenReturn(new ProblemResponse());
        when(submissionRepository.findSolvedSlugsByUserEmail("alice@example.com"))
                .thenReturn(Set.of("valid-parentheses"));

        ProblemResponse result = controller.getProblem("two-sum", AUTHENTICATED_ALICE);

        assertThat(result.isSolved()).isFalse();
    }

    @Test
    @DisplayName("progress reports 0/total for an anonymous caller, solved/total for an authenticated one")
    void progressReflectsAuthenticationState() {
        when(problemRepository.countByStatus(Problem.Status.PUBLISHED)).thenReturn(5L);

        ProgressResponse anonymous = controller.progress(ANONYMOUS);
        assertThat(anonymous.getSolved()).isZero();
        assertThat(anonymous.getTotal()).isEqualTo(5L);

        when(submissionRepository.findSolvedSlugsByUserEmail("alice@example.com")).thenReturn(Set.of("two-sum"));
        ProgressResponse authenticated = controller.progress(AUTHENTICATED_ALICE);
        assertThat(authenticated.getSolved()).isEqualTo(1L);
        assertThat(authenticated.getTotal()).isEqualTo(5L);
    }

    @Test
    @DisplayName("listProblems builds an explicit PagedResponse from the Elasticsearch page, with solved flags applied per item")
    void listProblemsUsesElasticsearchWhenAvailable() {
        ProblemSearchResult twoSum = new ProblemSearchResult("two-sum", "Two Sum", "EASY");
        ProblemSearchResult validParens = new ProblemSearchResult("valid-parentheses", "Valid Parentheses", "EASY");
        PageImpl<ProblemSearchResult> esPage = new PageImpl<>(List.of(twoSum, validParens), PageRequest.of(0, 10), 2);

        when(searchService.search(any(), any(), any())).thenReturn(esPage);
        when(submissionRepository.findSolvedSlugsByUserEmail("alice@example.com")).thenReturn(Set.of("two-sum"));

        PagedResponse<?> result = controller.listProblems(null, "ALL", 0, 10, AUTHENTICATED_ALICE);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(twoSum.isSolved()).isTrue();
        assertThat(validParens.isSolved()).isFalse();
        verify(problemService, never()).listPublished();
    }

    @Test
    @DisplayName("listProblems falls back to the MySQL listing when Elasticsearch throws")
    void listProblemsFallsBackToMySqlOnEsFailure() {
        when(searchService.search(any(), any(), any())).thenThrow(new RuntimeException("ES is down"));

        ProblemResponse fallbackItem = new ProblemResponse();
        fallbackItem.setSlug("two-sum");
        when(problemService.listPublished()).thenReturn(List.of(fallbackItem));

        PagedResponse<?> result = controller.listProblems(null, "ALL", 0, 10, ANONYMOUS);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(fallbackItem.isSolved()).isFalse();
    }

    @Test
    @Disabled("Relevance ranking (fuzzy matching, field boosting) only means something against a real "
            + "Elasticsearch cluster with a populated index — a mocked ElasticsearchSearchService can't "
            + "meaningfully exercise it. Left as a placeholder; covered by manual QA against a real ES "
            + "instance instead of an automated test here.")
    @DisplayName("search relevance ranking favors name matches over description matches")
    void relevanceRankingFavorsNameMatches() {
        // Intentionally left unimplemented — see the @Disabled reason above.
    }
}

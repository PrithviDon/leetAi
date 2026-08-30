package com.leetai.repository;

import com.leetai.model.Problem;
import com.leetai.model.Submission;
import com.leetai.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice test: only JPA infrastructure + repositories are loaded, backed by
 * an in-memory H2 instance (see src/test/resources/application.properties
 * for why it runs in MySQL-compatibility mode). Each test method runs in
 * its own transaction that's rolled back afterward, so tests never leak
 * data into each other despite sharing the same in-memory DB instance.
 */
@DataJpaTest
@DisplayName("SubmissionRepository")
class SubmissionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SubmissionRepository submissionRepository;

    private static Instant baseTime;

    private User alice;
    private User bob;
    private Problem twoSum;
    private Problem validParens;

    @BeforeAll
    static void setUpFixedReferenceTime() {
        // One reference timestamp shared by every test in this class, so
        // ordering/"first solved at" assertions don't depend on how long
        // the test run itself takes.
        baseTime = Instant.parse("2026-01-01T00:00:00Z");
    }

    @AfterAll
    static void tearDownFixedReferenceTime() {
        baseTime = null;
    }

    @BeforeEach
    void seedUsersAndProblems() {
        alice = persistUser("alice@example.com", "Alice");
        bob = persistUser("bob@example.com", "Bob");
        twoSum = persistProblem("two-sum", "Two Sum");
        validParens = persistProblem("valid-parentheses", "Valid Parentheses");
    }

    @AfterEach
    void clearPersistenceContext() {
        // @DataJpaTest rolls back the transaction after every test regardless,
        // but clearing explicitly means no test can accidentally read a
        // managed entity left over from its own setup instead of a fresh
        // query result.
        entityManager.clear();
    }

    private User persistUser(String email, String name) {
        User u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setProvider("google");
        u.setRole(User.Role.USER);
        return entityManager.persistAndFlush(u);
    }

    private Problem persistProblem(String slug, String name) {
        Problem p = new Problem();
        p.setSlug(slug);
        p.setName(name);
        p.setDifficulty(Problem.Difficulty.EASY);
        p.setDescription("desc");
        p.setFunctionName("solve");
        p.setStatus(Problem.Status.PUBLISHED);
        return entityManager.persistAndFlush(p);
    }

    private Submission persistSubmission(User user, Problem problem, boolean solved, Instant createdAt) {
        Submission s = new Submission();
        s.setUser(user);
        s.setProblem(problem);
        s.setCode("function solve() {}");
        s.setLanguage("javascript");
        s.setPassedCount(solved ? 3 : 1);
        s.setTotalCount(3);
        s.setAllPassed(solved);
        s.setSolved(solved);
        s.setAiFeedback("looks good");
        s.setCreatedAt(createdAt);
        return entityManager.persistAndFlush(s);
    }

    @Test
    @DisplayName("findByUser_IdAndProblem_SlugOrderByCreatedAtDesc returns only that user+problem's submissions, newest first")
    void findsOwnHistoryInDescendingOrder() {
        persistSubmission(alice, twoSum, false, baseTime);
        persistSubmission(alice, twoSum, true, baseTime.plus(1, ChronoUnit.DAYS));
        persistSubmission(alice, validParens, true, baseTime); // different problem — must be excluded
        persistSubmission(bob, twoSum, true, baseTime);        // different user — must be excluded

        List<Submission> history = submissionRepository
                .findByUser_IdAndProblem_SlugOrderByCreatedAtDesc(alice.getId(), "two-sum");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getCreatedAt()).isAfter(history.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("existsByUser_IdAndProblem_IdAndSolvedTrue is true only once a solved submission exists")
    void existsSolvedTrueOnlyWhenSolvedSubmissionExists() {
        persistSubmission(alice, twoSum, false, baseTime);

        assertThat(submissionRepository.existsByUser_IdAndProblem_IdAndSolvedTrue(alice.getId(), twoSum.getId()))
                .isFalse();

        persistSubmission(alice, twoSum, true, baseTime.plus(1, ChronoUnit.HOURS));

        assertThat(submissionRepository.existsByUser_IdAndProblem_IdAndSolvedTrue(alice.getId(), twoSum.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("findSolvedSlugsByUserEmail returns exactly the slugs that user has solved, not another user's")
    void findsSolvedSlugsForUser() {
        persistSubmission(alice, twoSum, true, baseTime);
        persistSubmission(alice, validParens, false, baseTime);
        persistSubmission(bob, validParens, true, baseTime);

        Set<String> aliceSolved = submissionRepository.findSolvedSlugsByUserEmail("alice@example.com");

        assertThat(aliceSolved).containsExactly("two-sum");
    }

    @ParameterizedTest(name = "slug=\"{0}\", email=\"{1}\" -> {2} match(es)")
    @DisplayName("admin search() filters by slug and/or email independently, ANDed together when both are given")
    @CsvSource({
            "two-sum, '', 1",
            "'', alice, 1",
            "two-sum, alice, 1",
            "valid-parentheses, alice, 0",
            "'', '', 2"
    })
    void adminSearchFiltersIndependently(String slug, String email, int expectedCount) {
        persistSubmission(alice, twoSum, true, baseTime);
        persistSubmission(bob, validParens, true, baseTime);

        Page<Submission> page = submissionRepository.search(
                blankToNull(slug), blankToNull(email), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(expectedCount);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    @Test
    @DisplayName("findSolvedUsersForProblem aggregates per user: earliest solved date, correct submission count")
    void aggregatesSolvedUsersPerProblem() {
        persistSubmission(alice, twoSum, true, baseTime);
        persistSubmission(alice, twoSum, true, baseTime.plus(1, ChronoUnit.DAYS));
        persistSubmission(bob, twoSum, true, baseTime.plus(2, ChronoUnit.DAYS));
        persistSubmission(bob, validParens, true, baseTime); // different problem — must be excluded

        List<SolvedUserProjection> solvedUsers = submissionRepository.findSolvedUsersForProblem("two-sum");

        assertThat(solvedUsers).hasSize(2);
        SolvedUserProjection aliceRow = solvedUsers.stream()
                .filter(r -> r.getUserEmail().equals("alice@example.com"))
                .findFirst().orElseThrow();
        assertThat(aliceRow.getSubmissionCount()).isEqualTo(2L);
        assertThat(aliceRow.getFirstSolvedAt()).isEqualTo(baseTime);
    }

    @Test
    @DisplayName("resetSolvedForUserAndProblem flips every solved submission for that user+problem, and nothing else")
    void resetSolvedFlipsOnlyMatchingRows() {
        persistSubmission(alice, twoSum, true, baseTime);
        persistSubmission(alice, twoSum, true, baseTime.plus(1, ChronoUnit.HOURS));
        persistSubmission(alice, validParens, true, baseTime); // different problem — must survive
        persistSubmission(bob, twoSum, true, baseTime);        // different user — must survive

        int changed = submissionRepository.resetSolvedForUserAndProblem(alice.getId(), twoSum.getId(), "admin@leetai.com");

        // A bulk JPQL update bypasses the persistence context, so re-check
        // via a fresh read rather than trusting anything already loaded.
        entityManager.flush();
        entityManager.clear();

        assertThat(changed).isEqualTo(2);
        assertThat(submissionRepository.existsByUser_IdAndProblem_IdAndSolvedTrue(alice.getId(), twoSum.getId())).isFalse();
        assertThat(submissionRepository.existsByUser_IdAndProblem_IdAndSolvedTrue(alice.getId(), validParens.getId())).isTrue();
        assertThat(submissionRepository.existsByUser_IdAndProblem_IdAndSolvedTrue(bob.getId(), twoSum.getId())).isTrue();
    }

    @Test
    @DisplayName("resetSolvedForUserAndProblem is a safe no-op when the user hasn't solved the problem")
    void resetSolvedIsNoOpWhenNothingToReset() {
        persistSubmission(alice, twoSum, false, baseTime);

        int changed = submissionRepository.resetSolvedForUserAndProblem(alice.getId(), twoSum.getId(), "admin@leetai.com");

        assertThat(changed).isZero();
    }

    @Test
    @DisplayName("regression: aiFeedback longer than 255 chars persists without truncation (the LONGTEXT fix)")
    void aiFeedbackLongerThan255CharsIsNotTruncated() {
        String longFeedback = "x".repeat(5_000);
        Submission s = persistSubmission(alice, twoSum, true, baseTime);
        s.setAiFeedback(longFeedback);
        entityManager.persistAndFlush(s);
        entityManager.clear();

        Submission reloaded = submissionRepository.findById(s.getId()).orElseThrow();

        assertThat(reloaded.getAiFeedback()).hasSize(5_000);
    }
}

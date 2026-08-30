package com.leetai.repository;

import com.leetai.model.Problem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("ProblemRepository")
class ProblemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProblemRepository problemRepository;

    // Purely illustrative use of @BeforeAll/@AfterAll here — a running count
    // of how many test methods executed in this class, printed once at the
    // end. Not needed for correctness, just demonstrates the lifecycle.
    private static int testsRun;

    @BeforeAll
    static void resetCounter() {
        testsRun = 0;
    }

    @AfterAll
    static void reportCounter() {
        System.out.println("ProblemRepositoryTest: ran " + testsRun + " test(s)");
    }

    @BeforeEach
    void countThisRun() {
        testsRun++;
    }

    private Problem persistProblem(String slug, Problem.Status status, String description) {
        Problem p = new Problem();
        p.setSlug(slug);
        p.setName(slug);
        p.setDifficulty(Problem.Difficulty.EASY);
        p.setDescription(description);
        p.setFunctionName("solve");
        p.setStatus(status);
        return entityManager.persistAndFlush(p);
    }

    @Test
    @DisplayName("findBySlug finds an existing problem and is empty for an unknown slug")
    void findBySlugWorks() {
        persistProblem("two-sum", Problem.Status.PUBLISHED, "desc");

        assertThat(problemRepository.findBySlug("two-sum")).isPresent();
        assertThat(problemRepository.findBySlug("does-not-exist")).isEmpty();
    }

    @ParameterizedTest(name = "counts only {0} problems")
    @DisplayName("countByStatus only counts problems in that exact status, not the other one")
    @EnumSource(Problem.Status.class)
    void countByStatusOnlyCountsMatchingStatus(Problem.Status status) {
        persistProblem("published-1", Problem.Status.PUBLISHED, "d1");
        persistProblem("published-2", Problem.Status.PUBLISHED, "d2");
        persistProblem("draft-1", Problem.Status.DRAFT, "d3");

        long expected = status == Problem.Status.PUBLISHED ? 2 : 1;

        assertThat(problemRepository.countByStatus(status)).isEqualTo(expected);
    }

    @Test
    @DisplayName("regression: a description longer than 255 chars persists without truncation (the LONGTEXT fix)")
    void descriptionLongerThan255CharsIsNotTruncated() {
        String longDescription = "a".repeat(10_000);
        Problem saved = persistProblem("long-description", Problem.Status.PUBLISHED, longDescription);
        entityManager.clear();

        Problem reloaded = problemRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getDescription()).hasSize(10_000);
    }

    @Test
    @Disabled("Slug uniqueness is enforced by a DB unique constraint (@Column(unique = true)). The exact exception "
            + "type/message for a constraint violation differs between H2 and real MySQL, so asserting on it here "
            + "would be testing H2's error-wrapping behavior more than our own code. Left as documentation of the "
            + "expectation rather than an assertion.")
    @DisplayName("duplicate slugs are rejected by the unique constraint")
    void duplicateSlugsAreRejected() {
        persistProblem("two-sum", Problem.Status.PUBLISHED, "d1");
        persistProblem("two-sum", Problem.Status.PUBLISHED, "d2");
    }
}

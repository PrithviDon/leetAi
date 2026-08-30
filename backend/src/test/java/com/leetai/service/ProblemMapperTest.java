package com.leetai.service;

import com.leetai.dto.CreateProblemRequest;
import com.leetai.dto.ProblemResponse;
import com.leetai.dto.TestCaseInput;
import com.leetai.model.Problem;
import com.leetai.model.TestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemMapper")
class ProblemMapperTest {

    private ProblemMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProblemMapper();
    }

    @AfterEach
    void tearDown() {
        // No shared resources to release — included for completeness /
        // symmetry with @BeforeEach, and as a place to add cleanup if this
        // class ever grows to hold something stateful.
        mapper = null;
    }

    private CreateProblemRequest requestWithDifficulty(String difficulty) {
        CreateProblemRequest req = new CreateProblemRequest();
        req.setName("Two Sum");
        req.setDescription("desc");
        req.setFunctionName("twoSum");
        req.setDifficulty(difficulty);

        TestCaseInput tc = new TestCaseInput();
        tc.setInput("[[2,7,11,15],9]");
        tc.setExpectedOutput("[0,1]");
        tc.setHidden(false);
        req.setTestCases(List.of(tc));
        return req;
    }

    @ParameterizedTest(name = "\"{0}\" resolves to {1}")
    @DisplayName("toEntity resolves the difficulty string case-insensitively, defaulting to MEDIUM for anything unrecognized")
    @CsvSource({
            "EASY, EASY",
            "easy, EASY",
            "  Hard , HARD",
            "bogus-value, MEDIUM",
            "'', MEDIUM"
    })
    void parsesOrDefaultsDifficulty(String input, Problem.Difficulty expected) {
        Problem problem = mapper.toEntity(requestWithDifficulty(input), "two-sum");

        assertThat(problem.getDifficulty()).isEqualTo(expected);
    }

    @Test
    @DisplayName("toEntity defaults to MEDIUM when difficulty is null (rather than throwing)")
    void defaultsToMediumWhenDifficultyIsNull() {
        Problem problem = mapper.toEntity(requestWithDifficulty(null), "two-sum");

        assertThat(problem.getDifficulty()).isEqualTo(Problem.Difficulty.MEDIUM);
    }

    @Test
    @DisplayName("toEntity carries every field across and wires each test case back to the owning problem")
    void toEntityCopiesAllFieldsAndLinksTestCases() {
        Problem problem = mapper.toEntity(requestWithDifficulty("EASY"), "two-sum");

        assertThat(problem.getSlug()).isEqualTo("two-sum");
        assertThat(problem.getName()).isEqualTo("Two Sum");
        assertThat(problem.getDescription()).isEqualTo("desc");
        assertThat(problem.getFunctionName()).isEqualTo("twoSum");
        assertThat(problem.getTestCases()).hasSize(1);
        assertThat(problem.getTestCases().get(0).getProblem()).isSameAs(problem);
    }

    @Test
    @DisplayName("toResponse excludes hidden test cases by default")
    void toResponseExcludesHiddenTestCasesByDefault() {
        Problem problem = buildProblemWithOneHiddenAndOneVisibleTestCase();

        ProblemResponse response = mapper.toResponse(problem);

        assertThat(response.getTestCases()).hasSize(1);
        assertThat(response.getTestCases().get(0).getInput()).isEqualTo("visible-input");
    }

    @Test
    @DisplayName("toResponse(problem, includeHidden=true) includes hidden test cases, for admin views")
    void toResponseIncludesHiddenTestCasesWhenRequested() {
        Problem problem = buildProblemWithOneHiddenAndOneVisibleTestCase();

        ProblemResponse response = mapper.toResponse(problem, true);

        assertThat(response.getTestCases()).hasSize(2);
    }

    @Test
    @DisplayName("toResponse maps a null difficulty/status to null strings rather than throwing an NPE")
    void toResponseHandlesNullEnumsGracefully() {
        Problem problem = new Problem();
        problem.setSlug("x");
        problem.setTestCases(List.of());

        ProblemResponse response = mapper.toResponse(problem);

        assertThat(response.getDifficulty()).isNull();
        assertThat(response.getStatus()).isNull();
    }

    @Test
    @DisplayName("applyUpdate replaces test cases wholesale rather than merging with the existing set")
    void applyUpdateReplacesTestCasesWholesale() {
        Problem existing = mapper.toEntity(requestWithDifficulty("EASY"), "two-sum");
        assertThat(existing.getTestCases()).hasSize(1);

        CreateProblemRequest update = requestWithDifficulty("HARD");
        TestCaseInput tc2 = new TestCaseInput();
        tc2.setInput("[[1,2],3]");
        tc2.setExpectedOutput("[0,1]");
        update.setTestCases(List.of(tc2, tc2));

        mapper.applyUpdate(existing, update);

        assertThat(existing.getDifficulty()).isEqualTo(Problem.Difficulty.HARD);
        assertThat(existing.getTestCases()).hasSize(2);
        assertThat(existing.getTestCases()).allMatch(tc -> tc.getProblem() == existing);
    }

    private Problem buildProblemWithOneHiddenAndOneVisibleTestCase() {
        Problem problem = new Problem();
        problem.setSlug("two-sum");

        TestCase visible = new TestCase();
        visible.setInputJson("visible-input");
        visible.setExpectedOutputJson("out");
        visible.setHidden(false);

        TestCase hidden = new TestCase();
        hidden.setInputJson("hidden-input");
        hidden.setExpectedOutputJson("out");
        hidden.setHidden(true);

        problem.setTestCases(List.of(visible, hidden));
        return problem;
    }
}

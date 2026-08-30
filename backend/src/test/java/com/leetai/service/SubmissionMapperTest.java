package com.leetai.service;

import com.leetai.dto.SubmissionRecordResponse;
import com.leetai.model.Problem;
import com.leetai.model.Submission;
import com.leetai.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubmissionMapper")
class SubmissionMapperTest {

    private SubmissionMapper mapper;
    private User user;
    private Problem problem;

    @BeforeEach
    void setUp() {
        mapper = new SubmissionMapper();

        user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setName("Alice");

        problem = new Problem();
        problem.setId(10L);
        problem.setSlug("two-sum");
        problem.setName("Two Sum");
    }

    @AfterEach
    void tearDown() {
        mapper = null;
        user = null;
        problem = null;
    }

    private Submission buildSubmission(boolean solved) {
        Submission s = new Submission();
        s.setId(99L);
        s.setUser(user);
        s.setProblem(problem);
        s.setCode("function twoSum() {}");
        s.setLanguage("javascript");
        s.setApproach("hash map");
        s.setPassedCount(3);
        s.setTotalCount(3);
        s.setAllPassed(true);
        s.setSolved(solved);
        s.setAiFeedback("Nice work");
        s.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return s;
    }

    @Test
    @DisplayName("toResponse copies every field, including values pulled from the nested user/problem")
    void copiesAllFields() {
        Submission submission = buildSubmission(true);

        SubmissionRecordResponse response = mapper.toResponse(submission);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getProblemSlug()).isEqualTo("two-sum");
        assertThat(response.getProblemName()).isEqualTo("Two Sum");
        assertThat(response.getUserEmail()).isEqualTo("alice@example.com");
        assertThat(response.getUserName()).isEqualTo("Alice");
        assertThat(response.getLanguage()).isEqualTo("javascript");
        assertThat(response.getCode()).isEqualTo("function twoSum() {}");
        assertThat(response.getApproach()).isEqualTo("hash map");
        assertThat(response.getPassedCount()).isEqualTo(3);
        assertThat(response.getTotalCount()).isEqualTo(3);
        assertThat(response.isAllPassed()).isTrue();
        assertThat(response.getAiFeedback()).isEqualTo("Nice work");
        assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @ParameterizedTest
    @DisplayName("toResponse preserves the solved flag exactly as it is on the entity")
    @ValueSource(booleans = {true, false})
    void preservesSolvedFlag(boolean solved) {
        SubmissionRecordResponse response = mapper.toResponse(buildSubmission(solved));

        assertThat(response.isSolved()).isEqualTo(solved);
    }

    @Test
    @DisplayName("toResponse carries markedBy through when an admin has touched the submission, null otherwise")
    void carriesMarkedByThroughWhenPresent() {
        Submission untouched = buildSubmission(false);
        assertThat(mapper.toResponse(untouched).getMarkedBy()).isNull();

        Submission touched = buildSubmission(false);
        touched.setMarkedBy("admin@leetai.com");
        assertThat(mapper.toResponse(touched).getMarkedBy()).isEqualTo("admin@leetai.com");
    }
}

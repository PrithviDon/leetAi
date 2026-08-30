package com.leetai.controller;

import com.leetai.dto.ProblemResponse;
import com.leetai.dto.TestCaseResult;
import com.leetai.dto.TestRunRequest;
import com.leetai.dto.TestRunResponse;
import com.leetai.model.Problem;
import com.leetai.service.CodeExecutionService;
import com.leetai.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Everything here is admin-only — enforced centrally in SecurityConfig via
 * the /api/admin/** rule, not re-checked per method here.
 */
@RestController
@RequestMapping("/api/admin/problems")
public class AdminProblemController {

    private final ProblemService problemService;
    private final CodeExecutionService codeExecutionService;

    public AdminProblemController(ProblemService problemService, CodeExecutionService codeExecutionService) {
        this.problemService = problemService;
        this.codeExecutionService = codeExecutionService;
    }

    /** Drafts and published problems both — the public listing hides drafts. */
    @GetMapping
    public List<ProblemResponse> listAll() {
        return problemService.listAllForAdmin();
    }

    /** Includes hidden test cases — needed to actually author/edit them. */
    @GetMapping("/{slug}")
    public ProblemResponse getOne(@PathVariable String slug) {
        return problemService.getForAdminBySlug(slug);
    }

    /**
     * Runs a reference solution through the exact same Docker sandbox real
     * submissions use, against ALL of this problem's test cases (including
     * hidden ones) — lets an admin catch a broken expected-output value
     * before publishing. Deliberately skips the AI review step: this is a
     * fast correctness check, not a coaching session.
     */
    @PostMapping("/{slug}/test-run")
    public TestRunResponse testRun(@PathVariable String slug, @Valid @RequestBody TestRunRequest request) {
        Problem problem = problemService.getEntityBySlug(slug);
        List<TestCaseResult> results = codeExecutionService.run(
                request.getCode(), request.getLanguage(), problem.getFunctionName(), problem.getTestCases());

        TestRunResponse response = new TestRunResponse();
        response.setResults(results);
        response.setTotalCount(results.size());
        response.setPassedCount((int) results.stream().filter(TestCaseResult::isPassed).count());
        response.setAllPassed(results.stream().allMatch(TestCaseResult::isPassed));
        return response;
    }
}

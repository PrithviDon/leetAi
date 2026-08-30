package com.leetai.service;

import com.leetai.dto.CreateProblemRequest;
import com.leetai.dto.ProblemResponse;
import com.leetai.dto.TestCaseInput;
import com.leetai.dto.TestCaseResponse;
import com.leetai.model.Problem;
import com.leetai.model.TestCase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Sole responsibility: convert between persistence entities and API DTOs.
 * Keeps mapping logic out of the service (business rules) and controller
 * (HTTP concerns).
 */
@Component
public class ProblemMapper {

    public Problem toEntity(CreateProblemRequest req, String slug) {
        Problem problem = new Problem();
        problem.setSlug(slug);
        problem.setName(req.getName());
        problem.setDescription(req.getDescription());
        problem.setFunctionName(req.getFunctionName());
        problem.setStarterCodeJs(req.getStarterCodeJs());
        problem.setStarterCodePython(req.getStarterCodePython());
        problem.setDifficulty(parseDifficulty(req.getDifficulty()));

        List<TestCase> testCases = req.getTestCases().stream()
                .map(tcIn -> toTestCaseEntity(tcIn, problem))
                .collect(Collectors.toList());
        problem.setTestCases(testCases);

        return problem;
    }

    public ProblemResponse toResponse(Problem problem) {
        return toResponse(problem, false);
    }

    /**
     * @param includeHidden true for admin views (authoring/editing a problem
     *                      needs to see hidden test cases too); false for
     *                      anything a normal user might see.
     */
    public ProblemResponse toResponse(Problem problem, boolean includeHidden) {
        ProblemResponse res = new ProblemResponse();
        res.setId(problem.getId());
        res.setSlug(problem.getSlug());
        res.setName(problem.getName());
        res.setDescription(problem.getDescription());
        res.setDifficulty(problem.getDifficulty() != null ? problem.getDifficulty().name() : null);
        res.setFunctionName(problem.getFunctionName());
        res.setStarterCodeJs(problem.getStarterCodeJs());
        res.setStarterCodePython(problem.getStarterCodePython());
        res.setStatus(problem.getStatus() != null ? problem.getStatus().name() : null);
        res.setCreatedBy(problem.getCreatedBy());
        res.setUpdatedBy(problem.getUpdatedBy());

        List<TestCaseResponse> testCases = problem.getTestCases().stream()
                .filter(tc -> includeHidden || !tc.isHidden())
                .map(tc -> new TestCaseResponse(tc.getInputJson(), tc.getExpectedOutputJson()))
                .collect(Collectors.toList());
        res.setTestCases(testCases);

        return res;
    }

    /**
     * Applies a full update onto an existing entity in place. Slug is left
     * untouched by design — callers who want to change it can do so
     * explicitly via the entity, but the API doesn't do it implicitly.
     */
    public void applyUpdate(Problem existing, CreateProblemRequest req) {
        existing.setName(req.getName());
        existing.setDescription(req.getDescription());
        existing.setFunctionName(req.getFunctionName());
        existing.setStarterCodeJs(req.getStarterCodeJs());
        existing.setStarterCodePython(req.getStarterCodePython());
        existing.setDifficulty(parseDifficulty(req.getDifficulty()));

        // Replace test cases wholesale. orphanRemoval=true on Problem.testCases
        // means the old rows get deleted rather than left dangling.
        existing.getTestCases().clear();
        req.getTestCases().stream()
                .map(tcIn -> toTestCaseEntity(tcIn, existing))
                .forEach(existing.getTestCases()::add);
    }

    private TestCase toTestCaseEntity(TestCaseInput in, Problem owner) {
        TestCase tc = new TestCase();
        tc.setProblem(owner);
        tc.setInputJson(in.getInput());
        tc.setExpectedOutputJson(in.getExpectedOutput());
        tc.setHidden(in.isHidden());
        return tc;
    }

    private Problem.Difficulty parseDifficulty(String raw) {
        if (raw == null || raw.isBlank()) return Problem.Difficulty.MEDIUM;
        try {
            return Problem.Difficulty.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Problem.Difficulty.MEDIUM;
        }
    }
}

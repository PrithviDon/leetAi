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
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/problems/{slug}")
public class SubmissionController {

    private final ProblemService problemService;
    private final CodeExecutionService codeExecutionService;
    private final AiAssistantService aiAssistantService;
    private final RateLimitService rateLimitService;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final SubmissionMapper submissionMapper;

    public SubmissionController(ProblemService problemService,
                                 CodeExecutionService codeExecutionService,
                                 AiAssistantService aiAssistantService,
                                 RateLimitService rateLimitService,
                                 SubmissionRepository submissionRepository,
                                 UserRepository userRepository,
                                 SubmissionMapper submissionMapper) {
        this.problemService = problemService;
        this.codeExecutionService = codeExecutionService;
        this.aiAssistantService = aiAssistantService;
        this.rateLimitService = rateLimitService;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.submissionMapper = submissionMapper;
    }

    @PostMapping("/submit")
    public SubmissionResponse submit(@PathVariable String slug, @Valid @RequestBody SubmissionRequest req,
                                      Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        // Admins already have an unlimited /test-run endpoint for iterating
        // on problems — the submission limit is about protecting the Docker
        // host from real-user submission bursts, not gatekeeping admins.
        if (!isAdmin) {
            rateLimitService.checkSubmissionAllowed(authentication.getName());
        }

        // Full entity (including hidden test cases) — needed for real grading.
        Problem problem = problemService.getEntityBySlug(slug);

        if (problem.getStatus() != Problem.Status.PUBLISHED && !isAdmin) {
            // Drafts don't exist as far as a regular user is concerned.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found: " + slug);
        }

        List<TestCaseResult> results = codeExecutionService.run(
                req.getCode(), req.getLanguage(), problem.getFunctionName(), problem.getTestCases());

        boolean allPassed = results.stream().allMatch(TestCaseResult::isPassed);
        long passedCount = results.stream().filter(TestCaseResult::isPassed).count();

        String aiFeedback = aiAssistantService.reviewSubmission(
                problem, req.getCode(), req.getLanguage(), req.getApproach(), results);

        // Persist the attempt so it shows up in the user's own history and in
        // the admin submissions view. A submission is auto-marked solved when
        // every test case passes; an admin can flip that later either way.
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setCode(req.getCode());
        submission.setLanguage(req.getLanguage());
        submission.setApproach(req.getApproach());
        submission.setPassedCount((int) passedCount);
        submission.setTotalCount(results.size());
        submission.setAllPassed(allPassed);
        submission.setSolved(allPassed);
        submission.setAiFeedback(aiFeedback);
        Submission saved = submissionRepository.save(submission);

        SubmissionResponse response = new SubmissionResponse();
        response.setSubmissionId(saved.getId());
        response.setAllPassed(allPassed);
        response.setSolved(saved.isSolved());
        response.setPassedCount((int) passedCount);
        response.setTotalCount(results.size());
        response.setResults(results);
        response.setAiFeedback(aiFeedback);
        return response;
    }

    /** The current user's own past submissions for this problem, most recent first. */
    @GetMapping("/submissions")
    public List<SubmissionRecordResponse> mySubmissions(@PathVariable String slug, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        return submissionRepository.findByUser_IdAndProblem_SlugOrderByCreatedAtDesc(user.getId(), slug).stream()
                .map(submissionMapper::toResponse)
                .toList();
    }
}

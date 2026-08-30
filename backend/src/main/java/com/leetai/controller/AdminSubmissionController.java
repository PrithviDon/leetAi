package com.leetai.controller;

import com.leetai.dto.SubmissionRecordResponse;
import com.leetai.model.Submission;
import com.leetai.repository.SubmissionRepository;
import com.leetai.service.SubmissionMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Everything here is admin-only — enforced centrally in SecurityConfig via
 * the /api/admin/** rule, not re-checked per method here.
 */
@RestController
@RequestMapping("/api/admin/submissions")
public class AdminSubmissionController {

    private final SubmissionRepository submissionRepository;
    private final SubmissionMapper submissionMapper;

    public AdminSubmissionController(SubmissionRepository submissionRepository,
                                      SubmissionMapper submissionMapper) {
        this.submissionRepository = submissionRepository;
        this.submissionMapper = submissionMapper;
    }

    /**
     * Browse submissions across all users, newest first. Optional filters:
     *   ?slug=two-sum          → only submissions for that problem
     *   &email=alice           → fuzzy match on submitter email
     *   &page=0&size=20        → zero-based page, default 20 per page
     */
    @GetMapping
    public Page<SubmissionRecordResponse> list(
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) String email,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String slugParam = (slug == null || slug.isBlank()) ? null : slug.trim();
        String emailParam = (email == null || email.isBlank()) ? null : email.trim();

        return submissionRepository.search(slugParam, emailParam, pageable)
                .map(submissionMapper::toResponse);
    }

    /** Marks a specific submission (and therefore that user's problem) as solved. */
    @PatchMapping("/{id}/mark-solved")
    public SubmissionRecordResponse markSolved(@PathVariable Long id, Authentication authentication) {
        return setSolved(id, true, authentication.getName());
    }

    /** Reverses a previous mark — the submission (and the user's "solved" status from it) no longer counts. */
    @PatchMapping("/{id}/unmark-solved")
    public SubmissionRecordResponse unmarkSolved(@PathVariable Long id, Authentication authentication) {
        return setSolved(id, false, authentication.getName());
    }

    private SubmissionRecordResponse setSolved(Long id, boolean solved, String adminEmail) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found: " + id));
        submission.setSolved(solved);
        submission.setMarkedBy(adminEmail);
        Submission saved = submissionRepository.save(submission);
        return submissionMapper.toResponse(saved);
    }
}

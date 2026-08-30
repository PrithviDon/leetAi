package com.leetai.controller;

import com.leetai.dto.ResetSolvedResponse;
import com.leetai.dto.SolvedUserResponse;
import com.leetai.model.Problem;
import com.leetai.model.Submission;
import com.leetai.repository.SolvedUserProjection;
import com.leetai.repository.SubmissionRepository;
import com.leetai.service.ProblemService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Admin-only (enforced centrally in SecurityConfig via /api/admin/**).
 * These endpoints operate on a user's solved status for a problem as a
 * whole — not on one submission at a time — because a user can have
 * multiple passing submissions for the same problem, and flipping just one
 * of them wouldn't actually change whether the problem shows as solved.
 */
@RestController
@RequestMapping("/api/admin/problems/{slug}")
public class AdminProblemProgressController {

    private final ProblemService problemService;
    private final SubmissionRepository submissionRepository;

    public AdminProblemProgressController(ProblemService problemService,
                                           SubmissionRepository submissionRepository) {
        this.problemService = problemService;
        this.submissionRepository = submissionRepository;
    }

    /** Everyone who currently has this problem marked solved. */
    @GetMapping("/solved-users")
    public List<SolvedUserResponse> solvedUsers(@PathVariable String slug) {
        // 404s early if the slug doesn't exist at all, same behavior as the
        // other admin problem endpoints.
        problemService.getEntityBySlug(slug);

        List<SolvedUserProjection> rows = submissionRepository.findSolvedUsersForProblem(slug);
        return rows.stream()
                .map(r -> new SolvedUserResponse(
                        r.getUserId(), r.getUserEmail(), r.getUserName(),
                        r.getFirstSolvedAt(), r.getSubmissionCount()))
                .toList();
    }

    /**
     * Un-marks the problem as solved for this user — flips every solved
     * submission of theirs for this problem back to unsolved. Safe to call
     * even if they aren't currently marked solved (just resets 0 rows).
     */
    @PostMapping("/users/{userId}/reset-solved")
    public ResetSolvedResponse resetSolved(@PathVariable String slug, @PathVariable Long userId,
                                            Authentication authentication) {
        Problem problem = problemService.getEntityBySlug(slug);
        int changed = submissionRepository.resetSolvedForUserAndProblem(userId, problem.getId(), authentication.getName());
        return new ResetSolvedResponse(userId, slug, changed);
    }

    /**
     * Marks the problem solved for this user by flipping their most recent
     * submission for it. Requires that they have at least one submission on
     * record — there's no code to attach a "solved" status to otherwise.
     */
    @PostMapping("/users/{userId}/mark-solved")
    public SolvedUserResponse markSolved(@PathVariable String slug, @PathVariable Long userId,
                                          Authentication authentication) {
        List<Submission> history = submissionRepository
                .findByUser_IdAndProblem_SlugOrderByCreatedAtDesc(userId, slug);
        if (history.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This user has no submissions for " + slug + " to mark as solved.");
        }
        Submission mostRecent = history.get(0);
        mostRecent.setSolved(true);
        mostRecent.setMarkedBy(authentication.getName());
        submissionRepository.save(mostRecent);

        return new SolvedUserResponse(
                mostRecent.getUser().getId(), mostRecent.getUser().getEmail(), mostRecent.getUser().getName(),
                mostRecent.getCreatedAt(), history.stream().filter(Submission::isSolved).count());
    }
}

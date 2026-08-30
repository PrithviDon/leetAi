package com.leetai.controller;

import com.leetai.dto.CreateProblemRequest;
import com.leetai.dto.PagedResponse;
import com.leetai.dto.ProblemResponse;
import com.leetai.dto.ProblemSearchResult;
import com.leetai.dto.ProgressResponse;
import com.leetai.model.Problem;
import com.leetai.repository.ProblemRepository;
import com.leetai.repository.SubmissionRepository;
import com.leetai.service.ElasticsearchSearchService;
import com.leetai.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final ElasticsearchSearchService searchService;
    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;

    public ProblemController(ProblemService problemService,
                              ElasticsearchSearchService searchService,
                              SubmissionRepository submissionRepository,
                              ProblemRepository problemRepository) {
        this.problemService = problemService;
        this.searchService = searchService;
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
    }

    /**
     * Slugs the current caller has marked solved, or an empty set for
     * anonymous requests. GET /api/problems* is permitAll, but JwtAuthFilter
     * still populates the SecurityContext when a valid Bearer token is sent,
     * so logged-in users get their solved badges without a second round trip.
     */
    private Set<String> resolveSolvedSlugs(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Set.of();
        }
        return submissionRepository.findSolvedSlugsByUserEmail(authentication.getName());
    }

    /**
     * Search published problems via Elasticsearch with pagination.
     * All params are optional:
     *   ?search=two sum         → fuzzy multi_match on name + description
     *   &difficulty=EASY        → filter by difficulty (EASY/MEDIUM/HARD/ALL)
     *   &page=0&size=10         → zero-based page, default 10 per page
     *
     * Falls back to listPublished() from MySQL when ES is unavailable.
     */
    @GetMapping
    public PagedResponse<?> listProblems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String difficulty,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Authentication authentication) {

        // When no search/filter params given and page=0, check if it's
        // a simple "load the home page" call — ES still handles it cleanly
        // (match_all + status=PUBLISHED) so just let ES do it.
        Pageable pageable = PageRequest.of(page, size, Sort.by("_score").descending());
        Set<String> solvedSlugs = resolveSolvedSlugs(authentication);
        try {
            Page<ProblemSearchResult> results = searchService.search(search, difficulty, pageable);
            results.getContent().forEach(r -> r.setSolved(solvedSlugs.contains(r.getSlug())));
            // Built explicitly rather than returning `results` directly — see
            // PagedResponse's javadoc for why we don't trust Page's own
            // Jackson serialization to stay stable across Spring Data versions.
            return new PagedResponse<>(results.getContent(), results.getNumber(), results.getSize(),
                    results.getTotalElements(), results.getTotalPages());
        } catch (Exception e) {
            // ES is down or index doesn't exist yet — fall back to MySQL.
            // Users get results; search just isn't fuzzy until ES is back.
            // listPublished() isn't itself paginated, so this fallback
            // reports everything as a single page.
            List<ProblemResponse> results = problemService.listPublished();
            results.forEach(r -> r.setSolved(solvedSlugs.contains(r.getSlug())));
            return new PagedResponse<>(results, 0, results.size(), results.size(), 1);
        }
    }

    /** 404s if the problem is still a draft — use /api/admin/problems/{slug} to preview drafts. */
    @GetMapping("/{slug}")
    public ProblemResponse getProblem(@PathVariable String slug, Authentication authentication) {
        ProblemResponse response = problemService.getPublishedBySlug(slug);
        response.setSolved(resolveSolvedSlugs(authentication).contains(slug));
        return response;
    }

    /**
     * Solved/total counter for the problem listing page — computed directly
     * from the DB rather than the (possibly paginated) list response, so it
     * stays correct regardless of page size. Anonymous callers get 0 solved.
     */
    @GetMapping("/progress")
    public ProgressResponse progress(Authentication authentication) {
        long total = problemRepository.countByStatus(Problem.Status.PUBLISHED);
        long solved = resolveSolvedSlugs(authentication).size();
        return new ProgressResponse(solved, total);
    }

    /**
     * Admin only (enforced in SecurityConfig). Creates a problem as DRAFT —
     * publish it separately once you've verified the test cases.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemResponse createProblem(@Valid @RequestBody CreateProblemRequest request,
                                          Authentication authentication) {
        return problemService.create(request, authentication.getName());
    }

    /** Admin only. Full replace of fields + test cases; slug stays fixed. */
    @PutMapping("/{slug}")
    public ProblemResponse updateProblem(@PathVariable String slug,
                                          @Valid @RequestBody CreateProblemRequest request,
                                          Authentication authentication) {
        return problemService.update(slug, request, authentication.getName());
    }

    /** Admin only. */
    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProblem(@PathVariable String slug) {
        problemService.delete(slug);
    }

    /** Admin only. Makes a draft visible to regular users. */
    @PatchMapping("/{slug}/publish")
    public ProblemResponse publish(@PathVariable String slug, Authentication authentication) {
        return problemService.publish(slug, authentication.getName());
    }

    /** Admin only. Pulls a problem back to draft without deleting it. */
    @PatchMapping("/{slug}/unpublish")
    public ProblemResponse unpublish(@PathVariable String slug, Authentication authentication) {
        return problemService.unpublish(slug, authentication.getName());
    }
}

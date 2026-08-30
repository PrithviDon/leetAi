package com.leetai.service;

import com.leetai.dto.CreateProblemRequest;
import com.leetai.dto.ProblemResponse;
import com.leetai.model.Problem;

import java.util.List;

public interface ProblemService {

    /** Published problems only, hidden test cases excluded — what regular users see. */
    List<ProblemResponse> listPublished();

    /** All problems (draft + published), hidden test cases excluded from the list view. Admin only. */
    List<ProblemResponse> listAllForAdmin();

    /** One published problem by slug, safe to show to users. 404s if it's a draft. */
    ProblemResponse getPublishedBySlug(String slug);

    /** One problem by slug regardless of status, hidden test cases included. Admin only. */
    ProblemResponse getForAdminBySlug(String slug);

    /**
     * The raw entity, including hidden test cases and regardless of status.
     * For internal use only (grading, AI review) — never return this
     * directly from a controller.
     */
    Problem getEntityBySlug(String slug);

    /** Creates a new problem (starts as DRAFT) and returns it. Admin only. */
    ProblemResponse create(CreateProblemRequest request, String adminEmail);

    /**
     * Replaces an existing problem's fields and test cases (full update).
     * The slug is preserved even if the name changes. Admin only.
     */
    ProblemResponse update(String slug, CreateProblemRequest request, String adminEmail);

    /** Deletes a problem and its test cases. Admin only. */
    void delete(String slug);

    /** Makes a problem visible to regular users. Admin only. */
    ProblemResponse publish(String slug, String adminEmail);

    /** Hides a problem from regular users again without deleting it. Admin only. */
    ProblemResponse unpublish(String slug, String adminEmail);
}

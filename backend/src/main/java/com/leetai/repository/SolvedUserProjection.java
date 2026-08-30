package com.leetai.repository;

import java.time.Instant;

/**
 * One row per user who has a problem marked solved — aggregated across all
 * of that user's submissions for the problem. Backs the admin "who solved
 * this" panel used for the reset feature.
 */
public interface SolvedUserProjection {
    Long getUserId();
    String getUserEmail();
    String getUserName();
    Instant getFirstSolvedAt();
    Long getSubmissionCount();
}

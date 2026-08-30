package com.leetai.repository;

import com.leetai.model.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /** A user's own submission history for one problem, most recent first. */
    List<Submission> findByUser_IdAndProblem_SlugOrderByCreatedAtDesc(Long userId, String problemSlug);

    /** Whether this user has a submission counted as solved for this problem. */
    boolean existsByUser_IdAndProblem_IdAndSolvedTrue(Long userId, Long problemId);

    /** Slugs of every problem the given user currently has marked solved — used to badge problem lists. */
    @Query("select distinct s.problem.slug from Submission s where s.user.email = :email and s.solved = true")
    Set<String> findSolvedSlugsByUserEmail(@Param("email") String email);

    /** Admin browsing: all submissions, optionally filtered by problem slug and/or submitter email. */
    @Query("select s from Submission s where " +
            "(:slug is null or s.problem.slug = :slug) and " +
            "(:email is null or lower(s.user.email) like lower(concat('%', :email, '%'))) " +
            "order by s.createdAt desc")
    Page<Submission> search(@Param("slug") String slug, @Param("email") String email, Pageable pageable);

    /**
     * Everyone who currently has this problem marked solved, one row per
     * user, aggregated across however many of their submissions are solved.
     * Backs the admin "solved users" panel for a given problem.
     */
    @Query("select s.user.id as userId, s.user.email as userEmail, s.user.name as userName, " +
            "min(s.createdAt) as firstSolvedAt, count(s) as submissionCount " +
            "from Submission s where s.problem.slug = :slug and s.solved = true " +
            "group by s.user.id, s.user.email, s.user.name " +
            "order by min(s.createdAt) asc")
    List<SolvedUserProjection> findSolvedUsersForProblem(@Param("slug") String slug);

    /**
     * Resets a user's solved status for a problem by flipping every one of
     * their solved submissions for it back to unsolved — a single submission
     * toggle isn't enough if they have multiple passing attempts on record.
     * Returns how many rows were changed (0 means they weren't marked solved).
     *
     * clearAutomatically = true: bulk JPQL updates bypass the persistence
     * context, so without this, an entity loaded earlier in the same
     * transaction could still look "solved" in memory even after this query
     * has changed it in the DB. Not currently a live bug in the caller
     * (AdminProblemProgressController doesn't re-read Submissions after
     * calling this), but it's a footgun waiting for the next person to
     * extend that method — cheap to close off now, found while writing the
     * repository tests for this method.
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update Submission s set s.solved = false, s.markedBy = :adminEmail " +
            "where s.user.id = :userId and s.problem.id = :problemId and s.solved = true")
    int resetSolvedForUserAndProblem(@Param("userId") Long userId, @Param("problemId") Long problemId,
                                      @Param("adminEmail") String adminEmail);
}

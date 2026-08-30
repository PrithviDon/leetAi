package com.leetai.repository;

import com.leetai.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    Optional<Problem> findBySlug(String slug);
    long countByStatus(Problem.Status status);
}

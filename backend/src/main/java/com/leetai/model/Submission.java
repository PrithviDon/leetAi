package com.leetai.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A persisted record of a single grading attempt. Created every time a user
 * hits /submit. Doubles as the source of truth for "has this user solved
 * this problem": `solved` starts out equal to whatever the autograder said
 * (allPassed), but an admin can flip it independently via the admin
 * submissions endpoints — e.g. to award credit when the grader was too
 * strict, or to revoke a bogus pass.
 */
@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String code;

    private String language; // "javascript" | "python"

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String approach;

    private int passedCount;
    private int totalCount;

    // What the autograder decided at submit time. Never changes after the fact.
    private boolean allPassed;

    // The status that actually counts for "problem marked as solved" — starts
    // out equal to allPassed, but admins can override it later.
    private boolean solved;

    // Email of the admin who last changed `solved` by hand; null if the
    // current value is just whatever the autograder produced.
    private String markedBy;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String aiFeedback;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Problem getProblem() { return problem; }
    public void setProblem(Problem problem) { this.problem = problem; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getApproach() { return approach; }
    public void setApproach(String approach) { this.approach = approach; }
    public int getPassedCount() { return passedCount; }
    public void setPassedCount(int passedCount) { this.passedCount = passedCount; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public boolean isAllPassed() { return allPassed; }
    public void setAllPassed(boolean allPassed) { this.allPassed = allPassed; }
    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }
    public String getMarkedBy() { return markedBy; }
    public void setMarkedBy(String markedBy) { this.markedBy = markedBy; }
    public String getAiFeedback() { return aiFeedback; }
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

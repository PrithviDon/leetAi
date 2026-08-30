package com.leetai.dto;

import java.time.Instant;

/**
 * A persisted Submission, shaped for two audiences:
 *   - a user looking at their own history for a problem
 *   - an admin browsing/marking submissions across all users
 * Both get the same shape; the admin endpoints just aren't scoped to one user.
 */
public class SubmissionRecordResponse {
    private Long id;
    private String problemSlug;
    private String problemName;
    private String userEmail;
    private String userName;
    private String language;
    private String code;
    private String approach;
    private int passedCount;
    private int totalCount;
    private boolean allPassed;
    private boolean solved;
    private String markedBy;
    private String aiFeedback;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProblemSlug() { return problemSlug; }
    public void setProblemSlug(String problemSlug) { this.problemSlug = problemSlug; }
    public String getProblemName() { return problemName; }
    public void setProblemName(String problemName) { this.problemName = problemName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
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

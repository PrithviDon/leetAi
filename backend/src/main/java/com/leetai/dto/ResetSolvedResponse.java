package com.leetai.dto;

public class ResetSolvedResponse {
    private Long userId;
    private String problemSlug;
    private int submissionsReset;

    public ResetSolvedResponse() {}

    public ResetSolvedResponse(Long userId, String problemSlug, int submissionsReset) {
        this.userId = userId;
        this.problemSlug = problemSlug;
        this.submissionsReset = submissionsReset;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getProblemSlug() { return problemSlug; }
    public void setProblemSlug(String problemSlug) { this.problemSlug = problemSlug; }
    public int getSubmissionsReset() { return submissionsReset; }
    public void setSubmissionsReset(int submissionsReset) { this.submissionsReset = submissionsReset; }
}

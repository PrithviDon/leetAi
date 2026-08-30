package com.leetai.dto;

import java.time.Instant;

public class SolvedUserResponse {
    private Long userId;
    private String userEmail;
    private String userName;
    private Instant firstSolvedAt;
    private long submissionCount;

    public SolvedUserResponse() {}

    public SolvedUserResponse(Long userId, String userEmail, String userName,
                               Instant firstSolvedAt, long submissionCount) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.firstSolvedAt = firstSolvedAt;
        this.submissionCount = submissionCount;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Instant getFirstSolvedAt() { return firstSolvedAt; }
    public void setFirstSolvedAt(Instant firstSolvedAt) { this.firstSolvedAt = firstSolvedAt; }
    public long getSubmissionCount() { return submissionCount; }
    public void setSubmissionCount(long submissionCount) { this.submissionCount = submissionCount; }
}

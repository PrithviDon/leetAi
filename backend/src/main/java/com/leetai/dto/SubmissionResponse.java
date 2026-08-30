package com.leetai.dto;

import java.util.List;

public class SubmissionResponse {

    private Long submissionId;
    private boolean allPassed;
    private int passedCount;
    private int totalCount;
    private List<TestCaseResult> results;

    // Whether this attempt counts as the problem being solved. Equal to
    // allPassed at submit time, but can later be flipped by an admin — this
    // field just reflects what was true the moment the submission was made.
    private boolean solved;

    // AI assistant's natural-language verdict: correctness explanation,
    // feedback on the approach, complexity notes, hints if it failed.
    private String aiFeedback;

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }
    public boolean isAllPassed() { return allPassed; }
    public void setAllPassed(boolean allPassed) { this.allPassed = allPassed; }
    public int getPassedCount() { return passedCount; }
    public void setPassedCount(int passedCount) { this.passedCount = passedCount; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public List<TestCaseResult> getResults() { return results; }
    public void setResults(List<TestCaseResult> results) { this.results = results; }
    public String getAiFeedback() { return aiFeedback; }
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }
}

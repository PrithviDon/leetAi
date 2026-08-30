package com.leetai.dto;

import java.util.List;

public class TestRunResponse {
    private boolean allPassed;
    private int passedCount;
    private int totalCount;
    private List<TestCaseResult> results;

    public boolean isAllPassed() { return allPassed; }
    public void setAllPassed(boolean allPassed) { this.allPassed = allPassed; }
    public int getPassedCount() { return passedCount; }
    public void setPassedCount(int passedCount) { this.passedCount = passedCount; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public List<TestCaseResult> getResults() { return results; }
    public void setResults(List<TestCaseResult> results) { this.results = results; }
}

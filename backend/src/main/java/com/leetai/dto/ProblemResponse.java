package com.leetai.dto;

import java.util.List;

/**
 * What the frontend actually receives. Hidden test cases are deliberately
 * excluded here (filtered in ProblemMapper) — they're used for grading in
 * SubmissionController but never shown to the user.
 */
public class ProblemResponse {
    private Long id;
    private String slug;
    private String name;
    private String description;
    private String difficulty;
    private String functionName;
    private String starterCodeJs;
    private String starterCodePython;
    private List<TestCaseResponse> testCases;
    private String status;      // DRAFT | PUBLISHED
    private String createdBy;
    private String updatedBy;

    // Whether the currently authenticated user has this marked as solved.
    // Always false for anonymous requests or when no auth context is present.
    private boolean solved;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getFunctionName() { return functionName; }
    public void setFunctionName(String functionName) { this.functionName = functionName; }
    public String getStarterCodeJs() { return starterCodeJs; }
    public void setStarterCodeJs(String starterCodeJs) { this.starterCodeJs = starterCodeJs; }
    public String getStarterCodePython() { return starterCodePython; }
    public void setStarterCodePython(String starterCodePython) { this.starterCodePython = starterCodePython; }
    public List<TestCaseResponse> getTestCases() { return testCases; }
    public void setTestCases(List<TestCaseResponse> testCases) { this.testCases = testCases; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }
}

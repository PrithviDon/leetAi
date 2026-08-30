package com.leetai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Body for POST /api/problems — lets problems be added programmatically
 * (e.g. via curl/Postman/a seeding script) without any admin UI.
 */
public class CreateProblemRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    // EASY | MEDIUM | HARD — defaults to MEDIUM if omitted
    private String difficulty;

    @NotBlank
    private String functionName;

    private String starterCodeJs;
    private String starterCodePython;

    @NotEmpty
    @Valid
    private List<TestCaseInput> testCases;

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
    public List<TestCaseInput> getTestCases() { return testCases; }
    public void setTestCases(List<TestCaseInput> testCases) { this.testCases = testCases; }
}

package com.leetai.dto;

import jakarta.validation.constraints.NotBlank;

public class TestCaseInput {

    @NotBlank
    private String input;          // JSON-encoded args, e.g. "[[2,7,11,15], 9]"

    @NotBlank
    private String expectedOutput; // JSON-encoded expected result, e.g. "[0,1]"

    private boolean hidden = false;

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}

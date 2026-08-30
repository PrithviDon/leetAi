package com.leetai.dto;

public class TestCaseResponse {
    private String input;
    private String expectedOutput;

    public TestCaseResponse() {}

    public TestCaseResponse(String input, String expectedOutput) {
        this.input = input;
        this.expectedOutput = expectedOutput;
    }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
}

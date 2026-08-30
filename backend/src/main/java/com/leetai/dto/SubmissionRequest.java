package com.leetai.dto;

import jakarta.validation.constraints.NotBlank;

public class SubmissionRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String language; // "javascript" | "python"

    // The user's plain-English explanation of their approach. Optional but
    // strongly encouraged — this is what the AI assistant reasons about.
    private String approach;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getApproach() { return approach; }
    public void setApproach(String approach) { this.approach = approach; }
}

package com.leetai.service;

import com.leetai.dto.TestCaseResult;
import com.leetai.model.TestCase;

import java.util.List;

public interface CodeExecutionService {
    /**
     * Actually runs user code against each test case and returns real
     * results. This is the sole source of truth for correctness — nothing
     * downstream (including the AI) overrides what comes back from here.
     */
    List<TestCaseResult> run(String code, String language, String functionName, List<TestCase> testCases);
}

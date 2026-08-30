package com.leetai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.leetai.dto.TestCaseResult;
import com.leetai.model.Problem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Calls an LLM to explain WHY the (already-executed) test results came out
 * the way they did, critique the user's stated approach, and give hints.
 * The AI never overrides pass/fail — CodeExecutionService already decided
 * that from real execution. The AI's job is explanation and coaching.
 *
 * Supports two providers, switched via `ai.provider`:
 *  - "claude": Anthropic API (needs ANTHROPIC_API_KEY)
 *  - "ollama": local Llama model served by Ollama (needs `ollama serve` running)
 */
@Service
public class AiAssistantService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.provider}")
    private String provider;

    @Value("${anthropic.api.key}")
    private String anthropicKey;
    @Value("${anthropic.api.model}")
    private String anthropicModel;
    @Value("${anthropic.api.url}")
    private String anthropicUrl;

    @Value("${ollama.api.url}")
    private String ollamaUrl;
    @Value("${ollama.model}")
    private String ollamaModel;

    public String reviewSubmission(Problem problem, String code, String language,
                                    String approach, List<TestCaseResult> results) {

        long passedCount = results.stream().filter(TestCaseResult::isPassed).count();

        StringBuilder resultsSummary = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            TestCaseResult r = results.get(i);
            resultsSummary.append(String.format(
                    "Test %d: input=%s expected=%s actual=%s passed=%s%s%n",
                    i + 1, r.getInput(), r.getExpected(), r.getActual(), r.isPassed(),
                    (r.getStderr() != null && !r.getStderr().isBlank()) ? " stderr=" + r.getStderr() : ""
            ));
        }

        String systemPrompt = """
            You are an AI coding coach embedded in a LeetCode-style practice platform.
            You are given: the problem statement, the user's stated approach (may be empty),
            their submitted code, and REAL execution results against test cases (already run
            by a sandbox — you do not decide correctness, it is already known).

            Write a short, encouraging but honest review:
            1. State clearly whether the solution passed (%d/%d tests).
            2. If it failed, explain the likely root cause by reasoning about the failing
               test case(s) — don't just repeat the numbers.
            3. Comment on their stated approach: is it correct in principle? What's the
               time/space complexity? Is there a better approach?
            4. If it passed, briefly note complexity and one thing to consider for edge cases
               or an optimization, if any.
            Keep it concise — a few short paragraphs or a tight bullet list, not an essay.
            Never reveal a full alternate solution unless the user explicitly asks for one;
            give a nudge/hint instead if they're stuck.
            """.formatted(passedCount, results.size());

        String userMsg = """
            Problem: %s (%s)
            %s

            User's stated approach:
            %s

            Submitted code (%s):
            %s

            Execution results:
            %s
            """.formatted(
                problem.getName(), problem.getDifficulty(),
                problem.getDescription(),
                (approach == null || approach.isBlank()) ? "(none provided)" : approach,
                language, code,
                resultsSummary
        );

        try {
            if ("ollama".equalsIgnoreCase(provider)) {
                return callOllama(systemPrompt, userMsg);
            }
            return callClaude(systemPrompt, userMsg);
        } catch (Exception e) {
            return "AI feedback unavailable right now (" + e.getMessage() + "). "
                    + "Test results: " + passedCount + "/" + results.size() + " passed.";
        }
    }

    private String callClaude(String systemPrompt, String userMsg) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", anthropicKey);
        headers.set("anthropic-version", "2023-06-01");

        var body = Map.of(
                "model", anthropicModel,
                "max_tokens", 700,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMsg))
        );

        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        JsonNode resp = restTemplate.postForObject(anthropicUrl, request, JsonNode.class);

        StringBuilder text = new StringBuilder();
        for (JsonNode block : resp.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        return text.toString();
    }

    private String callOllama(String systemPrompt, String userMsg) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var body = Map.of(
                "model", ollamaModel,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMsg)
                )
        );

        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        // Ollama's native chat endpoint: POST http://localhost:11434/api/chat
        JsonNode resp = restTemplate.postForObject(ollamaUrl, request, JsonNode.class);
        return resp.path("message").path("content").asText("(no response from local model)");
    }
}

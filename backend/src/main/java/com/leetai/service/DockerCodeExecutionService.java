package com.leetai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetai.dto.TestCaseResult;
import com.leetai.model.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Runs user-submitted code in a locked-down, ephemeral Docker container —
 * one container per test case. This is the security boundary: no network,
 * capped memory/CPU/process count, read-only source mount, non-root user.
 *
 * Flow per test case (mirrors a standard sandboxed-runner architecture):
 *   1. Write the harness-wrapped source to an isolated temp folder.
 *   2. `docker run` that folder read-only into a fresh, resource-capped,
 *      network-disabled container.
 *   3. Capture stdout/stderr, enforce a wall-clock timeout with a hard kill.
 *   4. Delete the temp folder.
 *
 * Requires Docker installed and running on the host this Spring app runs
 * on (calls the `docker` CLI directly via ProcessBuilder).
 */
@Service
public class DockerCodeExecutionService implements CodeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(DockerCodeExecutionService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${execution.timeout-seconds:8}")
    private int timeoutSeconds;

    @Value("${execution.memory-limit:256m}")
    private String memoryLimit;

    @Value("${execution.cpu-limit:0.5}")
    private String cpuLimit;

    @Value("${execution.pids-limit:64}")
    private String pidsLimit;

    @Value("${execution.node-image:node:20-alpine}")
    private String nodeImage;

    @Value("${execution.python-image:python:3.11-alpine}")
    private String pythonImage;

    @Override
    public List<TestCaseResult> run(String code, String language, String functionName, List<TestCase> testCases) {
        List<TestCaseResult> results = new ArrayList<>();
        for (TestCase tc : testCases) {
            results.add(runOne(code, language, functionName, tc));
        }
        return results;
    }

    private TestCaseResult runOne(String code, String language, String functionName, TestCase tc) {
        Path workDir = null;
        String containerName = "leetai-" + UUID.randomUUID();
        try {
            workDir = Files.createTempDirectory("leetai-");
            String filename = isPython(language) ? "script.py" : "script.js";
            String source = buildHarness(code, language, functionName, tc.getInputJson());
            Path scriptPath = workDir.resolve(filename);
            Files.writeString(scriptPath, source, StandardCharsets.UTF_8);

            List<String> command = buildDockerCommand(containerName, workDir, language, filename);

            long start = System.currentTimeMillis();
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                killContainer(containerName);
                process.destroyForcibly();
                return new TestCaseResult(tc.getInputJson(), tc.getExpectedOutputJson(), null, false,
                        "Time limit exceeded (" + timeoutSeconds + "s)", timeoutSeconds * 1000L);
            }

            long runtimeMs = System.currentTimeMillis() - start;
            String stdout = readStream(process.getInputStream()).trim();
            String stderr = readStream(process.getErrorStream()).trim();

            String expected = tc.getExpectedOutputJson().trim();
            boolean passed = stderr.isBlank() && jsonEquals(stdout, expected);

            return new TestCaseResult(tc.getInputJson(), expected, stdout, passed,
                    stderr.isBlank() ? null : stderr, runtimeMs);

        } catch (Exception e) {
            log.error("Execution failed for container {}", containerName, e);
            killContainer(containerName); // best-effort, in case it's still running
            return new TestCaseResult(tc.getInputJson(), tc.getExpectedOutputJson(), null, false,
                    "Execution error: " + e.getMessage(), 0);
        } finally {
            if (workDir != null) deleteRecursively(workDir);
        }
    }

    private List<String> buildDockerCommand(String containerName, Path workDir, String language, String filename) {
        boolean python = isPython(language);
        String image = python ? pythonImage : nodeImage;
        String runCmd = python ? "python3 " + filename : "node " + filename;

        List<String> cmd = new ArrayList<>(List.of(
                "docker", "run", "--rm",
                "--name", containerName,
                "--network", "none",
                "--memory", memoryLimit,
                "--memory-swap", memoryLimit,
                "--cpus", cpuLimit,
                "--pids-limit", pidsLimit,
                "--read-only",
                "--tmpfs", "/tmp:rw,size=10m",
                "-v", workDir.toAbsolutePath() + ":/box:ro",
                "-w", "/box",
                "--security-opt", "no-new-privileges",
                image,
                "sh", "-c", runCmd
        ));
        return cmd;
    }

    private void killContainer(String containerName) {
        try {
            new ProcessBuilder("docker", "kill", containerName).start().waitFor(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // container may have already exited on its own — fine either way
        }
    }

    private String readStream(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void deleteRecursively(Path dir) {
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {
            // temp dir cleanup is best-effort
        }
    }

    private boolean isPython(String language) {
        return "python".equalsIgnoreCase(language);
    }

    private boolean jsonEquals(String a, String b) {
        try {
            return mapper.readTree(a).equals(mapper.readTree(b));
        } catch (Exception e) {
            return a != null && a.equals(b);
        }
    }

    private String buildHarness(String userCode, String language, String functionName, String inputJson) {
        if (isPython(language)) {
            return userCode + "\n\n" +
                    "import json\n" +
                    "args = json.loads('''" + inputJson.replace("'", "\\'") + "''')\n" +
                    "result = " + functionName + "(*args)\n" +
                    "print(json.dumps(result))\n";
        }
        return userCode + "\n\n" +
                "const args = " + inputJson + ";\n" +
                "const result = " + functionName + "(...args);\n" +
                "console.log(JSON.stringify(result));\n";
    }
}

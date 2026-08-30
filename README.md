# LeetAI — LeetCode-style platform with an AI coach

[![CI](https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME/actions/workflows/ci.yml)

## How it works
1. User writes their **approach** (text) and **code** for a problem.
2. On submit, the backend **actually runs the code** in an isolated Docker
   container (one per test case) — this is what decides pass/fail, not the
   AI. No network access, capped memory/CPU/process count, read-only source
   mount, non-root — see `DockerCodeExecutionService`.
3. The backend then sends the problem, the user's approach, their code, and
   the real test results to an LLM (Claude or a local Ollama model), which
   writes a short coaching review: why it passed/failed, feedback on the
   approach, complexity, hints.

This split matters: correctness is ground-truth (sandboxed execution), and
the AI's job is explanation/coaching, not judging — so it can't hallucinate
a wrong verdict.

## Architecture (SOLID)
- `ProblemController` / `SubmissionController` — HTTP layer only.
- `ProblemService` (interface) / `ProblemServiceImpl` — business logic. The
  controller depends on the interface, not the implementation or the
  repository (Dependency Inversion) — swap in a different implementation
  without touching the controller.
- `ProblemMapper` — sole job is entity ↔ DTO conversion, and filtering hidden
  test cases out of anything user-facing (Single Responsibility).
- `SlugGenerator` — sole job is turning a name into a unique slug.
- `ProblemRepository` — persistence only (Spring Data JPA).
- `CodeExecutionService` (interface) / `DockerCodeExecutionService` — sole
  job is running code in a sandboxed container and returning raw results.
  Swappable (e.g. for a queued/distributed executor later) without touching
  `SubmissionController`.
- `AiAssistantService` — talks to the LLM only. Doesn't know about HTTP,
  the database, or how code execution works.

## Run it

### 0. MySQL
```sql
CREATE DATABASE leetai;
```
Set credentials via env vars if not using local root/no-password defaults:
```bash
export DB_URL="jdbc:mysql://localhost:3306/leetai"
export DB_USER=root
export DB_PASSWORD=yourpassword
```

### 1. Docker (code execution sandbox)
No separate service to run — just make sure Docker Desktop (or the Docker
daemon) is installed, running, and the `docker` CLI is on PATH for whatever
shell/user runs the Spring Boot process. Pull the runtime images once ahead
of time so the first submission isn't slow:
```bash
docker pull node:20-alpine
docker pull python:3.11-alpine
```

### 2. Backend
```bash
cd backend
# AI defaults to Ollama — see "AI provider" section below for Claude instead
mvn spring-boot:run
```
Runs on `http://localhost:8080`. Starts with an empty `problems` table — use
the API below to add problems (there's no more auto-seeded data).

### Adding problems programmatically
No admin UI — POST directly:
```bash
curl -X POST http://localhost:8080/api/problems \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Reverse String",
    "description": "Reverse the given string s in place.",
    "difficulty": "EASY",
    "functionName": "reverseString",
    "starterCodeJs": "function reverseString(s) {\n  // your code\n}",
    "starterCodePython": "def reverseString(s):\n    pass",
    "testCases": [
      { "input": "[\"hello\"]", "expectedOutput": "\"olleh\"", "hidden": false },
      { "input": "[\"a\"]", "expectedOutput": "\"a\"", "hidden": true }
    ]
  }'
```
`hidden: true` test cases are used for grading but never returned to the
frontend (filtered in `ProblemMapper`).

### Full CRUD reference
```bash
# List all
curl http://localhost:8080/api/problems

# Get one
curl http://localhost:8080/api/problems/two-sum

# Create (see example above)
curl -X POST http://localhost:8080/api/problems -H "Content-Type: application/json" -d '{...}'

# Update — full replace of fields + test cases, slug in the URL stays fixed
curl -X PUT http://localhost:8080/api/problems/two-sum \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Two Sum",
    "description": "Updated description...",
    "difficulty": "EASY",
    "functionName": "twoSum",
    "starterCodeJs": "function twoSum(nums, target) {\n  // your code\n}",
    "testCases": [
      { "input": "[[2,7,11,15], 9]", "expectedOutput": "[0,1]", "hidden": false }
    ]
  }'

# Delete
curl -X DELETE http://localhost:8080/api/problems/two-sum
```

### Bulk-loading sample problems
`sample-problems/` has ready-to-use JSON for Two Sum, Valid Parentheses,
Reverse String, and Maximum Subarray. Load them all in one go once the
backend is running:
```bash
cd sample-problems
./load-problems.sh              # defaults to http://localhost:8080
```
Or add just one:
```bash
curl -X POST http://localhost:8080/api/problems \
  -H "Content-Type: application/json" \
  -d @sample-problems/two-sum.json
```

### AI provider: Claude or local Ollama
Set `AI_PROVIDER=ollama` (default) to use a local Llama model via Ollama —
just make sure `ollama serve` is running and you've pulled a model:
```bash
ollama pull llama3.1
export AI_PROVIDER=ollama
export OLLAMA_MODEL=llama3.1   # optional, this is the default
```
Or set `AI_PROVIDER=claude` and `ANTHROPIC_API_KEY=sk-ant-...` to use Claude
instead.

### Execution limits (tunable via env vars)
| Env var | Default | Meaning |
|---|---|---|
| `EXEC_TIMEOUT_SECONDS` | `8` | Wall-clock kill switch per submission |
| `EXEC_MEMORY_LIMIT` | `256m` | Hard memory cap per container |
| `EXEC_CPU_LIMIT` | `0.5` | Fraction of a CPU core |
| `EXEC_PIDS_LIMIT` | `64` | Max processes/threads (fork-bomb guard) |
| `EXEC_NODE_IMAGE` | `node:20-alpine` | JS runtime image |
| `EXEC_PYTHON_IMAGE` | `python:3.11-alpine` | Python runtime image |

### Going to production later
This calls the `docker` CLI directly from the Spring process, which means:
- **The host running the backend needs Docker installed and the daemon
  reachable** — works great on a VM you fully control (EC2, DigitalOcean
  droplet, your own server), but will **not** work on platforms that block
  Docker-in-Docker or privileged access (Render, Railway, Vercel, most
  managed PaaS/serverless).
- If you containerize the Spring Boot app itself later, it'll need the host
  Docker socket mounted in (`-v /var/run/docker.sock:/var/run/docker.sock`)
  so it can spawn sibling containers — that's a meaningful trust boundary
  (the app effectively gets root on the host via the socket), worth
  hardening before opening this up publicly.
- At real scale, swap `DockerCodeExecutionService` for a queued version
  (Redis/BullMQ-style) so a burst of submissions doesn't spawn hundreds of
  containers at once — the `CodeExecutionService` interface exists so this
  swap doesn't touch `SubmissionController`.

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`.

## What's here vs. what you'll want to add
- **Here:** problem CRUD (read), Monaco code editor, JS + Python execution,
  AI review pipeline, MySQL persistence, Google OAuth2 login, per-user solved
  tracking with submission history, an admin panel (mark/reset solved status,
  browse all submissions), and a backend JUnit suite (see `CHANGES.md`).
- **Not here yet, straightforward to add:** more languages (add a runtime
  image + harness in `DockerCodeExecutionService.buildHarness`/
  `buildDockerCommand`), a "give me a hint" chat mode (reuse
  `AiAssistantService` with a lighter prompt), rate limiting on the AI call
  itself (submission rate limiting already exists via `RateLimitService`), a
  request queue in front of execution for burst traffic, and a deploy step
  in CI once this is actually hosted somewhere (see the CI section below).

## Notes
- I couldn't compile/run the Java backend in this sandbox (no Maven Central
  access here) — the frontend installs and runs cleanly (verified). Worth a
  `mvn spring-boot:run` on your machine before you build further on top; ping
  me with any compile errors and I'll fix them.
- Keep `ANTHROPIC_API_KEY` (if using Claude) out of source control — it's
  read from env vars in `application.properties`.

## Continuous Integration

`.github/workflows/ci.yml` runs on every push and pull request against
`main`:
- **Backend job** — `mvn test`, the full JUnit suite (see `CHANGES.md` for
  what's covered). Runs against an in-memory H2 database, no live
  MySQL/Elasticsearch/OAuth2 credentials needed. Test reports are uploaded
  as a downloadable artifact on the workflow run page even when tests fail.
- **Frontend job** — `npm ci && npm run build`, catching build breaks the
  same way the earlier rounds of manual `npm run build` did in this
  conversation.

This is CI, not CD — there's no deploy step, since the project isn't hosted
anywhere. If that changes later, a deploy job can be added to the same
workflow file, gated on the existing two jobs passing first.

**Before this badge/workflow does anything:**
1. Push this project to a GitHub repository if it isn't already one —
   `git init && git add . && git commit -m "initial commit"`, create a repo
   on GitHub, then `git remote add origin <url> && git push -u origin main`.
2. Replace `YOUR_GITHUB_USERNAME/YOUR_REPO_NAME` in the badge URL at the top
   of this file with your actual repo path.
3. If your default branch is `master` rather than `main`, update the
   `branches: [ main ]` lines in `.github/workflows/ci.yml` to match — GitHub
   Actions only triggers on the branches you list there.
4. The workflow runs automatically on your very next push after that — check
   the "Actions" tab on the repo page to watch it run.

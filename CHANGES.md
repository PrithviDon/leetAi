# "Mark as Solved" feature — what changed

## Backend (Spring Boot)

- **New `Submission` entity** (`model/Submission.java`) — every call to
  `POST /api/problems/{slug}/submit` now persists a row: code, language,
  approach, pass counts, AI feedback, and a `solved` flag. `solved` starts
  out equal to the autograder's `allPassed` result, but can be changed later
  by an admin.
- **`SubmissionRepository`** — lookups for a user's own history, the set of
  problem slugs a user has solved, and a filterable admin search.
- **`SubmissionController`** (`/api/problems/{slug}`)
  - `POST /submit` — unchanged behavior, but now saves the attempt and
    returns `submissionId` + `solved` in the response.
  - `GET /submissions` *(new)* — the logged-in user's own submission history
    for that problem.
- **`AdminSubmissionController`** (`/api/admin/submissions`) *(new)*
  - `GET /` — browse all submissions, filterable by `?slug=` and `?email=`,
    paginated.
  - `PATCH /{id}/mark-solved` — admin marks a submission (and thus the
    problem, for that user) as solved.
  - `PATCH /{id}/unmark-solved` — reverses it.
- **`ProblemController`** — `GET /api/problems` and `GET /api/problems/{slug}`
  now include a `solved: boolean` field for the logged-in user (false for
  anonymous requests).
- **`SecurityConfig`** — `GET /api/problems/*/submissions` now requires
  authentication (previously fell through to `permitAll`).

## Frontend (React)

- **Home page** — a ✓ next to any problem the logged-in user has solved.
- **Problem page** — a "✓ Solved" pill next to the title once solved, plus a
  collapsible "your submissions" history panel that updates after each
  submit.
- **New admin page** — `/admin/submissions` (linked from the top nav and from
  each row in the admin problem list): browse submissions by problem/email,
  expand to see code/approach/AI feedback, and toggle "Mark solved" /
  "Unmark solved" per submission.

## Note on this build

This sandbox's network only allows PyPI/npm/GitHub-type registries — Maven
Central is blocked — so I couldn't run `mvn package` to compile-check the
backend or produce a runnable jar. The Java was written and manually
reviewed carefully, but you'll want to run `mvn clean package` (or open it
in your IDE) once, locally, before deploying. The **frontend was built
successfully** with `npm run build` in this environment with no errors.

`node_modules`, `target`, `.git`, and `.idea` were stripped out of this zip
to keep it small — run `npm install` in `frontend/` and `mvn clean install`
(or let your IDE resolve it) in `backend/` after unzipping.

---

## Update: progress counter + real admin reset (this round)

Two follow-up requests drove this round of changes:

1. **User dashboard indicator + progress counter.** The ✓ badge on solved
   problems already existed from the first round; this adds an actual
   `X / Y solved` counter with a progress bar at the top of the problem
   list. It's backed by a new `GET /api/problems/progress` endpoint rather
   than computed from the visible page, since the problem list is paginated
   and a client-side count would only reflect whatever page you're on.

2. **Admin reset feature, done properly.** The previous "mark/unmark solved"
   buttons operated on a single `Submission` row. That's a real bug for the
   reset use case: if a user has *multiple* passing submissions for the same
   problem, unmarking just one of them doesn't change whether the problem
   shows as solved for them — `existsByUser_IdAndProblem_IdAndSolvedTrue`
   would still find another solved row. The admin panel now has a "Solved
   by" panel (visible once you filter submissions down to one problem) that
   lists every user who's solved it and lets you **Reset** their status in
   one click — this flips *every* solved submission of theirs for that
   problem back to unsolved via a bulk update, so the reset is actually
   complete. The old per-submission toggle is still there for correcting
   one specific record, now relabeled so it's clear it isn't the same thing.

### New backend endpoints
- `GET /api/problems/progress` — `{ solved, total }` for the logged-in user
  (0 solved for anonymous callers).
- `GET /api/admin/problems/{slug}/solved-users` — everyone who currently has
  the problem marked solved, aggregated per user.
- `POST /api/admin/problems/{slug}/users/{userId}/reset-solved` — resets
  that user's solved status for the problem (all their submissions, not
  just one).
- `POST /api/admin/problems/{slug}/users/{userId}/mark-solved` — marks it
  solved by flipping their most recent submission (fails with 409 if they
  have no submissions for it at all).

### Also fixed while in there
- `Problem.description` / `starterCodeJs` / `starterCodePython` and
  `TestCase.inputJson` / `expectedOutputJson` were still using bare `@Lob`
  with no explicit column size — same landmine as the `Submission` fields
  from the previous round (Hibernate defaults an un-sized `@Lob String` to
  JPA's `length=255`, which maps to MySQL `tinytext`). Added
  `@Column(columnDefinition = "LONGTEXT")` to all of them. **You still need
  to run `ALTER TABLE ... MODIFY COLUMN ... LONGTEXT` by hand on any
  existing database** — `ddl-auto=update` does not reliably widen LOB
  columns on tables that already exist, it only adds missing ones. See the
  SQL block below.
- Removed the explicit `spring.jpa.properties.hibernate.dialect=...` line
  from `application.properties` per Hibernate's own startup warning
  (`HHH90000025`) — Boot auto-detects the correct dialect version from the
  JDBC connection now, which is more reliable than pinning a generic one.

```sql
-- submissions table
ALTER TABLE submissions MODIFY COLUMN ai_feedback LONGTEXT;
ALTER TABLE submissions MODIFY COLUMN code LONGTEXT;
ALTER TABLE submissions MODIFY COLUMN approach LONGTEXT;

-- problems table
ALTER TABLE problems MODIFY COLUMN description LONGTEXT;
ALTER TABLE problems MODIFY COLUMN starter_code_js LONGTEXT;
ALTER TABLE problems MODIFY COLUMN starter_code_python LONGTEXT;

-- test_cases table
ALTER TABLE test_cases MODIFY COLUMN input_json LONGTEXT;
ALTER TABLE test_cases MODIFY COLUMN expected_output_json LONGTEXT;
```

---

## Update: filterable, visually distinct solved status (this round)

Follow-up to the progress counter: a small ✓ character and a "3/10 solved"
number don't actually tell you *which* problems are left, especially once
the list is paginated. Two changes to fix that:

- **Solved/Unsolved filter** — a new dropdown next to the difficulty filter
  on the problem list. Picking "Unsolved" shows you exactly what's left to
  do; "Solved" shows your history. This is done client-side (fetch a larger
  batch matching search/difficulty, then filter+paginate in the browser) —
  the backend has no notion of "solved" at the search-index level since that
  data lives per-user in the submissions table, not in Elasticsearch. Fine
  at this app's scale; would need a real backend filter if the problem count
  ever got into the thousands.
- **Stronger visual treatment** — solved rows now get a colored left border
  and a tinted background, plus a proper "✓ Solved" pill instead of a small
  inline character, so they register while scanning instead of needing to
  be read.

---

## Update: backend JUnit test suite

8 test classes, 40 `@Test` methods, under `backend/src/test/java/com/leetai`.
No frontend tests this round — backend only, as requested.

### What's covered, and why these files specifically
- **`service/ProblemMapperTest.java`**, **`service/SubmissionMapperTest.java`**
  — plain unit tests (no Spring context) for the entity↔DTO mapping logic,
  including `ProblemMapper`'s difficulty-string-parsing fallback behavior
  (case-insensitivity, whitespace, unrecognized values defaulting to MEDIUM)
  and hidden-test-case filtering.
- **`controller/ProblemControllerTest.java`** — Mockito unit tests for the
  exact `resolveSolvedSlugs` logic that had the auth-header bug fixed a few
  rounds back: anonymous vs. authenticated callers, the progress counter,
  and both the Elasticsearch and MySQL-fallback branches of `listProblems`.
- **`controller/SubmissionControllerTest.java`** — grading outcomes (solved
  vs. not), rate-limit bypass for admins, draft-problem rejection.
- **`controller/AdminSubmissionControllerTest.java`**,
  **`controller/AdminProblemProgressControllerTest.java`** — the mark/unmark
  and reset/mark-solved admin actions, including the 404/409 edge cases.
- **`repository/SubmissionRepositoryTest.java`**,
  **`repository/ProblemRepositoryTest.java`** — `@DataJpaTest` slice tests
  against a real (in-memory) database for every hand-written JPQL query:
  `findSolvedSlugsByUserEmail`, the admin `search` filter combos, the
  `findSolvedUsersForProblem` aggregation, and `resetSolvedForUserAndProblem`.
  These are the highest-value tests in the suite — hand-written JPQL is
  exactly the kind of thing that's easy to get subtly wrong, and it *is* the
  entire "solved" feature.

### Regression tests for bugs found earlier in this conversation
- `aiFeedbackLongerThan255CharsIsNotTruncated` and
  `descriptionLongerThan255CharsIsNotTruncated` directly re-create the
  `tinytext`/truncation bug from before (an unsized `@Lob String` defaulting
  to a 255-char column) and assert a 5,000–10,000 char value survives a
  round trip. If that mapping regresses, these fail immediately instead of
  silently corrupting data again.

### A bug this exercise found (and fixed) along the way
Writing the reset test surfaced that `resetSolvedForUserAndProblem` didn't
have `clearAutomatically = true` on its `@Modifying` query. Bulk JPQL
updates bypass Hibernate's persistence context, so without it, an entity
already loaded earlier in the same transaction could still look "solved" in
memory after the DB had already changed it. Not a live bug today (the
caller doesn't re-read Submissions afterward), but cheap to close off now
rather than leave as a footgun for whoever extends that method next.

### Test-only infrastructure
- Added `com.h2database:h2` (test scope) to `pom.xml`.
- Added `backend/src/test/resources/application.properties`, which shadows
  the real one during `mvn test` and points at an in-memory H2 database
  running in **MySQL compatibility mode** (`MODE=MySQL`) — required because
  the `@Lob` fields use `columnDefinition = "LONGTEXT"`, a MySQL-specific
  keyword H2 only understands in that mode. Repository tests use
  `@AutoConfigureTestDatabase(replace = Replace.NONE)` to make sure Spring
  Boot actually uses this config instead of auto-generating its own default
  embedded-DB URL.
- Deliberately **did not** write a full `@SpringBootTest` context-loading
  integration test. This app's Spring context requires live MySQL,
  Elasticsearch, and valid Google OAuth2 client registration properties to
  start — none of which exist in a clean checkout or CI runner without
  extra setup, and getting that wrong would produce tests that fail for
  environment reasons having nothing to do with actual bugs. The unit +
  slice (`@DataJpaTest`) tests above cover the real logic without that
  fragility.

### How to run these tests
See the section at the end of this file.

---

## Update: fixed two real bugs behind "solved filter isn't working"

Reported from screenshots: progress bar said "1/5 solved," but filtering to
"Solved" showed 0 results, and the "All problems" view showed no solved
styling on any row despite the count being right. Two separate, confirmed
bugs:

1. **`listProblems()` and `getProblem()` in `client.js` never sent the auth
   token.** Only `getProgress()` did. So the backend saw those two requests
   as anonymous, returned `solved: false` for every problem regardless of
   actual history, while the progress counter (which did send the token)
   was correct the whole time. This also means the "✓ Solved" pill on the
   individual problem detail page was silently broken since it was added —
   fixed now too, same root cause. **Fix:** both functions now send
   `authHeaders()` like every other authenticated call.

2. **"0 problems found" while rows still rendered.** The list endpoint was
   returning Spring Data's `Page<T>` object directly from the controller,
   and its JSON shape isn't something we control — pagination metadata
   fields have moved around between Spring Data versions, which silently
   broke the frontend's `totalElements` read even though `content` itself
   was fine. **Fix:** added `PagedResponse<T>`, a small explicit DTO with
   fixed field names (`content`, `page`, `size`, `totalElements`,
   `totalPages`), and `GET /api/problems` now always returns that shape —
   for both the Elasticsearch path and the MySQL fallback, which used to
   return a bare array and required the frontend to branch on response
   shape. That branching is gone from `Home.jsx` now that both backend paths
   agree on one format.


---

## How to run the backend tests

```bash
cd backend

# Run the entire suite
mvn test

# Run one class
mvn test -Dtest=SubmissionRepositoryTest

# Run one method
mvn test -Dtest=SubmissionRepositoryTest#resetSolvedFlipsOnlyMatchingRows

# Run everything in a package
mvn test -Dtest="com.leetai.repository.*"
```

What to expect in the output:
- A summary per class, e.g. `Tests run: 8, Failures: 0, Errors: 0, Skipped: 1`
  — the "Skipped: 1" is expected in `ProblemControllerTest` and
  `ProblemRepositoryTest`, from the two intentionally-`@Disabled` tests
  (see their reasons in the source — not something broken).
- `@DisplayName` text shows up instead of method names if you run with
  `mvn test` verbosely, or automatically in any IDE's test runner (IntelliJ,
  VS Code with the Java extension, Eclipse) — just right-click a test class
  or method and "Run".
- Full HTML/text reports land in `backend/target/surefire-reports/` after
  the run, one file per test class, if you want the raw output later.
- A failed build (`mvn test`) exits non-zero and stops `mvn package`/
  `mvn install` from producing a jar — this is normal Maven behavior, not
  specific to this project. Use `mvn test -DskipTests=false` is the default;
  `mvn package -DskipTests` skips tests entirely if you need to build
  despite a known-failing test.

No Docker, no live MySQL, no live Elasticsearch, and no Google OAuth2
credentials are needed to run any of these — everything runs against an
in-memory H2 database or plain Mockito mocks.

---

## Update: CI pipeline (test setup fix + GitHub Actions)

### Test setup fix
The two `@DataJpaTest` classes had `@AutoConfigureTestDatabase(replace =
Replace.NONE)` removed. Since `@DataJpaTest` defaults that to `Replace.ANY`,
removing the per-class override meant Spring Boot would swap in its own
auto-generated (default-mode) H2 instance and silently ignore the
`MODE=MySQL` datasource URL in `application.properties` — which would break
schema creation for every `@Lob` field using `columnDefinition = "LONGTEXT"`
(a MySQL-specific keyword H2's default mode doesn't recognize). Fixed by
setting `spring.test.database.replace=NONE` once, centrally, in
`backend/src/test/resources/application.properties` — same effect, without
needing the annotation on every test class.

### CI pipeline
Added `.github/workflows/ci.yml` — **CI only, no CD**, since the project
isn't hosted anywhere yet:
- **Backend job:** `mvn test` — the full JUnit suite, against in-memory H2,
  no live infra or secrets needed. Test reports upload as a downloadable
  artifact on the run page even when tests fail.
- **Frontend job:** `npm ci && npm run build`.

Both run on every push and PR against `main`. Added a status badge and a
"Continuous Integration" section to `README.md` with setup steps (push to
GitHub, swap the badge placeholder for your actual repo path, adjust
`branches: [ main ]` if your default branch is `master`).

Also corrected a stale part of `README.md`'s "what's not here yet" list —
it still said user auth and submission history weren't implemented, when
both were built over the course of this conversation.

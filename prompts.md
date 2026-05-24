# AI Usage Documentation

This document records how AI tools were used during development of the IssueFlow Java backend (`issueflow-java`). It reflects the actual workflow: incremental features, manual review, automated tests before each commit, and separate Git commits per completed layer.

**Primary references:** `issueflow-java/README.md` (REST API contract), `TDP_issueflow_requirements.pdf` (business rules).  
**Scope:** Java module only; the TypeScript starter was not implemented.

---

## Tools and Models Used

| Tool | Role |
|------|------|
| **Cursor AI** (Agent mode inside the IDE) | Code generation, file edits, running Maven tests, implementation guidance aligned with the README and requirements PDF |
| **ChatGPT / GPT-5.5 Thinking** | Planning support, architecture explanations, debugging concepts (e.g. JWT and password hashing), and guidance on assignment documentation |

Cursor handled most in-editor implementation work. ChatGPT was used separately when I needed clearer explanations or to think through design and troubleshooting outside the IDE.

---

## Development Workflow

The project was built incrementally, not as a single bulk generation:

1. **Analyze requirements** — Read the skeleton, README API tables, and requirements PDF; agree on feature order before coding.
2. **Implement one layer at a time** — Each step covered a focused area (e.g. entities, users API, JWT, tickets).
3. **Compile and test after each layer** — Run `.\mvnw.cmd clean test` from `issueflow-java` before committing.
4. **Review AI output** — Inspect generated services, controllers, tests, and config; reject or correct changes that did not match the contract (e.g. test port, enum values).
5. **Commit separately** — One Git commit per completed feature; push before starting the next layer.

Standing constraints I gave to Cursor at the start of the project:

- Keep business logic in **services**, not controllers.
- Follow README paths and shapes exactly.
- Do not implement unrelated features in the same step.
- After each step, report files changed, rationale, verification command, and suggested commit message.

I performed all Git commits and pushes myself.

---

## Prompt Log by Development Stage

### Stage 1: Initial requirements analysis and implementation plan

**Goal:** Understand the skeleton, define a commit order, and scope the Java implementation.

**Prompt summary (Cursor):** Requested a plan-only review of the skeleton and README without code changes, with an ordered list of implementation commits. Later asked to include the requirements PDF and confirm Java-only scope.

**AI assistance:** Cursor inspected the project structure, README endpoints, and PDF rules; proposed a layered commit plan (infrastructure → domain → APIs → advanced features → documentation).

**Developer review:** Confirmed Java-only scope, validated the commit order against the PDF, and decided to follow one feature per commit for a clean Git history.

**Verification:** Read-only inspection; no build yet.

**Commit:** No commit (planning phase).

**ChatGPT (supplementary):** Used GPT-5.5 Thinking to clarify overall Spring Boot layering and how the assignment features map to typical service/controller structure.

---

### Stage 2: Shared infrastructure and configuration

**Goal:** Remove placeholder SQL, add shared exceptions and error handling, prepare test and runtime configuration.

**Prompt summary (Cursor):** Proceed with the first infrastructure commit: clean placeholder schema/data, fix JPA/SQL init settings, add global exception handling and audit stubs, enable scheduling for a later feature.

**AI assistance:** Generated exception types, `GlobalExceptionHandler`, `ErrorResponse`, audit enum stubs, no-op `AuditService`, H2 test profile, and `@EnableScheduling` on the main application class.

**Developer review:** Rejected an unwanted change to the test server port (`8080` must stay). Confirmed `sql.init.mode: never` and that main `application.yaml` still targets PostgreSQL via `compose.yml`.

**Verification:** `cd issueflow-java; .\mvnw.cmd clean test`

**Commit:** `chore: clean placeholder SQL and add shared infrastructure`

---

### Stage 3: Domain entities, enums, and repository layer

**Goal:** Define the full JPA model and Spring Data repositories required by later features.

**Prompt summary (Cursor):** Implement domain entities and repositories as the next commit in the plan.

**AI assistance:** Created enums (`Role`, `TicketStatus`, `TicketPriority`, `TicketType`), entities (`User`, `Project`, `Ticket`, `Comment`, etc.), and repository interfaces with query methods for soft delete, workload, mentions, dependencies, and audit.

**Developer review:** Verified `TicketType` uses `BUG`, `FEATURE`, `TECHNICAL` per README/PDF (not `TASK` from some reference samples). Checked status transition logic on `TicketStatus` and `@Version` fields where required.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: add domain entities and repositories`

---

### Stage 4: PostgreSQL and local runtime configuration

**Goal:** Run the application against PostgreSQL for manual testing while keeping automated tests on H2.

**Prompt summary (Cursor / self):** Use the provided `compose.yml`; align datasource settings in `application.yaml`.

**AI assistance:** Cursor documented Docker Compose usage in later `run.md`; runtime config was largely from the skeleton (`jdbc:postgresql://localhost:5432/issueflow`, user/password `issueflow`).

**Developer review:** Started Postgres with `docker compose -f compose.yml up -d`. Verified connectivity via pgAdmin. Resolved Maven wrapper path issues by running commands from `issueflow-java`, not the repo root.

**Verification:** Manual DB check in pgAdmin; `.\mvnw.cmd spring-boot:run` with Docker running.

**Commit:** Included in infrastructure and subsequent feature commits (no separate commit).

**ChatGPT (supplementary):** Clarified difference between H2 for tests vs PostgreSQL for local development.

---

### Stage 5: User management

**Goal:** Implement the Users API per README (`POST/GET/PATCH /users`, etc.).

**Prompt summary (Cursor):** Implement the users API as the next planned commit.

**AI assistance:** Generated `UserService`, `UserController`, DTOs, mappers, and service/controller tests.

**Developer review:** Checked validation, role handling (`ADMIN` / `DEVELOPER`), and password encoding expectations for later JWT work.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: implement users API`

---

### Stage 6: JWT authentication and authorization

**Goal:** Secure all endpoints except login; support roles, JWT filter, and auth endpoints.

**Prompt summary (Cursor):** Implement JWT authentication and secure endpoints as commit 3 in the plan.

**AI assistance:** Generated `SecurityConfig`, `JwtService`, `AuthService`, JWT filter, auth controller, and `SecurityIntegrationTest`.

**Developer review:** Manually tested login with PowerShell/`Invoke-RestMethod`. Discovered existing DB users had bcrypt hashes that could not be used with ad-hoc SQL passwords. Verified protected vs public routes and ADMIN-only behavior. Used pgAdmin to inspect stored users before retrying authentication.

**Verification:** `.\mvnw.cmd clean test`; manual `POST /auth/login` and authenticated `GET /auth/me`

**Commit:** `feat: add JWT authentication and secure endpoints`

**ChatGPT (supplementary):** Used GPT-5.5 Thinking to understand JWT flow, bcrypt password storage, and why login failed when the stored hash did not match the plaintext password used in manual tests.

---

### Stage 7: Project management

**Goal:** CRUD for projects with ownership and README-compliant responses.

**Prompt summary (Cursor):** Implement the projects API as the next commit.

**AI assistance:** Generated `ProjectService`, `ProjectController`, DTOs, mappers, and tests.

**Developer review:** Confirmed owner linkage and response fields match README.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: implement projects API`

---

### Stage 8: Project soft delete and restore

**Goal:** Soft-delete projects; ADMIN-only list deleted and restore.

**Prompt summary (Cursor):** Add project soft delete and admin restore endpoints.

**AI assistance:** Extended `ProjectService` and controller with `deletedAt` filtering and ADMIN authorization on restore/deleted-list paths.

**Developer review:** Verified non-admin users cannot access admin-only deleted endpoints (covered in security tests).

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: add project soft delete and admin restore`

---

### Stage 9: Workload and auto-assignment

**Goal:** Expose per-developer ticket counts and implement least-loaded developer auto-assignment for new tickets.

**Prompt summary (Cursor):** Add the project workload endpoint; ticket auto-assignment was part of the tickets core commit.

**AI assistance:** Generated workload aggregation in `ProjectService` and `GET /projects/{id}/workload`; auto-assign logic in ticket creation within `TicketService`.

**Developer review:** Checked counting rules (active tickets per assignee) and tie-breaking behavior.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: add project workload endpoint` (workload); auto-assignment verified in tickets core commit below.

---

### Stage 10: Ticket management and validation rules

**Goal:** Ticket CRUD, forward-only status transitions, no edits when `DONE`, overdue flag, auto-assign on create.

**Prompt summary (Cursor):** Implement the tickets API core.

**AI assistance:** Generated `TicketService`, controller, DTOs, status transition checks, auto-assign integration, and tests.

**Developer review:** Validated status machine (`TODO → IN_PROGRESS → IN_REVIEW → DONE`), business rule exceptions, and README field names.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: implement tickets API core`

---

### Stage 11: Optimistic locking

**Goal:** Prevent lost updates on concurrent ticket (and related) modifications per PDF.

**Prompt summary (Cursor):** Included as part of tickets core implementation per requirements.

**AI assistance:** Added `@Version` on entities and conflict handling via `OptimisticLockConflictException` / global handler.

**Developer review:** Confirmed conflict responses are returned instead of silent overwrites.

**Verification:** Covered in `TicketServiceTest` and full `.\mvnw.cmd clean test`

**Commit:** `feat: implement tickets API core`

**ChatGPT (supplementary):** Used GPT-5.5 Thinking to explain how JPA optimistic locking works and when `@Version` increments.

---

### Stage 12: Ticket soft delete and restore

**Goal:** Soft-delete tickets; ADMIN deleted list and restore.

**Prompt summary (Cursor):** Add ticket soft delete and admin restore.

**AI assistance:** Extended ticket service and controller with delete/restore/deleted-list behavior and tests.

**Developer review:** Verified deleted tickets are excluded from normal lists; ADMIN paths require correct role.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: add ticket soft delete and admin restore`

---

### Stage 13: CSV export and import

**Goal:** Export tickets to CSV and import tickets from CSV with validation.

**Prompt summary (Cursor):** Implement CSV export (commit 9), then CSV import together with comments API (commits 10–11 batch).

**AI assistance:** Generated `TicketCsvService`, export/import endpoints, parsing/validation logic, and tests.

**Developer review:** Checked CSV columns match ticket fields and import errors are handled cleanly.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: add ticket CSV export`; `feat: add ticket CSV import and comments API`

---

### Stage 14: Comment management

**Goal:** CRUD comments on tickets per README.

**Prompt summary (Cursor):** Implement comments API (batched with CSV import in one development step).

**AI assistance:** Generated `CommentService`, `CommentController`, DTOs, and tests.

**Developer review:** Verified ticket association and update/delete rules.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: add ticket CSV import and comments API`

---

### Stage 15: Comment mentions

**Goal:** Parse `@username` mentions in comment bodies and expose a mentions feed per user.

**Prompt summary (Cursor):** Implement mention parsing with comments; add dedicated mentions API in the following batch (commits 12–13).

**AI assistance:** Generated `CommentMentionService`, `MentionService`, mention repositories/queries, `GET /users/{id}/mentions`, and tests.

**Developer review:** Checked case handling for usernames and that mentions link to the correct comment/ticket context.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: add ticket CSV import and comments API`; `feat: add mentions API and audit log persistence`

---

### Stage 16: Ticket dependencies

**Goal:** Add/remove dependencies between tickets with cycle prevention.

**Prompt summary (Cursor):** Implement ticket dependencies and attachments APIs together (commits 14–15 batch).

**AI assistance:** Generated `TicketDependencyService`, dependency controller, cycle detection, and tests.

**Developer review:** Verified blocker relationships and rejection of circular dependencies.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: add ticket dependencies and attachments APIs`

---

### Stage 17: Attachment management

**Goal:** Upload, list, download, and delete ticket attachments with disk storage and size limits.

**Prompt summary (Cursor):** Implement attachments API in the same batch as dependencies.

**AI assistance:** Generated `AttachmentService`, controller, storage under `uploads/`, 10MB multipart limits, and tests with temporary upload directories.

**Developer review:** Confirmed `uploads/` is gitignored and test profile uses a temp directory.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: add ticket dependencies and attachments APIs`

---

### Stage 18: Audit logs

**Goal:** Persist audit entries and expose queryable audit log API; wire `AuditService` to the database.

**Prompt summary (Cursor):** Add mentions API and audit log persistence together (commits 12–13 batch).

**AI assistance:** Generated `AuditLogService`, audit controller, persisted `AuditService.log`, and tests.

**Developer review:** Verified audit actions (create, update, delete, escalate, etc.) are written on key operations.

**Verification:** `.\mvnw.cmd clean test`

**Commit:** `feat: add mentions API and audit log persistence`

---

### Stage 19: Scheduled escalation

**Goal:** Background job to mark overdue tickets and escalate priority with system audit entries.

**Prompt summary (Cursor):** Implement overdue ticket auto-escalation scheduler, plus final documentation files, with full test verification.

**AI assistance:** Generated `TicketEscalationService` with `@Scheduled`, priority escalation rules, `ESCALATE` audit with `AuditActor.SYSTEM`, config properties, and unit tests.

**Developer review:** Confirmed scheduler delay is configurable; tests use a very long delay so the job does not interfere with other tests. Verified priority steps: LOW → MEDIUM → HIGH → CRITICAL.

**Verification:** `.\mvnw.cmd clean test` (113 tests passing)

**Commit:** `feat: add overdue ticket auto-escalation scheduler`

---

### Stage 20: Testing strategy

**Goal:** Maintain automated coverage throughout development; use manual checks for auth and DB.

**Prompt summary (Cursor):** Run full test suite after each commit; fix failures before proceeding.

**AI assistance:** Generated and updated JUnit tests: `@SpringBootTest` service tests, `@WebMvcTest` controller tests with security test base, and `SecurityIntegrationTest` for JWT flows.

**Developer review:** Insisted on fixed port 8080 in test config. Ran tests locally after every layer. Used pgAdmin and PowerShell for manual auth smoke tests when automated tests were not enough.

**Verification:** `.\mvnw.cmd clean test` after each commit; optional `.\mvnw.cmd spring-boot:run` with Docker Postgres

**Commit:** Tests included in each feature commit (no separate test-only commit).

---

### Stage 21: `run.md` and final documentation

**Goal:** Document setup, build, run, test, and smoke-test steps for graders and local reproduction.

**Prompt summary (Cursor):** Produce `run.md` at repo root and finalize AI usage documentation (`prompts.md`).

**AI assistance:** Generated `run.md` with prerequisites, Docker Compose, Maven commands, configuration table, JWT smoke examples, and troubleshooting.

**Developer review:** Rewrote this `prompts.md` for submission to accurately reflect tools used, real workflow, and accountability. Reviewed `run.md` for Windows/PowerShell paths and correctness.

**Verification:** Followed steps in `run.md` where applicable; `.\mvnw.cmd clean test`

**Commit:** `docs: add run.md with setup and run instructions`; `docs: add prompts.md documenting AI-assisted development`

**ChatGPT (supplementary):** Used GPT-5.5 Thinking for guidance on what to include in run/documentation sections for a professional submission.

---

## Accountability Statement

All AI-generated suggestions were reviewed, adapted where necessary, compiled, tested, and committed by me. I understand the implementation—including services, security, validation rules, tests, and configuration—and am responsible for the submitted code. AI was used as an assistant to accelerate implementation and clarify concepts; it did not replace my judgment, verification, or ownership of the final solution.

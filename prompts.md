# AI-Assisted Development — Prompts and Workflow

This file documents how **Cursor (Auto agent)** was used to build the IssueFlow Java backend incrementally, with one feature per Git commit. It satisfies the homework requirement to record main prompts and agent interaction patterns.

**Tooling:** Cursor IDE, Agent mode, conversation in project workspace `TDP2026HW`.  
**Contracts:** `issueflow-java/README.md` (exact REST paths), `TDP_issueflow_requirements.pdf` (business rules).  
**Reference:** Partial solution under `Downloads/tdp2026-homework/TDP2026HW/issueflow-java` used for parity checks, not copied wholesale.

---

## 1. Standing instructions (every session)

These rules were given at the start and repeated when needed:

```
I am restarting this assignment from a clean skeleton and want a professional Git history.

Important workflow:
- Implement one layer or feature at a time.
- After each layer, I will compile, test, commit, and push.
- Do not implement multiple unrelated features at once.
- Do not rewrite the whole project unless necessary.
- Keep business logic in services, not controllers.
- Follow README.md API paths exactly.
- Use the requirements document as the feature contract.
- After every implementation step, tell me:
  1. which files changed
  2. why they changed
  3. what command to run to verify
  4. suggested commit message
```

**Why this mattered:** It kept diffs reviewable, matched course grading expectations, and forced verification (`mvnw clean test`) before each commit.

---

## 2. Phase 0 — Plan only (no code)

**Prompt:**

```
First, inspect the skeleton and README.md only. Do not edit code yet.
Give me a clean implementation plan and recommended commit order.
```

**Agent behavior:** Read skeleton, README, and (once provided) `TDP_issueflow_requirements.pdf`; produced a **19-commit roadmap** (infrastructure → domain → APIs → advanced features → docs).

**Outcome:** Commits 0–18 order agreed before implementation.

---

## 3. Per-commit implementation prompts

Each feature was started with a short directive. The agent then read the reference homework tree, matched existing conventions, implemented, and ran tests.

| Commit | User prompt (representative) | Agent focus |
|--------|------------------------------|-------------|
| 0 | `lets start with commit 0` | Clean SQL placeholders, shared exceptions, `GlobalExceptionHandler`, audit enums stub, `@EnableScheduling`, H2 test config |
| 1 | `lets do commit 1` | JPA entities + repositories |
| 2 | `start commit 2` | Users API (`UserService`, `UserController`, DTOs, tests) |
| 3 | (JWT / security commit) | `SecurityConfig`, `JwtService`, `AuthService`, filter, integration tests |
| 4 | Projects CRUD | `ProjectService`, controller, mappers |
| 5 | Project soft delete + restore | ADMIN-only deleted/restore |
| 6 | Workload endpoint | `GET /projects/{id}/workload` |
| 7 | Tickets core | Status machine, `@Version`, auto-assign, overdue flag |
| 8 | Ticket soft delete + restore | DELETE, deleted list, restore |
| 9 | CSV export | `TicketCsvService`, export endpoint |
| 10 | CSV import | Import validation and persistence |
| 11 | Comments + @mentions parsing | `CommentService`, `CommentMentionService` |
| 12 | Mentions API | `GET /users/{id}/mentions` |
| 13 | Audit logs persisted | `AuditLogService`, `GET /audit-logs`, `AuditService` writes DB |
| 14 | Ticket dependencies | Blockers, cycle detection |
| 15 | Attachments | Disk storage under `uploads/`, 10MB limit |
| 16–18 | `okay perfect lets do 16 to 18 all together` | Escalation scheduler, `run.md`, this `prompts.md` |

---

## 4. Representative mid-project prompts

### Security / test port

When tests failed due to random ports or security setup:

- Agent aligned test `application.yaml` to **fixed port 8080** per user preference.
- Controller tests used `@WebMvcTest` + `SecuredControllerTestBase` with mocked `SecurityUtils` (user id `1`).
- Full JWT flows covered in `SecurityIntegrationTest` with `admin` / `dev` users and `TestPasswords`.

### Enum naming fix

Reference code used `TicketType.TASK`; README/PDF use **BUG / FEATURE / TECHNICAL**. Prompts did not always mention this; the agent corrected tests to `TicketType.TECHNICAL` when compilation failed.

### Commit discipline

**User:** `yeah i did that and it worked, so now i should commit it to github? before continuing right?`  
**Agent:** Confirmed verify → commit → push → next feature; provided exact `git add` / `git commit` / `git push` commands without committing on the user's behalf unless asked.

---

## 5. Commit 16–18 batch prompt

```
okay perfect lets do 16 to 18 all together, be as detailed as possible and make sure everything is okay
```

**Requested scope:**

1. **Commit 16 — Auto-escalation:** `TicketEscalationService` with `@Scheduled`, overdue query, priority bump, `AuditAction.ESCALATE` with `AuditActor.SYSTEM`, unit tests.
2. **Commit 17 — `run.md`:** Prerequisites, Docker Postgres, build, run, test, JWT smoke, troubleshooting.
3. **Commit 18 — `prompts.md`:** This file.

---

## 6. How the agent used the codebase

Typical loop for each commit:

1. **Read** reference implementation file(s) from homework download path.
2. **Read** existing project files for naming and patterns.
3. **Implement** minimal diff (services + thin controllers + DTOs + tests).
4. **Run** `.\mvnw.cmd clean test` from `issueflow-java`.
5. **Report** four-point summary (files, why, verify command, commit message).

**Deliberate choices:**

- Business logic in **services**; controllers delegate only.
- **Audit** centralized in `AuditService` (persisted after commit 13).
- **Soft delete** via `deletedAt`; list endpoints exclude deleted unless ADMIN deleted views.
- **Tests:** `@SpringBootTest` + `@Transactional` for services; `@WebMvcTest` for controllers; H2 for integration tests so CI/local test does not require Docker.

---

## 7. What was not automated by AI

- Git **commits and pushes** (user performed manually unless explicitly requested).
- Production secrets (JWT secret left as dev default in YAML with comment to change).
- `issueflow-typescript` module (out of scope for Java track).

---

## 8. Verification commands (used throughout)

```powershell
cd C:\Users\user\Desktop\IssueflowProject\TDP2026HW\issueflow-java
.\mvnw.cmd clean test
```

```powershell
docker compose -f compose.yml up -d
.\mvnw.cmd spring-boot:run
```

See **`run.md`** for full setup and smoke-test steps.

---

## 9. Suggested commit messages (16–18)

| Commit | Message |
|--------|---------|
| 16 | `feat: add overdue ticket auto-escalation scheduler` |
| 17 | `docs: add run.md with setup and run instructions` |
| 18 | `docs: add prompts.md documenting AI-assisted development` |

You may squash 17–18 into one docs commit if your course allows; keeping three commits preserves the original plan.

---

## 10. Files related to AI workflow

| Artifact | Role |
|----------|------|
| `prompts.md` | This log of prompts and process |
| `run.md` | Human runbook for graders/reviewers |
| Cursor user rules | Commit only when asked; PowerShell-friendly commands |
| Conversation transcript | Full tool history in Cursor agent transcript (not committed) |

No custom Cursor **skills** or `.cursor/rules` were added to the repo; development relied on default Agent mode plus the standing workflow prompt in section 1.

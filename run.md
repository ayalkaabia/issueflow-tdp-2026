# IssueFlow — Setup, Build, Run, and Test

This document describes how to run the **Java** IssueFlow backend (`issueflow-java`) on a local machine. Paths assume the repository root is `TDP2026HW`.

---

## Prerequisites

| Tool | Version / notes |
|------|-----------------|
| **Java JDK** | **21** (matches `pom.xml`; Java 25 may work with Spring Boot 3.4 but 21 is the tested target) |
| **Docker Desktop** (or Docker Engine + Compose) | Required for PostgreSQL via `compose.yml` |
| **Git** | To clone the repository |
| **PowerShell or bash** | Commands below use PowerShell on Windows; bash equivalents are noted where different |

Verify Java:

```powershell
java -version
```

You should see version **21**.

---

## 1. Clone and enter the Java module

```powershell
git clone <your-repo-url> TDP2026HW
cd TDP2026HW\issueflow-java
```

All Maven commands below are run from **`issueflow-java`**.

---

## 2. Start PostgreSQL (Docker Compose)

The app expects PostgreSQL on `localhost:5432` with database/user/password **`issueflow`** (see `src/main/resources/application.yaml`).

From `issueflow-java`:

```powershell
docker compose -f compose.yml up -d
```

Check the container is running:

```powershell
docker compose -f compose.yml ps
```

Stop the database when finished:

```powershell
docker compose -f compose.yml down
```

**Port conflict:** If port `5432` is already in use, stop the other PostgreSQL instance or change the published port in `compose.yml` and update `spring.datasource.url` accordingly.

---

## 3. Configuration (optional overrides)

Default settings live in `issueflow-java/src/main/resources/application.yaml`.

| Property | Default | Purpose |
|----------|---------|---------|
| `server.port` | `8080` | HTTP port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/issueflow` | Database |
| `spring.datasource.username` / `password` | `issueflow` / `issueflow` | DB credentials |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema managed by JPA |
| `issueflow.jwt.secret` | Dev secret (32+ chars) | JWT signing — **change in production** |
| `issueflow.jwt.expiration-seconds` | `3600` | Token lifetime |
| `issueflow.attachments.upload-dir` | `uploads` | On-disk attachment storage (created automatically) |
| `issueflow.escalation.fixed-delay-ms` | `60000` | How often the overdue-ticket scheduler runs (ms) |

Override without editing tracked files (examples):

```powershell
$env:ISSUEFLOW_JWT_SECRET = "your-production-secret-at-least-32-characters"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/issueflow"
.\mvnw.cmd spring-boot:run
```

Spring Boot maps environment variables: `issueflow.jwt.secret` → `ISSUEFLOW_JWT_SECRET`, etc.

For local-only YAML, you can add `application-local.yml` (gitignored) and run with:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

---

## 4. Build the project

```powershell
cd issueflow-java
.\mvnw.cmd clean package
```

This compiles, runs all tests, and produces `target/issueflow-0.0.1-SNAPSHOT.jar`.

Skip tests during packaging only if you intentionally want a faster build (not recommended before submit):

```powershell
.\mvnw.cmd clean package -DskipTests
```

---

## 5. Run the application

**Option A — Maven (development):**

```powershell
.\mvnw.cmd spring-boot:run
```

**Option B — JAR:**

```powershell
java -jar target\issueflow-0.0.1-SNAPSHOT.jar
```

The API listens on **http://localhost:8080**.

On first boot with an empty database, JPA creates tables. Create users via `POST /users` or seed data as needed for your environment.

---

## 6. Run tests

Tests use an **in-memory H2** database (see `src/test/resources/application.yaml`). **PostgreSQL does not need to be running** for `mvn test`.

```powershell
cd issueflow-java
.\mvnw.cmd clean test
```

The escalation scheduler is configured with a very long delay in tests so it does not interfere with other cases.

---

## 7. Smoke test the API (JWT)

Only **`POST /auth/login`** is public. All other endpoints require `Authorization: Bearer <token>`.

### 7.1 Create users (if database is empty)

```powershell
$body = @{
  username = "admin"
  email = "admin@example.com"
  fullName = "Admin User"
  role = "ADMIN"
  password = "password123"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/users" `
  -ContentType "application/json" -Body $body
```

Repeat for a `DEVELOPER` user if desired.  
*(If security is already enabled and `/users` is protected, bootstrap the first admin through your chosen seed process or temporarily adjust security for local dev.)*

### 7.2 Login

```powershell
$login = @{
  username = "admin"
  password = "password123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/auth/login" `
  -ContentType "application/json" -Body $login

$token = $response.token
```

### 7.3 Call a protected endpoint

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/auth/me" `
  -Headers @{ Authorization = "Bearer $token" }
```

### 7.4 curl (Git Bash / WSL)

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'
```

---

## 8. Feature notes for local runs

- **Attachments:** Files are stored under `issueflow-java/uploads/` (gitignored). Max upload size is **10 MB** per file.
- **CSV import/export:** Authenticated endpoints under `/tickets/export` and `/tickets/import`.
- **Soft delete / restore:** `GET .../deleted` and `POST .../restore` require **ADMIN**.
- **Auto-escalation:** A background job runs every `issueflow.escalation.fixed-delay-ms` (default 60s). Overdue non-`DONE` tickets are marked `overdue` and priority is raised `LOW → MEDIUM → HIGH → CRITICAL` with an `ESCALATE` audit log (`SYSTEM` actor).

---

## 9. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|----------------|-----|
| Connection refused to `localhost:5432` | Postgres not running | `docker compose -f compose.yml up -d` |
| `FATAL: password authentication failed` | Wrong credentials | Match `application.yaml` and `compose.yml` |
| Port 8080 in use | Another process on 8080 | Stop it or set `server.port` |
| Tests fail with port bind error | Another app on 8080 during tests | Stop conflicting process (tests use port 8080 in test profile) |
| JWT 401 on all routes | Missing/invalid token | Login again; check `Authorization: Bearer` header |
| Upload fails | File > 10MB | Reduce file size or adjust multipart limits |

---

## 10. Project layout (Java)

```
TDP2026HW/
  run.md                 ← this file
  prompts.md             ← AI-assisted development log
  issueflow-java/
    compose.yml          ← PostgreSQL for local dev
    mvnw / mvnw.cmd      ← Maven wrapper
    src/main/java/       ← Application code
    src/test/java/       ← Tests (H2)
    src/main/resources/application.yaml
```

For full API paths and request/response shapes, see **`issueflow-java/README.md`** and the course requirements PDF (`TDP_issueflow_requirements.pdf` at repo root when provided).

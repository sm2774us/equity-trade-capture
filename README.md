# Trading Platform — Real-Time Equity & Autocallable Trade Capture

A production-shaped showcase of a **real-time trade capture, streaming
normalization, and P&L/risk platform** for equities and autocallable
structured notes, built for a **Senior Full Stack Java Equity Trading
Engineer** role at an alternative investment / multi-manager platform.

It exists to demonstrate, in running code rather than slides: Java 17
service design, Kafka-based streaming architecture, low-latency/thread-safe
position accounting, a modern Angular front end, and an enforced CI/CD
pipeline — the exact skill set called out in the target job description
(trade capture, Kafka pipelines, Angular, AI-assisted dev workflows,
Jenkins/GitHub Actions CI/CD, Postgres, multithreaded/distributed design).

---

## 1. Why this project, and why it matters to the role

Alternative investment platforms that run multiple autonomous trading pods
(a "multi-manager" model) share one hard problem: every pod's executions —
across equities, options, futures, and structured products like
autocallable notes — must be captured, normalized into one canonical shape,
and fanned out to P&L, risk, and reporting **in real time**, without ever
losing or double-counting a fill. That is precisely the "real-time trade
capture platform... feeding downstream systems including real-time P&L,
risk, and reporting" described in the job spec, with particular emphasis on
autocallables and integration with systems like Murex/ION.

This repository implements that pipeline end-to-end:

1. **`trade-capture-service`** — ingests raw executions (REST today; the
   same `TradeCaptureOrchestrator` seam is where a Murex/ION/FIX adapter
   would plug in), validates them against business rules (including
   autocallable-specific term checks — barrier level, maturity, coupon),
   persists an immutable audit-of-record row in Postgres, and publishes a
   normalized event to Kafka.
2. **`pnl-risk-consumer`** — a downstream streaming consumer that maintains
   a thread-safe, weighted-average-cost position book per book/instrument
   and computes realized/unrealized P&L on every fill — the same mechanics
   a real risk engine uses, simplified to an in-memory book for this
   showcase (documented scale-out path to Redis/Ignite below).
3. **`frontend`** — an Angular trading desk UI: a live trade blotter, a
   positions/P&L dashboard, and a manual trade-entry form for the ops
   fallback path.

### Relevance to the industry and the role

- **Kafka streaming pipelines**: topics are provisioned declaratively,
  producers use `acks=all` + idempotence (exactly-once per partition) since
  this event feeds P&L directly — silently dropping or duplicating a fill
  here is a real financial error, not just a UI bug.
- **Autocallable buildout**: `TradeEvent.AutocallableTerms` and the
  validation rules in `TradeValidationService` model the product terms
  (barrier, coupon, observation dates, maturity) the JD explicitly calls
  out as the desk's current focus.
- **Low-latency, thread-safe, multithreaded design**: `Position.applyFill`
  is `synchronized` and uses `BigDecimal` weighted-average-cost accounting
  — correct under concurrent fills on the same book, which is the realistic
  failure mode in a Kafka consumer processing multiple partitions.
- **Fault tolerance**: Kafka publish failures retry with exponential
  backoff (`@Retryable`); the trade audit row is persisted to Postgres
  *before* publish, so the durable record never depends on Kafka
  availability; consumer offsets are manually committed after successful
  processing so a crash mid-processing replays rather than silently drops
  a fill.
- **Observability**: Spring Boot Actuator + Micrometer/Prometheus on both
  services (`/actuator/prometheus`), structured logging, and `@Timed`
  method-level latency metrics — the "logging, metrics, alerting" the JD
  asks DevOps/Platform partnership to support.
- **AI-assisted developer workflow**: see [`ai-tooling/`](./ai-tooling)
  for ready-to-use Claude Code, GitHub Copilot, and OpenAI Codex
  configurations scoped to this codebase's conventions — directly
  addressing the JD's "Deploy AI-assisted tools... using MCPs, Agent
  Architecture, and post-AI evaluations."

---

## 2. Repository / directory structure

```
trading-platform/
├── apps/
│   ├── trade-capture-service/     # Java 17 / Spring Boot — ingest, validate, persist, publish
│   │   ├── src/main/java/com/balyasny/tradecapture/
│   │   │   ├── api/                # REST controllers
│   │   │   ├── config/              # Kafka topic/producer, observability config
│   │   │   ├── domain/              # JPA entities, request DTOs
│   │   │   ├── repository/          # Spring Data JPA repositories
│   │   │   └── service/             # Validation, enrichment, orchestration
│   │   ├── src/main/resources/      # application.yml, Flyway-style SQL migration
│   │   ├── src/test/.../unit/       # Pure unit tests (no Spring context)
│   │   ├── src/test/.../integration/# Testcontainers (real Kafka + Postgres) tests
│   │   ├── pom.xml                  # Maven build
│   │   ├── build.gradle.kts         # Gradle build (equivalent, parallel path)
│   │   └── Dockerfile
│   ├── pnl-risk-consumer/          # Java 17 / Spring Boot — Kafka consumer, position book, P&L
│   │   └── ... (same shape as above)
│   └── frontend/                   # Angular 18 — blotter, positions dashboard, trade entry
│       ├── src/app/core/           # services, models, interceptors
│       ├── src/app/features/       # trade-blotter, positions, trade-entry (standalone components)
│       ├── e2e/                    # Playwright end-to-end specs
│       └── Dockerfile / nginx.conf
├── libs/
│   └── common-events/              # Shared Kafka event schemas (TradeEvent, EnrichedTradeEvent)
├── ai-tooling/                     # Claude Code / GitHub Copilot / OpenAI Codex configs (also zipped separately)
├── .github/
│   ├── workflows/                  # pr-verification.yml, release.yml (Nx-orchestrated CI/CD)
│   ├── CODEOWNERS, PR/issue templates
├── docker-compose.yml              # Local Kafka + Postgres + all three services
├── nx.json / package.json          # Nx monorepo orchestration
├── pom.xml                          # Root Maven reactor (aggregates libs/common-events + both services)
├── settings.gradle.kts              # Root Gradle multi-project build
└── lefthook.yml                     # Pre-commit hooks (lint, format, spotless, commit-msg)
```

**Why a monorepo, and why Nx**: the frontend and both backend services
version and release together, share the `common-events` schema, and need a
single CI graph that only rebuilds what actually changed (`nx affected`).
Nx orchestrates that graph across Java (via `nx:run-commands` wrapping
Maven/Gradle) and Angular (native `@nx/angular` executors) — this is the
same pattern used in the attached `FULLSTACK_BUILD_GITHUB_BEST_PRACTICES.md`
best-practices doc, and both GitHub Actions workflows below implement it
exactly as specified there (Nx-affected PR verification + Nx Release
changelog/tagging on merge to `main`).

---

## 3. Architecture at a glance

```
┌───────────────┐      REST (ops entry / adapter)     ┌──────────────────────┐
│  Angular UI   │ ───────────────────────────────────▶│ trade-capture-service │
│ (blotter, P&L,│◀────────────────────────────────────│  validate → persist   │
│  trade entry) │        REST (blotter/positions)      │  → publish            │
└───────┬───────┘                                       └──────────┬────────────┘
        │                                                            │ Kafka: trade-capture.executions.v1
        │                                                            ▼
        │                                              ┌──────────────────────┐
        │                                              │ (enrichment, in-proc) │
        │                                              └──────────┬────────────┘
        │                                                          │ Kafka: trade-capture.enriched.v1
        │                                                          ▼
        │                                              ┌──────────────────────┐
        └───────────────── REST (positions) ──────────▶│  pnl-risk-consumer    │
                                                          │  position book + P&L │
                                                          └──────────────────────┘
                             Postgres: trades (audit of record, both services independently scalable)
```

Failed publishes retry with backoff; a bounded retry exhaustion routes to a
per-service DLQ topic (`trade-capture.executions.dlq.v1` /
`pnl-risk.enriched.dlq.v1`) for manual replay — the standard "fail-safe"
pattern for a pipeline that cannot silently lose a trade.

---

## 4. Prerequisites

You need, on **both** Windows 11 and Ubuntu:

| Tool | Version | Purpose |
|---|---|---|
| JDK | 17 (Temurin recommended) | Both Java services |
| Maven | 3.9+ | Backend build (path A) |
| Gradle | 8.10+ | Backend build (path B) |
| Node.js | 22 LTS | Nx, Angular, tooling |
| npm | 10+ (bundled with Node 20) | JS package management |
| Docker Desktop (Win11) / Docker Engine + Compose (Ubuntu) | latest | Kafka, Postgres, containerized run |
| Git | latest | Version control |

### 4.1 Ubuntu 22.04/24.04 setup

```bash
# Java 17
sudo apt update
sudo apt install -y openjdk-17-jdk

# Maven
sudo apt install -y maven

# Gradle (via SDKMAN, recommended over apt for version control)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle 8.10

# Node.js 22 (via nvm, recommended)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
source ~/.bashrc
nvm install 22
nvm use 22

# Docker + Compose plugin
sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER   # log out/in after this

# Verify
java -version && mvn -version && gradle -version && node -v && npm -v && docker --version
```

### 4.2 Windows 11 setup

```powershell
# Using winget (built into Windows 11)
winget install EclipseAdoptium.Temurin.17.JDK
winget install Apache.Maven
winget install Gradle.Gradle
winget install OpenJS.NodeJS.LTS
winget install Docker.DockerDesktop
winget install Git.Git

# Restart your terminal, then verify
java -version
mvn -version
gradle -version
node -v
npm -v
docker --version
```

> Start **Docker Desktop** manually after install (Windows) before running
> anything that needs Kafka/Postgres — the Linux containers backend must
> be selected (default on modern Docker Desktop).

---

## 5. First-time repo setup (both OSes, once cloned/imported)

```bash
# From the repository root
npm install             # installs Nx + Angular + all JS devDependencies
npx lefthook install    # activates pre-commit hooks (lint, spotless)
```

> **Why `npm install` and not `npm ci`?** `npm ci` demands the committed
> `package-lock.json` match a fresh dependency resolution *exactly* — any
> drift at all (even in an unused transitive package neither this repo nor
> its dependencies actually need) hard-fails the whole install. `npm
> install` resolves and installs the same dependencies but reconciles
> small inconsistencies instead of refusing to proceed. For a project this
> size, "always installs, self-heals minor drift" beats "occasionally
> refuses to build over something nobody touched" — every `npm install` in
> this repo's workflows, Dockerfiles, and local setup instructions is that
> deliberate choice, not an oversight.

### 5.1 Materializing the build-tool wrappers

To keep this repository's checked-in size small and avoid binary blobs in
git, the Maven/Gradle **wrapper scripts** (`mvnw`, `mvnw.cmd`, `gradlew`,
`gradlew.bat`) are not pre-generated — only their `.properties` pointer
files are (`.mvn/wrapper/maven-wrapper.properties`,
`gradle/wrapper/gradle-wrapper.properties`), and CI installs Maven/Gradle
directly via `actions/setup-java` and `gradle/actions/setup-gradle` rather
than depending on wrapper binaries. Generate the wrappers locally, once,
with network access:

```bash
# Gradle wrapper (from repo root)
gradle wrapper --gradle-version 8.10

# Maven wrapper (from repo root)
mvn -N wrapper:wrapper -Dmaven=3.9.9
```

After this, `./gradlew` and `./mvnw` (or `gradlew.bat` / `mvnw.cmd` on
Windows) work exactly like the `gradle`/`mvn` commands used throughout this
README — use whichever you prefer.

---

## 6. Running everything locally

### 6.1 Fastest path: Docker Compose (all services + infra)

```bash
docker compose up --build
```

This starts Kafka (KRaft mode, no ZooKeeper needed), Postgres, both Java
services, and the Angular frontend (served via nginx, proxying `/api/*` to
the backends). Once healthy:

- Frontend: http://localhost:4200
- trade-capture-service: http://localhost:8080/actuator/health
- pnl-risk-consumer: http://localhost:8081/actuator/health

### 6.2 Development path: infra in Docker, apps run natively (hot reload)

```bash
# 1. Start only infra
docker compose up kafka postgres

# 2. Run the Flyway-style migration once (or let Hibernate validate against
#    a schema you create manually using apps/trade-capture-service/src/main/resources/db/migration/V1__init_trades.sql)
psql -h localhost -U trade_capture -d trade_capture -f apps/trade-capture-service/src/main/resources/db/migration/V1__init_trades.sql

# 3a. Run trade-capture-service — Maven
cd apps/trade-capture-service
mvn spring-boot:run
# 3b. ...or Gradle
gradle bootRun

# 4. In another terminal, run pnl-risk-consumer the same way
cd apps/pnl-risk-consumer
mvn spring-boot:run    # or: gradle bootRun

# 5. In another terminal, run the Angular frontend with hot reload
npx nx serve frontend
# → http://localhost:4200, proxying to localhost:8080 / localhost:8081
```

### 6.3 Smoke-test the pipeline end-to-end

```bash
curl -X POST http://localhost:8080/api/v1/trades \
  -H "Content-Type: application/json" \
  -d '{
    "externalOrderId": "EXT-001", "bookId": "SYSMACRO-EQ-01", "traderId": "TRADER-1",
    "instrumentId": "AAPL", "instrumentType": "EQUITY", "side": "BUY",
    "quantity": 100, "price": 189.32, "currency": "USD",
    "executionTimestamp": "2026-09-23T14:00:00Z", "venue": "NASDAQ", "sourceSystem": "MANUAL_ENTRY"
  }'

curl http://localhost:8080/api/v1/trades/book/SYSMACRO-EQ-01
curl http://localhost:8081/api/v1/positions
```

---

## 7. Building & testing

### 7.1 Whole monorepo, via Nx (recommended)

```bash
npm run build     # nx run-many --target=build --all (Angular + wraps Java builds if configured as Nx projects)
npm run test      # nx run-many --target=test --all
npm run lint       # nx run-many --target=lint --all
npm run e2e        # Playwright, frontend
```

### 7.2 Backend only — Maven

```bash
# From repo root (builds libs/common-events, then both services, reactor-ordered)
mvn -B verify

# Single module
cd apps/trade-capture-service
mvn verify   # unit + Testcontainers integration tests + JaCoCo coverage report + Spotless format check
```

### 7.3 Backend only — Gradle

```bash
# From repo root
gradle build

# Single module
cd apps/trade-capture-service
gradle test jacocoTestReport
```

### 7.4 Frontend only

```bash
npx nx test frontend      # Jest unit tests, coverage threshold enforced (80% lines/functions)
npx nx lint frontend      # ESLint + Angular template rules
npx nx e2e frontend       # Playwright, spins up dev server automatically
npx nx build frontend --configuration=production
```

### 7.5 Test coverage philosophy

Coverage gates are enforced (JaCoCo ≥80% line coverage on the backend,
Jest thresholds on the frontend) and Testcontainers-backed integration
tests exercise the **real** Kafka + Postgres wire protocol rather than
mocks, plus Playwright E2E specs exercise the actual browser UI. These are
real, meaningful tests intended to genuinely catch regressions — not
padding to hit an arbitrary number.

---

## 8. CI/CD — exactly as specified in `FULLSTACK_BUILD_GITHUB_BEST_PRACTICES.md`

Two workflows in `.github/workflows/`:

- **`pr-verification.yml`** — triggers on every PR into `main`, including
  feature branches tested in isolation. Enforces a Conventional-Commit PR
  title, runs `nx affected` lint/test/build for the frontend, a Trivy CVE
  scan, the full Maven `verify` reactor (unit + Testcontainers integration
  tests + coverage gate) and a parity Gradle build, then a Playwright E2E
  job. Concurrency group cancels superseded runs on new pushes to the same
  PR.
- **`release.yml`** — triggers on push to `main` (i.e. every merged PR).
  Runs `nx release` to compute the next semantic version from Conventional
  Commits, writes/commits `CHANGELOG.md`, tags the commit, builds
  production artifacts for both Java services and the frontend, generates
  **Sigstore build provenance attestations** for all three, and pushes
  signed container images to **GHCR** (`ghcr.io/<org>/<repo>/<service>`).

This matches the attached best-practices document's low-constraint,
high-security adaptation for a standard GitHub-hosted-runner repo (GHCR
instead of AWS ECR, native Sigstore attestation, scoped npm/Maven/Gradle
caching) — every workflow step listed there is implemented here.

### 8.1 Branch protection (configure once, in GitHub repo settings)

GitHub Actions cannot set branch protection rules themselves (they're a
repo Settings feature, not a workflow artifact), so enable this once after
import: **Settings → Branches → Add rule** for `main`:
- Require a pull request before merging (1+ approval; CODEOWNERS enforced — see `.github/CODEOWNERS`)
- Require status checks to pass: `pr-title-lint`, `verify`, `e2e`
- Require branches to be up to date before merging
- Require signed commits (optional but recommended given this repo also produces signed release artifacts)
- Do not allow force pushes or deletions on `main`

---

## 9. AI-assisted developer tooling

See [`ai-tooling/README.md`](./ai-tooling/README.md) for cross-compatible
**Claude Code**, **GitHub Copilot**, and **OpenAI Codex** configurations
scoped to this repo's architecture and conventions (same content is also
packaged as a standalone `ai-tooling.zip` for import into a fresh clone on
either Windows 11 or Ubuntu).

---

## 10. Murex & ION adapters — switchable upstream integration

`trade-capture-service` ships with three fully-implemented, always-available
ingestion adapters behind a common interface
(`com.balyasny.tradecapture.adapter.TradeSourceAdapter`), so the platform
can take live feeds from **Murex**, **ION**, or manual/ops entry — matching
the JD's explicit call-out for integrating with third-party platforms such
as Murex and ION.

| Endpoint | Adapter | Native shape modeled on |
|---|---|---|
| `POST /api/v1/adapters/murex/trades` | `MurexTradeAdapter` | Murex MX.3 trade-capture export vocabulary (`DEAL_ID`, `PORTFOLIO`, `mxProductType`, Murex buy/sell convention, nested `structuredTerms` for autocallable notes) |
| `POST /api/v1/adapters/ion/trades` | `IonExecutionAdapter` | ION (Fidessa/Triton) FIX 4.4 `ExecutionReport` tags (`ClOrdID`, `ExecID`, `Symbol`, `SecurityType`, `Side`, `LastQty`/`LastPx`, `TransactTime`, `LastMkt`) |
| `POST /api/v1/trades` and `/api/v1/adapters/manual/trades` | `ManualEntryAdapter` | This platform's own canonical shape — backs the Angular trade-entry form |

All three endpoints are **always live simultaneously** — in production
Murex and ION post concurrently and independently, so nothing about
switching "active" ever disables either dedicated endpoint.

### The default/active switch

`trade-capture.adapter.active` (env var `TRADE_CAPTURE_ACTIVE_ADAPTER`,
default `MANUAL_ENTRY`) selects which adapter the **generic** endpoint,
`POST /api/v1/adapters/default/trades`, delegates to — the endpoint an ops
runbook points at when it wants "send this to whatever the desk's primary
feed is" without hardcoding a system. It can also be flipped **at runtime,
with no redeploy**:

```bash
# Inspect current + configured-default active adapter
curl http://localhost:8080/api/v1/adapters/active

# Cut the desk's default path over to Murex
curl -X PUT http://localhost:8080/api/v1/adapters/active/MUREX

# ...or to ION
curl -X PUT http://localhost:8080/api/v1/adapters/active/ION

# Revert to the configured default (MANUAL_ENTRY unless overridden)
curl -X POST http://localhost:8080/api/v1/adapters/active/reset
```

The switch is process-local (in-memory `AtomicReference`); a real
multi-instance deployment would back this with a distributed config store
(Consul/etcd) so every instance picks up the change together — noted
explicitly as a trade-off in the table below.

### Example payloads

```bash
# Murex — equity
curl -X POST http://localhost:8080/api/v1/adapters/murex/trades \
  -H "Content-Type: application/json" -d '{
    "dealId": "DEAL-100", "mxPortfolio": "SYSMACRO-EQ-01", "counterparty": "CPTY-GS",
    "trader": "TRADER-1", "instrumentCode": "AAPL", "mxProductType": "EQUITY_SPOT",
    "buySell": "B", "nominalQuantity": 100, "dealPrice": 189.32, "dealCurrency": "USD",
    "tradeDateTime": "2026-09-23T14:00:00Z", "executionVenue": "NASDAQ"
  }'

# ION — equity fill
curl -X POST http://localhost:8080/api/v1/adapters/ion/trades \
  -H "Content-Type: application/json" -d '{
    "clOrdId": "CLORD-100", "execId": "EXEC-100", "account": "SYSMACRO-EQ-01",
    "trader": "TRADER-1", "symbol": "MSFT", "securityType": "CS", "side": "1",
    "lastQty": 50, "lastPx": 410.10, "currency": "USD",
    "transactTime": "2026-09-23T14:05:00Z", "lastMkt": "XNAS"
  }'
```

Every adapter routes through the same `TradeValidationService` and
`TradeCaptureOrchestrator` as manual entry — validation rules (including
autocallable barrier/maturity checks), Postgres persistence, and Kafka
publication are identical no matter which upstream system produced the
fill. Tests: `MurexTradeAdapterTest`, `IonExecutionAdapterTest`,
`AdapterRegistryTest` (unit), `AdapterSwitchIntegrationTest` (Testcontainers,
exercises the live switch end-to-end).

## 11. Repository housekeeping — keeping Actions runs and branches from piling up

Two separate things pile up over time in an active repo, and they need two separate fixes:

### 11.1 Branches (one-click, do this once)

GitHub does **not** delete a PR's branch automatically by default — merged and closed PRs leave their branch behind forever unless you turn this on:

**Settings → General → Pull Requests → check "Automatically delete head branches"**

This is a repo setting, not something a workflow file can turn on for you, and it alone handles branch pile-up for every merged or closed PR going forward.

### 11.2 Workflow run history (no repo setting exists for this — hence the workflow)

GitHub never automatically deletes old Actions run history, successful or not — `.github/workflows/housekeeping.yml` handles this instead. It runs weekly (Sundays 06:00 UTC) and is also available as a manual "Run workflow" button in the Actions tab. For each workflow (`pr-verification.yml`, `release.yml`, `housekeeping.yml` itself) it:

1. Deletes every non-successful run (failed, cancelled, skipped, timed-out) — these have no ongoing value once superseded.
2. Deletes every successful run **except the single most recent one** — so `main`'s Actions tab always shows exactly one green run per workflow as the current state, not an ever-growing history.

This workflow needs no additional secrets — `secrets.GITHUB_TOKEN` with the `actions: write` permission already declared in the file is sufficient.

### 11.3 Dependency updates (manual — no bot running against this repo)

This repo does not run Dependabot or any other automated dependency-update bot. On a small, single-maintainer repo, an update bot's PRs still require someone to review, merge, and — as the earlier iterations of this project's CI/CD found the hard way — verify locally before merging, since a bot cannot know that a "minor" bump (e.g. `zone.js`) is actually breaking for this specific dependency graph. That review burden was outweighing the benefit here, so it's gone.

Check for updates on whatever cadence suits you, review changelogs for anything you take, and verify locally (`npm install`, `nx build`, `nx test`, `mvn verify`) before pushing — exactly the steps that would have caught every dependency-bump failure this repo hit during development:

```bash
# npm (frontend + Nx tooling)
npm outdated
npm audit                          # flags known CVEs in current deps

# Maven (both Java services)
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates

# Gradle
gradle dependencyUpdates           # requires the com.github.ben-manes.versions plugin (not included by default)
```

If you later want automation back without Dependabot's per-PR overhead, a scheduled workflow that runs `npm outdated`/`mvn versions:display-dependency-updates` and opens a single summary issue (not a PR per package) is a lighter-weight middle ground — flags what's available without anything auto-merging or needing individual review.

## 12. Design notes: production-viability trade-offs made for this showcase

Being transparent about what's simplified vs. what mirrors a real desk
system, since a senior engineer should be able to name these trade-offs in
an interview:

| Area | This showcase | Real production equivalent |
|---|---|---|
| Position store | In-memory `ConcurrentHashMap` in `pnl-risk-consumer` | Redis/Apache Ignite for horizontal scale-out + warm-restart recovery via Kafka offset replay |
| Reference data | In-memory seeded cache (`ReferenceDataEnrichmentService`) | Call-out to the firm's reference-data service / static-data cache with TTL invalidation |
| Auth | Stub bearer token interceptor | Firm SSO (Okta/internal) with short-lived JWTs, mTLS between services |
| Schema evolution | Plain Java records (`schemaVersion` field reserved) | Avro/Protobuf + Confluent Schema Registry with compatibility enforcement |
| Murex/ION integration | Fully implemented REST adapters (`MurexTradeAdapter`, `IonExecutionAdapter`) translating each system's native message shape into `TradeEvent` | Same shape in production, but fed by a real Murex TDD/MXML gateway and an ION FIX engine/drop-copy feed rather than direct REST POSTs |
| Active-adapter switch | In-memory `AtomicReference` in `AdapterRegistry`, per-instance | Distributed config store (Consul/etcd) so every instance in the cluster switches together |
| Secrets | `.env`-style local defaults | Vault / AWS Secrets Manager, injected at deploy time |

This list is itself meant to demonstrate the awareness the JD asks for:
"ability to quickly learn and understand existing systems... working
independently while collaborating effectively with a distributed global
team."

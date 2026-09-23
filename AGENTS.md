# AGENTS.md — trading-platform

Instructions for OpenAI Codex (CLI and IDE extension) operating in this
repository.

## Project summary

Nx monorepo combining two Java 17 / Spring Boot Kafka microservices
(`apps/trade-capture-service`, `apps/pnl-risk-consumer`), a shared event
schema library (`libs/common-events`), and an Angular 18 frontend
(`apps/frontend`). Domain: real-time equity/autocallable trade capture
feeding a streaming P&L/risk pipeline.

## Setup commands

```bash
npm ci
npx lefthook install
docker compose up -d kafka postgres
```

## How to build

```bash
mvn -B verify          # Java, full reactor (preferred for CI parity)
gradle build            # Java, alternative build path — keep both in sync
npx nx build frontend --configuration=production
```

## How to test

```bash
mvn -B verify                    # runs unit + Testcontainers integration tests + JaCoCo gate
npx nx test frontend             # Jest, coverage-gated
npx nx e2e frontend              # Playwright
```

Run the narrowest relevant test command after any change — for a
single-module Java change, `cd` into that module and run `mvn verify`
rather than the full reactor.

## Code style rules Codex must follow

1. Java: records for event/DTO types, constructor injection, one class per
   file, package-by-layer (`api`/`config`/`domain`/`repository`/`service`).
2. Any Kafka producer added or modified must preserve `acks=all` +
   `enable.idempotence=true`.
3. Angular: standalone components, signals + TanStack Query only — do not
   introduce NgModules or a second state-management library.
4. Every new public class/method gets a doc comment explaining *why*, not
   just *what* (see existing classes for the expected tone/length).
5. Commit messages: Conventional Commits, enforced by CI.

## PR expectations

Before proposing a PR is done, Codex should confirm:
- `mvn -B verify` passes for any touched Java module
- `npx nx affected --target=test` passes for any touched frontend code
- `npx nx affected --target=lint` is clean
- No secrets, credentials, or `.env` values are included in the diff

## Do not touch without explicit instruction

- `.github/workflows/*.yml` (CI/CD contracts — changes here need human review)
- `pom.xml` / `build.gradle.kts` version numbers (dependency bumps go through Dependabot)

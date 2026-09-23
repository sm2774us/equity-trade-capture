# CLAUDE.md — trading-platform

This file is read automatically by Claude Code at session start. It gives
repo-specific context so suggestions match this codebase's conventions
without you having to re-explain them every session.

## What this repo is

Nx monorepo: two Java 17 / Spring Boot Kafka services
(`apps/trade-capture-service`, `apps/pnl-risk-consumer`) sharing event
schemas from `libs/common-events`, plus an Angular 18 frontend
(`apps/frontend`). Real-time trade capture → Kafka → P&L/risk pipeline for
equities and autocallable notes.

## Build & test commands (use these, not guesses)

```bash
# Whole repo
npm run build            # nx run-many --target=build --all
npm run test              # nx run-many --target=test --all
npm run lint               # nx run-many --target=lint --all

# Backend (either works — repo ships both)
mvn -B verify                       # from repo root, full reactor
cd apps/trade-capture-service && mvn verify
gradle build                        # from repo root

# Frontend
npx nx test frontend
npx nx lint frontend
npx nx e2e frontend
npx nx serve frontend

# Local infra
docker compose up kafka postgres
```

## Conventions to follow

- **Java**: records for immutable event/DTO shapes (see
  `libs/common-events/src/main/java/com/balyasny/events/TradeEvent.java`).
  Package-by-layer within each service (`api/`, `config/`, `domain/`,
  `repository/`, `service/`). Constructor injection only, no field
  `@Autowired`. Every public service method that can fail on bad input
  throws `TradeCaptureException` (or a new domain-specific unchecked
  exception), never a raw `RuntimeException`.
- **Kafka**: any new topic goes through `KafkaTopicConfig` as a declared
  `NewTopic` bean; any new producer follows the `acks=all` +
  `enable.idempotence=true` pattern in `KafkaProducerConfig` — this is
  non-negotiable for anything that feeds P&L.
- **Tests**: unit tests (`src/test/.../unit/`) must not start a Spring
  context. Integration tests (`src/test/.../integration/`) use
  Testcontainers against real Kafka/Postgres, not mocks — follow the
  pattern in `TradeCaptureIntegrationTest.java`.
- **Angular**: standalone components only (no NgModules), signals for
  local component state, TanStack Query (`injectQuery`) for server state —
  do not introduce a second server-state pattern (no manual
  subscribe-in-ngOnInit + component field). Angular Material for form
  controls/tables, Tailwind utility classes for layout/spacing.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`,
  `refactor:`) — CI's `nx release` changelog generation depends on this,
  and the PR-title-lint check in `pr-verification.yml` enforces it.

## What NOT to do

- Don't add a new state-management library (no NgRx store, no Zustand) —
  TanStack Query + signals is the deliberate choice for this app's scope.
- Don't bypass `TradeValidationService` — every path into
  `TradeCaptureOrchestrator.capture()` must go through validation first.
- Don't hand-roll JSON (de)serialization for Kafka messages — use the
  existing `JsonSerializer`/`JsonDeserializer` + trusted-packages config.

## MCP servers configured for this repo

See `.claude/mcp-config.json` in this folder for a starter config wiring
up filesystem and (optionally) GitHub MCP servers scoped to this repo —
copy it to your global Claude Code config or project `.mcp.json` per
Anthropic's Claude Code docs.

# GitHub Copilot instructions — trading-platform

Applies to Copilot Chat and inline completions across VS Code, JetBrains,
and Visual Studio for this repository.

## Repo shape

Nx monorepo. Two Java 17 / Spring Boot Kafka services
(`apps/trade-capture-service`, `apps/pnl-risk-consumer`), one shared event
schema library (`libs/common-events`), one Angular 18 frontend
(`apps/frontend`). Domain: real-time trade capture and P&L/risk for
equities and autocallable notes.

## When suggesting Java code

- Target Java 17 language features (records, pattern matching, sealed
  types where useful) — do not suggest Java 8-era patterns (no anonymous
  inner classes where a lambda/method reference fits, no raw `Date`, use
  `java.time`).
- Follow the existing package-by-layer structure (`api`, `config`,
  `domain`, `repository`, `service`) inside whichever service you're in.
- New Kafka producers must set `acks=all` and
  `enable.idempotence=true` — copy the pattern in
  `KafkaProducerConfig.java`, don't invent a new one.
- New REST endpoints go through `@RestController` + constructor injection,
  with a `@ExceptionHandler` for any new domain exception, matching
  `TradeController.java`.
- Every new service class needs a corresponding unit test under
  `src/test/.../unit/` with no Spring context, per existing examples.

## When suggesting TypeScript/Angular code

- Standalone components only. Use `inject()` for DI, signals for local
  state, `injectQuery` (TanStack Angular Query) for anything server-backed
  — do not suggest `HttpClient` calls wired up manually in `ngOnInit`.
- Angular Material for form controls and tables; Tailwind utility classes
  for spacing/layout (see `tailwind.config.js` for the `trading-*` color
  tokens already defined — reuse them, don't invent new hex colors inline).
- Every new component needs a `.spec.ts` following the
  `TestBed.configureTestingModule` + `provideHttpClientTesting` pattern
  used in the existing `*.component.spec.ts` files.

## Commit messages

Conventional Commits only (`feat:`, `fix:`, `chore:`, `docs:`,
`refactor:`, `test:`) — required by the `pr-title-lint` CI check and by
`nx release`'s changelog generation.

## Build/test commands to suggest when relevant

```
mvn -B verify
gradle build
npx nx test frontend
npx nx affected --target=lint
docker compose up kafka postgres
```

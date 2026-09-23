/**
 * Root Gradle build for the trading-platform multi-project build.
 *
 * The root project itself has no source — `libs/common-events`,
 * `apps/trade-capture-service`, and `apps/pnl-risk-consumer` (declared in
 * settings.gradle.kts) are the real subprojects, each with its own
 * build.gradle.kts. This file exists so `gradle build` / `gradle test`
 * run from the repository root work exactly like `mvn verify` does from
 * the root pom.xml — one command, whole reactor — rather than requiring
 * you to `cd` into each module.
 */

plugins {
    base
}

// `gradle build` / `gradle test` / `gradle check` at the root delegate to
// every subproject's same-named task automatically via Gradle's built-in
// task aggregation (the `base` plugin wires this up) — no extra config
// needed for the common case. These aliases just make the root-level
// commands referenced in the README explicit and discoverable via
// `gradle tasks`.

tasks.register("buildAll") {
    group = "build"
    description = "Builds every subproject (libs/common-events, both Java services)."
    dependsOn(subprojects.map { "${it.path}:build" })
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs tests for every subproject."
    dependsOn(subprojects.map { "${it.path}:test" })
}

tasks.register("cleanAll") {
    group = "build"
    description = "Cleans every subproject's build output."
    dependsOn(subprojects.map { "${it.path}:clean" })
}

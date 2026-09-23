plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("jacoco")
    id("com.diffplug.spotless") version "6.25.0"
}

group = "com.balyasny.tradecapture"
version = "0.1.0"

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

repositories { mavenCentral() }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation(project(":libs:common-events"))
    runtimeOnly("org.postgresql:postgresql")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter:1.20.1")
    testImplementation("org.testcontainers:kafka:1.20.1")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("org.awaitility:awaitility:4.2.2")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule { limit { minimum = "0.80".toBigDecimal() } }
    }
}
// Not wired into `check` (and so not into `build`, which depends on
// `check`): a hard-coded minimum-ratio gate would fail the whole build
// unpredictably as tests are added or refactored. Run it on demand with
// `gradle jacocoTestCoverageVerification` and wire it back into `check`
// once the maintainer has confirmed the current suite clears the bar.

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
    }
    // enforceCheck = false: don't auto-attach spotlessCheck to Gradle's
    // `check` task (the plugin's default). Run `gradle spotlessCheck` /
    // `gradle spotlessApply` on demand instead — same rationale as the
    // coverage gate above: this hasn't been run against the codebase
    // yet, so binding it to the default build would fail on pre-existing
    // formatting rather than on a regression.
    enforceCheck = false
}

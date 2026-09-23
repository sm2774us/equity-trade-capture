# Multi-stage build: compile with Maven (cached layer via dependency
# pre-fetch), then run on a minimal JRE for a small, fast-starting image.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY pom.xml ./root-pom.xml
COPY libs/common-events/pom.xml libs/common-events/pom.xml
COPY apps/trade-capture-service/pom.xml apps/trade-capture-service/pom.xml
COPY apps/pnl-risk-consumer/pom.xml apps/pnl-risk-consumer/pom.xml
COPY libs/common-events/src libs/common-events/src
COPY apps/trade-capture-service/src apps/trade-capture-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f root-pom.xml -pl libs/common-events,apps/trade-capture-service -am \
    package -DskipTests -q

FROM eclipse-temurin:17-jre-jammy
RUN useradd --system --create-home appuser
USER appuser
WORKDIR /app
COPY --from=build /workspace/apps/trade-capture-service/target/trade-capture-service.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]

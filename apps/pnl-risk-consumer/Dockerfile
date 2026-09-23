FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY pom.xml ./root-pom.xml
COPY libs/common-events/pom.xml libs/common-events/pom.xml
COPY apps/trade-capture-service/pom.xml apps/trade-capture-service/pom.xml
COPY apps/pnl-risk-consumer/pom.xml apps/pnl-risk-consumer/pom.xml
COPY libs/common-events/src libs/common-events/src
COPY apps/pnl-risk-consumer/src apps/pnl-risk-consumer/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f root-pom.xml -pl libs/common-events,apps/pnl-risk-consumer -am \
    package -DskipTests -q

FROM eclipse-temurin:17-jre-jammy
RUN useradd --system --create-home appuser
USER appuser
WORKDIR /app
COPY --from=build /workspace/apps/pnl-risk-consumer/target/pnl-risk-consumer.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]

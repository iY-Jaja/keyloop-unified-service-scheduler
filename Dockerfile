FROM maven:3.9.9-eclipse-temurin-8 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY openapi.yaml .
RUN mvn -B -ntp package

FROM eclipse-temurin:8-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app
COPY --from=build /workspace/target/service-scheduler-1.0.0.jar /app/app.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
    CMD curl --fail --silent http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# syntax=docker/dockerfile:1
FROM eclipse-temurin:11-jdk-jammy AS build
WORKDIR /workspace
# Prefer the Google-hosted Maven Central mirror; repo.maven.apache.org rate-limits (HTTP 429)
# anonymous image builds. Override with --build-arg GRADLE_MIRROR=<url> if needed.
ARG GRADLE_MIRROR=https://maven-central.storage-download.googleapis.com/maven2
RUN mkdir -p /root/.gradle && printf '%s\n' \
  "allprojects {" \
  "  buildscript { repositories { maven { url '${GRADLE_MIRROR}' } } }" \
  "  repositories { maven { url '${GRADLE_MIRROR}' } }" \
  "}" \
  "settingsEvaluated { settings ->" \
  "  settings.pluginManagement { repositories { maven { url '${GRADLE_MIRROR}' }; gradlePluginPortal() } }" \
  "}" > /root/.gradle/init.gradle
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN --mount=type=cache,target=/root/.gradle/caches,id=gradle-caches --mount=type=cache,target=/root/.gradle/wrapper,id=gradle-wrapper \
    ./gradlew --no-daemon --version
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle/caches,id=gradle-caches --mount=type=cache,target=/root/.gradle/wrapper,id=gradle-wrapper \
    ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:11-jre-jammy
WORKDIR /app
RUN mkdir -p /app/data
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
ENV COMMENTS_SERVICE_PORT=8081 \
    COMMENTS_DB_PATH=/app/data/comments.db
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=10 \
  CMD curl -fs http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

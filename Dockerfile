# syntax=docker/dockerfile:1

# Build
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 set -eu; \
    chmod +x mvnw; \
    ./mvnw -B -ntp clean package -DskipTests; \
    cp target/*.jar /app/app.jar

# Runtime
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN set -eu; \
    groupadd --system app && useradd --system --gid app --home-dir /app app; \
    mkdir /app/logs /app/data && chown app:app /app/logs /app/data

COPY --from=build --chown=app:app /app/app.jar /app/app.jar
USER app

# secrets.properties is gitignored and excluded from the build context, so you'd pass these when running:
#   DISCORD_TOKEN -> discord.token
#   WIKI_API_KEY  -> wiki.api.key
ENV SPRING_CONFIG_IMPORT="optional:classpath:config/secrets.properties"

# The default prod profile writes rolling logs to /app/logs, while effects scraped from the wiki
# are appended to /app/data/ScrapedEffects.txt. Bind-mount either directory to persist its data.
# A bind-mounted host directory must be writable by the container's app user (check its uid with
# `docker run --rm --entrypoint id lockium` and chown the host directory accordingly).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

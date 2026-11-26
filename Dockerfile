FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --only-upgrade --no-install-recommends perl-base \
    && rm -rf /var/lib/apt/lists/*
# Spring Boot se inicia directamente con Java y no utiliza el supervisor Pebble.
RUN rm -f /usr/bin/pebble
WORKDIR /app
COPY --from=build /build/target/maestranza-choko-*.jar /app/choko.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/choko.jar"]

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/maestranza-choko-*.jar /app/choko.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/choko.jar"]

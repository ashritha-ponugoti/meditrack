# ---- Stage 1: build the jar with Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -Dmaven.test.skip=true

# ---- Stage 2: run it on a small Java 17 image ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render's free instance has only 512 MB RAM, so cap the Java heap.
ENV JAVA_TOOL_OPTIONS="-Xmx320m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# Render supplies PORT; application.properties already reads it (server.port=${PORT:8080}).
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

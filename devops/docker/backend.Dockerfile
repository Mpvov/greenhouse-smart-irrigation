# =============================================================================
# backend.Dockerfile — Multi-stage build for Spring Boot (WebFlux / Reactive)
#
# Stage 1 (builder): Uses Maven to compile and package the JAR
# Stage 2 (runtime): Uses a slim JRE image to run the JAR
#
# Multi-stage keeps the final image small by discarding Maven and source code.
# =============================================================================

# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first and resolve dependencies (cached layer — only re-runs if pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build the JAR (skip tests for faster image builds)
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create a non-root user for security best practices
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy only the fat JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# JVM flags:
#   -XX:+UseContainerSupport   : Respect Docker CPU/memory limits
#   -XX:MaxRAMPercentage=75.0  : Use up to 75% of container RAM for heap
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

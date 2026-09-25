# ==============================================================================
# Multi-Stage Dockerfile for dollar-api-java
# Optimized for minimal image size, security (non-root), and production workloads
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and POM first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure execute permission on mvnw
RUN chmod +x ./mvnw

# Pre-fetch project dependencies offline
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build the final jar
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal Distroless / Alpine Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

# Install curl for container health check
RUN apk --no-cache add curl tzdata && rm -rf /var/cache/apk/*

ENV TZ=America/Argentina/Buenos_Aires

# Create non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/dollar-api-java-*.jar /app/app.jar

# Switch to unprivileged user
USER appuser:appgroup

# Configure JVM flags optimized for containerized environments
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV PORT=8080

EXPOSE 8080

# Built-in container health check
HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

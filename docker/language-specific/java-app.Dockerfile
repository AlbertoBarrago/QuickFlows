# Dockerfile for Java applications

# Stage 1: Build stage
FROM maven:3.8-openjdk-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .

# Download dependencies (will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline

# Copy source code
COPY src/ ./src/

# Build the application
RUN mvn package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Copy configuration files if needed
COPY --from=build /app/src/main/resources/application.properties /app/config/

# Use the non-root user for security
USER appuser

# Expose application port
EXPOSE 8080

# Set JVM options
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"

# Run the application with proper configuration
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.config.location=file:/app/config/application.properties"]

# Health check for the application
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Usage:
# Build: docker build -t java-app:latest -f java-app.Dockerfile .
# Run: docker run -p 8080:8080 java-app:latest

# Benefits:
# 1. Multi-stage build reduces final image size
# 2. Alpine-based JRE minimizes attack surface
# 3. Non-root user improves security
# 4. Optimized JVM settings for containerized environments
# 5. Health check ensures application readiness
# 6. Dependency caching improves build performance
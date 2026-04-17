# ============================================
# Stage 1: Build Stage
# ============================================
FROM maven:3.9.4-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy Maven configuration
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests in Docker build)
RUN mvn clean package -DskipTests -B

# ============================================
# Stage 2: Runtime Stage
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider \
    http://localhost:8080/actuator/health || exit 1

# JVM optimization flags
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
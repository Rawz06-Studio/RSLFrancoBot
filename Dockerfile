# Multi-stage build for RSLFrancoBot
# Stage 1: Build the Java application
FROM maven:3.9-eclipse-temurin-25-alpine AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image with Java only
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# System dependencies
RUN apk add --no-cache \
    git \
    curl

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar ./app.jar

# Create non-root user for security
RUN addgroup -g 1000 botuser && \
    adduser -D -u 1000 -G botuser botuser && \
    chown -R botuser:botuser /app

USER botuser

# Expose health check port (if needed)
EXPOSE 8080

# Set environment variables (override via docker-compose or -e flags)
ENV DISCORD_TOKEN="" \
    SEED_API_DOMAIN="https://api.randomizer.example.com" \
    JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE="prod"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
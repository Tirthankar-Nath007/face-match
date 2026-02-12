# Build stage - using Maven wrapper from the project
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and settings
COPY mvnw ./mvnw
COPY mvnw.cmd ./mvnw.cmd
COPY .mvn ./.mvn

# Copy pom.xml and download dependencies first (for caching)
COPY pom.xml .

# Build with Maven wrapper
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B || true  # Try to download deps, ignore errors

# Copy source code and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built artifact from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 1: Build the application JAR
FROM maven:3.9.0-eclipse-temurin-17 AS builder
# Set the working directory inside the container
WORKDIR /app
# Copy the Maven project files into the container
COPY pom.xml ./
COPY src ./src
# Build the application
RUN mvn clean package -DskipTests
# Stage 2: Run the application
FROM eclipse-temurin:17-jre-focal
# Set the working directory
WORKDIR /app
# Copy the built JAR file from the builder stage
COPY --from=builder /app/target/transaction-0.0.1-SNAPSHOT.jar /app/transaction-0.0.1-SNAPSHOT.jar
# Copy any additional configuration files if required
# Expose the port the app runs on
EXPOSE 8084
# Run the JAR file
ENTRYPOINT ["java", "-jar", "transaction-0.0.1-SNAPSHOT.jar"]

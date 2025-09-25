# ==============================
# 1. Build stage (using Maven)
# ==============================
FROM maven:3.9.4-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Package the application (skip tests to speed up)
RUN mvn clean package -DskipTests

# ==============================
# 2. Run stage (lighter JDK image)
# ==============================
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy only the packaged JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port 8080 (Render will map its own $PORT)
EXPOSE 8080

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
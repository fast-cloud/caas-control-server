FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built JAR file (build locally with: ./gradlew bootJar)
COPY build/libs/*.jar app.jar

# Expose port (Spring Boot default)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]


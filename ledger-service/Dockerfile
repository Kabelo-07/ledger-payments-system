# Use OpenJDK 21 Alpine image
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy jar
COPY target/ledger-service-0.0.1-SNAPSHOT.jar app.jar

# Expose port (Spring Boot server.port)
EXPOSE 8081

# JVM options (tune as needed)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

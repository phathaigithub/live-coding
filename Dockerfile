# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Install Docker inside the container (to use Docker Out Of Docker - DooD)
# Or we can mount the docker.sock from the host
RUN apt-get update && apt-get install -y docker.io

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

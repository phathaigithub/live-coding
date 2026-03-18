# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# rebuild lại hoàn toàn, không dùng cache nội bộ của Maven
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Cài đặt Docker CLI để Backend có thể gọi lệnh Docker (DooD)
RUN apt-get update && apt-get install -y docker.io

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

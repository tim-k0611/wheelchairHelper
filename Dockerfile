# Multi-Stage Build für optimale Image-Größe

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Kopiere pom.xml und lade Dependencies (für besseres Caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kopiere Source Code
COPY src ./src

# Baue Application (Frontend wird automatisch gebaut)
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Kopiere JAR aus Build Stage
COPY --from=build /app/target/*.jar app.jar

# Port exposieren
EXPOSE 8083

# Health Check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:808/api/emergency/health || exit 1

# Starte Application
ENTRYPOINT ["java", "-jar", "app.jar"]
# ---------- Build Stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Run Stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Default log path inside the container — overridden via -e LOG_PATH=... at runtime
ENV LOG_PATH=/app/elk-resource/logs

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
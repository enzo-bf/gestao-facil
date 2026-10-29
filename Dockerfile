FROM maven:3.9.15-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine
RUN apk upgrade --no-cache && addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /workspace/target/gestao-facil-0.1.0-SNAPSHOT.jar app.jar
USER spring:spring
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

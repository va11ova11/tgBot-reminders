# Stage 1: сборка артефакта
FROM maven:3.8.6-openjdk-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# Stage 2: готовый исполняемый образ
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV SPRING_CONFIG_ADDITIONAL_LOCATION=/config/

ARG JAR_FILE=telegram-reminder-spring-0.0.1-SNAPSHOT.jar
COPY --from=builder /build/target/${JAR_FILE} app.jar

EXPOSE 8080

RUN addgroup -S app && useradd -S -G app app
USER app

ENTRYPOINT ["java","-jar","/app/app.jar"]
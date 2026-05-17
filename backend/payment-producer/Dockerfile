# Multi-stage build for payment-producer
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy shared-kernel first (dependency)
COPY backend/shared-kernel /shared-kernel
RUN cd /shared-kernel && mvn install -q -DskipTests

# Build the service
COPY backend/payment-producer/pom.xml .
RUN mvn dependency:go-offline -q
COPY backend/payment-producer/src ./src
RUN mvn package -q -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

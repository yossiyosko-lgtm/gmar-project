# שלב 1: בנייה
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# שלב 2: הרצה (אימג' קטן, רק JRE)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/care-circle.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
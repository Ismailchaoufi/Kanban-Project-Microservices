#FROM maven:3.9-eclipse-temurin-17 AS build
#WORKDIR /app
#COPY pom.xml .
#RUN mvn dependency:go-offline -B
#COPY src ./src
#RUN mvn clean package -DskipTests
#
#FROM eclipse-temurin:17-jre-alpine
#WORKDIR /app
#RUN addgroup -S spring && adduser -S spring -G spring
#USER spring:spring
#COPY --from=build /app/target/*.jar app.jar
#
#HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
#  CMD wget --quiet --tries=1 --spider http://localhost:8761/actuator/health || exit 1
#
#EXPOSE 8761
#ENTRYPOINT ["java", "-jar", "/app/app.jar"]


#For deploy in render
# Multi-stage build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source
COPY src ./src

# Build
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE ${PORT:-8080}

# Run with PORT environment variable
ENTRYPOINT ["sh", "-c", "java -Xmx300m -Xms256m -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
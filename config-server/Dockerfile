FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget --quiet --tries=1 --spider http://localhost:8888/actuator/health || exit 1

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


#For deploy in failway
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
## Important: Use PORT environment variable
#EXPOSE ${PORT:-8888}
#ENTRYPOINT ["sh", "-c", "java -Xmx400m -Xms256m -Dserver.port=${PORT:-8888} -jar /app/app.jar"]
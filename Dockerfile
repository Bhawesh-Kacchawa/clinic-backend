FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]


# FROM maven:3.9-eclipse-temurin-21 AS build

# WORKDIR /workspace
# COPY mvnw pom.xml ./
# COPY .mvn .mvn
# RUN ./mvnw -B -DskipTests dependency:go-offline

# COPY src src
# RUN ./mvnw -B -DskipTests package

# FROM eclipse-temurin:21-jre

# WORKDIR /app
# RUN useradd --system --uid 10001 appuser
# COPY --from=build /workspace/target/*.jar app.jar
# USER appuser

# EXPOSE 8080
# ENTRYPOINT ["java", "-jar", "app.jar"]

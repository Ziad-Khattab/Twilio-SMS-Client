FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

FROM maven:3-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn dependency:go-offline -B
COPY --from=frontend /app/src/main/webapp ./src/main/webapp
RUN mvn package -B -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=backend /app/target/*.war ./ROOT.war
ADD https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-runner/11.0.20/jetty-runner-11.0.20.jar /app/jetty-runner.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD curl -sf http://localhost:8080/ || exit 1
CMD ["java", "-jar", "/app/jetty-runner.jar", "--port", "8080", "/app/ROOT.war"]
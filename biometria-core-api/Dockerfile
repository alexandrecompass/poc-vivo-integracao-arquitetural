FROM maven:3.9.8-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml .
COPY biometria-legacy-soap/pom.xml biometria-legacy-soap/pom.xml
COPY biometria-core-api/pom.xml biometria-core-api/pom.xml
COPY biometria-gateway/pom.xml biometria-gateway/pom.xml
COPY biometria-core-api/src biometria-core-api/src

RUN mvn -pl biometria-core-api -am clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /workspace/biometria-core-api/target/biometria-core-api.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

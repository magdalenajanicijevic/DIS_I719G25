# BUILD STAGE

FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

ARG SERVICE

COPY common-security/pom.xml common-security/pom.xml
COPY common-security/src common-security/src

RUN mvn -f common-security/pom.xml clean install -DskipTests

COPY ${SERVICE}/pom.xml ${SERVICE}/pom.xml
COPY ${SERVICE}/src ${SERVICE}/src

RUN mvn -f ${SERVICE}/pom.xml clean package -DskipTests


# RUNTIME STAGE

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ARG SERVICE

COPY --from=build /workspace/${SERVICE}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
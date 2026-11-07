# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar con Maven + JDK 21 ----------
# Las imágenes base van fijadas por digest (inmutables); Dependabot propone la actualización cuando salga una nueva
FROM maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
WORKDIR /workspace

# Primero solo el pom: así las dependencias quedan en caché mientras no cambie
COPY pom.xml .
COPY src/main/resources/openapi ./src/main/resources/openapi
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
# Las pruebas ya corren en CI y en deploy.sh (mvn verify); aquí solo se empaqueta
RUN mvn -B -q -DskipTests package \
    && cp target/equipos-mineros-*.jar /workspace/app.jar

# ---------- Etapa 2: ejecutar con JRE 21 liviano, sin root ----------
FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474

# Tag de la imagen (SHA corto del commit); /health lo expone para validar los despliegues
ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION} \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app
EXPOSE 8080

# 503 o sin respuesta => wget sale con error => el contenedor pasa a "unhealthy"
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null "http://127.0.0.1:${PORT}/health" || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

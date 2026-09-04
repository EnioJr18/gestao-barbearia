FROM eclipse-temurin:21-jdk@sha256:85f00967bcc624fc19fa9c2cf124ea426a5363898e267141726f31f358c2e14b AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew && ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine@sha256:974b08960c5d96694c780e65b2d5705268ab1e1ca1a0dd0caf4ba6c3fe34d699

WORKDIR /app

RUN apk add --no-cache postgresql17-client \
    && addgroup -S app \
    && adduser -S app -G app \
    && mkdir -p /app/backups \
    && chown -R app:app /app

COPY --from=build --chown=app:app /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

USER app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM eclipse-temurin:17.0.20_8-jre-jammy

ARG MODULE_NAME
ARG APP_VERSION=1.0.0

RUN groupadd --system --gid 10001 mall \
    && useradd --system --uid 10001 --gid mall \
        --create-home --home-dir /home/mall \
        --shell /usr/sbin/nologin mall

WORKDIR /app

COPY --chown=mall:mall ${MODULE_NAME}/target/${MODULE_NAME}-${APP_VERSION}.jar /app/app.jar

USER mall:mall

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
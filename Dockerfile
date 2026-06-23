FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x ./gradlew && ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN groupadd --system bookmate \
    && useradd --system --gid bookmate --home-dir /app bookmate \
    && mkdir -p /app/uploads/profile-images \
    && chown -R bookmate:bookmate /app

COPY --from=build /workspace/build/libs/*.jar /app/bookmate-server.jar

USER bookmate

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/bookmate-server.jar"]

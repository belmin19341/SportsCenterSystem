# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn/
RUN sed -i 's/\r//' mvnw && chmod +x mvnw && ./mvnw dependency:go-offline -q
COPY src src/
RUN ./mvnw clean package -DskipTests -q && \
    java -Djarmode=layertools -jar target/*.jar extract --destination /extracted

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/* && groupadd -r spring && useradd -r -g spring spring
WORKDIR /app
COPY --from=builder --chown=spring:spring /extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /extracted/application/ ./
USER spring:spring
EXPOSE 8761
HEALTHCHECK --interval=20s --timeout=10s --retries=5 --start-period=60s \
  CMD wget -qO- http://localhost:8761/actuator/health || exit 1
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]

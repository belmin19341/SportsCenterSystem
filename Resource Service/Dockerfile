# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn/
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q
COPY src src/
RUN ./mvnw clean package -DskipTests -q && \
    java -Djarmode=layertools -jar target/*.jar extract --destination /extracted

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder --chown=spring:spring /extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /extracted/application/ ./
USER spring:spring
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=90s \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]

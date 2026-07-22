# ================================
# Multi-stage Dockerfile for JewelERP
# Optimized for production deployment
# ================================

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first (better layer caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Extract layers for better caching
RUN java -Djarmode=layertools -jar target/*.jar extract

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

# Headless Chromium + fonts for the branded invoice HTML→PDF render.
#  - chromium: the render engine (same as the desktop print)
#  - font-liberation: Arial-compatible sans for the body (Segoe UI fallback)
#  - font-noto / font-noto-emoji: broad coverage + the 📞 ✉ 🌐 📍 icon glyphs
#  - Playfair Display (headings) isn't packaged — download the OFL variable TTF
RUN apk add --no-cache \
        chromium \
        nss freetype harfbuzz fontconfig ca-certificates curl \
        font-liberation font-noto font-noto-emoji \
    && mkdir -p /usr/share/fonts/truetype/playfair \
    && curl -fsSL -o /usr/share/fonts/truetype/playfair/PlayfairDisplay.ttf \
        "https://raw.githubusercontent.com/google/fonts/main/ofl/playfairdisplay/PlayfairDisplay%5Bwght%5D.ttf" \
    && fc-cache -f
ENV PDF_CHROMIUM_BINARY=/usr/bin/chromium-browser

# Security: Run as non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy layers from builder (ordered by change frequency)
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

# Optional Dockerfile to run the notifier-pipe demo without a local Java installation.
# Build: docker build -t notifier-pipe-demo .
# Run:   docker run --rm notifier-pipe-demo

# -----------------------------------------------------------------------------
# Stage 1: Build the library with Maven
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Copy only POMs first for better layer caching
COPY pom.xml .
COPY notifier-pipe-core/pom.xml notifier-pipe-core/

# Resolve dependencies (offline cache for repeat builds)
RUN mvn -f notifier-pipe-core/pom.xml dependency:go-offline -B -q || true

# Copy source and build the JAR (skip tests for a quick demo image)
COPY notifier-pipe-core/src notifier-pipe-core/src
RUN mvn -f notifier-pipe-core/pom.xml package -DskipTests -B -q

# -----------------------------------------------------------------------------
# Stage 2: Run the example (small image with JRE only)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /build/notifier-pipe-core/target/notifier-pipe-core.jar app.jar

# Run the bundled example: configures Email/SMS/Push with named providers and sends one of each
ENTRYPOINT ["java", "-cp", "app.jar", "io.github.alexvargashn.notifierpipe.example.NotifierPipeConfigExample"]

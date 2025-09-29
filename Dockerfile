FROM gradle:9.1-jdk25 AS builder

COPY . /build/
WORKDIR /build
RUN gradle bootJar

FROM eclipse-temurin:21-jre

COPY --from=builder /build/build/libs/lexigeek-0.0.1.jar /app.jar
CMD ["java", "-jar", "/app.jar"]

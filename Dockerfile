# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/callapp_backend-0.0.1-SNAPSHOT.jar app.jar

# Configuration
ENV SERVER_PORT=8080
ENV DB_URL=jdbc:mysql://mysql:3306/eventos_institucionales
ENV DB_USER=root
ENV DB_PASS=password
ENV JWT_SECRET=change_me_in_production_12345678901234567890123456789012

# Heap acotado para no invadir la RAM de MySQL en un servidor compartido (línea base
# 8GB total: SO ~1GB, Nginx ~0.25GB, resto repartido mitad MySQL/mitad JVM). Ajustar
# -Xmx según la tabla de tuning por RAM del plan de migración si la VM tiene más RAM
# (16GB->6g, 32GB->10g) y coordinar con el mem_limit del servicio backend en
# docker-compose.yml.
ENV JAVA_OPTS="-Xms1g -Xmx2560m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

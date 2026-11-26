FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# Sin root: si alguien explotara un fallo del backend, no controla el contenedor
RUN useradd --system --no-create-home --shell /usr/sbin/nologin conecta
USER conecta
# Memoria contenida: el backend no necesita más de 384 MB en el piloto
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

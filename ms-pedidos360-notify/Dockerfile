# Etapa 1: Compilación y empaquetado
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copiar pom.xml y código fuente
COPY pom.xml .
COPY src ./src

# Compilar el artefacto JAR omitiendo pruebas
RUN mvn clean package -DskipTests

# Etapa 2: Imagen final de ejecución
# BASELINE-VULN [SEC-REQ-014]: contenedor ejecutandose como root sin instruccion USER no-root definida
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar el artefacto generado
COPY --from=builder /app/target/ms-pedidos360-notify-*.jar app.jar

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]

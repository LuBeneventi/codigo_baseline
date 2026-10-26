# SEC-CASE-05 — Configuración Segura
## Reporte SAST — Estado A (Baseline)

**Herramienta:** Semgrep 1.181.0 + Revisión manual de marcadores BASELINE-VULN  
**Fecha:** 2026-10-14  
**Rama/commit analizado:** Estado A (Baseline) — `D:\Escritorio\CloudComputing_Baseline`  
**Resultado esperado:** FAIL (vulnerabilidades intencionales de laboratorio)

---

## Resumen Ejecutivo

| ID Finding | SEC-REQ | Severidad | CWE | OWASP 2021 | Detectado por Semgrep | Estado |
|---|---|---|---|---|---|---|
| CASE05-F001 | SEC-REQ-014 | HIGH | CWE-250 | A05:2021 – Security Misconfiguration | ✅ Sí (`missing-user-entrypoint` / `missing-user`) | ❌ FAIL |
| CASE05-F002 | SEC-REQ-014 | MEDIUM | CWE-250 | A05:2021 – Security Misconfiguration | ✅ Sí (`no-new-privileges`, `writable-filesystem`) | ❌ FAIL |
| CASE05-F003 | SEC-REQ-012 | HIGH | CWE-798 / CWE-1188 | A07:2021 – Auth Failures / A05 | ❌ No | ❌ FAIL |
| CASE05-F004 | SEC-REQ-013 | MEDIUM | CWE-942 | A05:2021 – Security Misconfiguration | ❌ No | ❌ FAIL |
| CASE05-F005 | SEC-REQ-014 | HIGH | CWE-209 | A05:2021 – Security Misconfiguration | ❌ No | ❌ FAIL |
| CASE05-F006 | SEC-REQ-014 | MEDIUM | CWE-200 | A05:2021 – Security Misconfiguration | ❌ No | ❌ FAIL |
| CASE05-F007 | SEC-REQ-014 | LOW | CWE-200 | A05:2021 – Security Misconfiguration | ❌ No | ❌ FAIL |

**Total hallazgos:** 7  
**Hallazgos por herramienta automatizada (Semgrep):** 9 reglas activadas (5 contenedores root + 4 advertencias de docker-compose)  
**Hallazgos por revisión de código manual:** 5 requisitos adicionales  

---

## Hallazgos Detallados

---

### CASE05-F001 — Contenedores ejecutándose como `root` sin usuario dedicado ⚠️ DETECTADO POR SEMGREP

| Campo | Valor |
|---|---|
| **ID** | CASE05-F001 |
| **SEC-REQ** | SEC-REQ-014 |
| **Tipo** | Execution with Excessive Privileges in Containers |
| **CWE** | CWE-250: Execution with Unnecessary Privileges |
| **OWASP 2021** | A05: Security Misconfiguration |
| **Severidad** | HIGH |
| **Método de detección** | **Semgrep** (`dockerfile.security.missing-user-entrypoint` y `dockerfile.security.missing-user`) |
| **Archivos afectados** | `api_cloud/Dockerfile`, `frontend/Dockerfile`, `ms-pedidos360-catalog/Dockerfile`, `ms-pedidos360-notify/Dockerfile`, `ms-pedidos360-orders/Dockerfile` |

#### Evidencia — Detecciones Semgrep

1. **`api_cloud/Dockerfile:24`**:
   - Rule: `dockerfile.security.missing-user-entrypoint.missing-user-entrypoint`
   - Severity: ERROR
   - Message: *By not specifying a USER, a program in the container may run as 'root'. This is a security hazard if the container is compromised.*
2. **`frontend/Dockerfile:14`**:
   - Rule: `dockerfile.security.missing-user.missing-user`
   - Severity: ERROR
3. **`ms-pedidos360-catalog/Dockerfile:24`**:
   - Rule: `dockerfile.security.missing-user-entrypoint.missing-user-entrypoint`
   - Severity: ERROR
4. **`ms-pedidos360-notify/Dockerfile:21`**:
   - Rule: `dockerfile.security.missing-user-entrypoint.missing-user-entrypoint`
   - Severity: ERROR
5. **`ms-pedidos360-orders/Dockerfile:24`**:
   - Rule: `dockerfile.security.missing-user-entrypoint.missing-user-entrypoint`
   - Severity: ERROR

#### Evidencia — Código fuente (Ejemplo en `api_cloud/Dockerfile`)

```dockerfile
# BASELINE-VULN [SEC-REQ-014]: contenedor ejecutandose como root sin instruccion USER no-root definida
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
# Falta: USER appuser
```

#### Impacto

Si un atacante logra ejecución remota de código (RCE) o escape mediante vulnerabilidades en bibliotecas o runtime, tiene privilegios `root` completos dentro del contenedor.

---

### CASE05-F002 — Servicios de Docker Compose sin restricción de escalamiento ni sistema de archivos de solo lectura ⚠️ DETECTADO POR SEMGREP

| Campo | Valor |
|---|---|
| **ID** | CASE05-F002 |
| **SEC-REQ** | SEC-REQ-014 |
| **Tipo** | Container Hardening Deficiencies |
| **CWE** | CWE-250: Execution with Unnecessary Privileges |
| **OWASP 2021** | A05: Security Misconfiguration |
| **Severidad** | MEDIUM |
| **Método de detección** | **Semgrep** (`no-new-privileges`, `writable-filesystem-service`) |
| **Archivo** | `ms-pedidos360-notify/docker-compose.yml` (líneas 4 y 19) |

#### Evidencia — Detecciones Semgrep

- `yaml.docker-compose.security.no-new-privileges.no-new-privileges` (RabbitMQ y Mailpit)
- `yaml.docker-compose.security.writable-filesystem-service.writable-filesystem-service` (RabbitMQ y Mailpit)

#### Descripción del problema

Los contenedores se ejecutan sin `security_opt: [no-new-privileges:true]` y con sistemas de archivos raíz modificables (`read_only: false`), facilitando la persistencia de artefactos maliciosos si el servicio es comprometido.

---

### CASE05-F003 — Secretos y credenciales por defecto predecibles (Hardcoded / Insecure Defaults)

| Campo | Valor |
|---|---|
| **ID** | CASE05-F003 |
| **SEC-REQ** | SEC-REQ-012 |
| **Tipo** | Use of Hard-coded / Default Credentials |
| **CWE** | CWE-798: Use of Hard-coded Credentials / CWE-1188: Insecure Default Initialization |
| **OWASP 2021** | A07: Identification and Authentication Failures / A05: Security Misconfiguration |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcadores BASELINE-VULN |
| **Archivos** | `api_cloud/src/main/resources/application.yml`, `ms-pedidos360-catalog/src/main/resources/application.yml`, `ms-pedidos360-notify/src/main/resources/application.yml`, `ms-pedidos360-orders/src/main/resources/application.yml`, `docker-compose.yml` |

#### Evidencia — Código fuente

```yaml
# api_cloud/src/main/resources/application.yml:9
# BASELINE-VULN [SEC-REQ-012]: clave de base de datos por defecto predecible (postgres)
password: ${DB_PASSWORD:postgres}

# api_cloud/src/main/resources/application.yml:17
# BASELINE-VULN [SEC-REQ-012]: secreto JWT debil por defecto hardcodeado
security:
  jwt:
    secret: ${JWT_SECRET:changeme}

# ms-pedidos360-notify/src/main/resources/application.yml:8
# BASELINE-VULN [SEC-REQ-012]: credenciales por defecto de RabbitMQ (guest/guest)
username: ${RABBITMQ_USER:guest}
password: ${RABBITMQ_PASS:guest}

# docker-compose.yml:102
# BASELINE-VULN [SEC-REQ-012]: secreto JWT por defecto debil (changeme)
- JWT_SECRET=changeme
```

#### Impacto

- Cualquier atacante conoce las contraseñas por defecto (`postgres`, `guest`, `changeme`).
- Con el secreto JWT `changeme`, un atacante puede firmar sus propios tokens con rol `ADMIN` usando algoritmo HS256 sin necesidad de interactuar con el endpoint de login.

---

### CASE05-F004 — CORS permisivo con comodín (*) y ausencia de Content-Security-Policy (CSP)

| Campo | Valor |
|---|---|
| **ID** | CASE05-F004 |
| **SEC-REQ** | SEC-REQ-013 |
| **Tipo** | Overly Permissive CORS Policy / Missing Security Headers |
| **CWE** | CWE-942: Permissive Cross-Domain Policy with Untrusted Domains |
| **OWASP 2021** | A05: Security Misconfiguration |
| **Severidad** | MEDIUM |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java` |
| **Línea** | 72 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-013]: configuracion CORS permisiva con comodin wildcard (*) y ausencia de Content-Security-Policy
CorsConfiguration configuration = new CorsConfiguration();
configuration.setAllowedOriginPatterns(List.of("*"));
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("*"));
configuration.setAllowCredentials(false);
```

#### Descripción del problema

- Se permite cualquier origen cruzado (`*`) a los recursos de la API.
- No se definen encabezados de seguridad modernos en el `SecurityFilterChain`:
  - Falta `Content-Security-Policy` (CSP).
  - Falta `X-Content-Type-Options: nosniff`.
  - Falta `X-Frame-Options: DENY`.
  - Falta `Strict-Transport-Security` (HSTS).

---

### CASE05-F005 — Fuga de información por detalle de excepciones internas (`ex.getMessage()`)

| Campo | Valor |
|---|---|
| **ID** | CASE05-F005 |
| **SEC-REQ** | SEC-REQ-014 |
| **Tipo** | Information Exposure Through an Error Message |
| **CWE** | CWE-209: Generation of Error Message Containing Sensitive Information |
| **OWASP 2021** | A05: Security Misconfiguration |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcadores BASELINE-VULN |
| **Archivos** | `api_cloud/src/main/java/cl/duoc/api/OrdersController.java:294`, `ms-pedidos360-orders/src/main/java/com/pedidos360/orders/controller/exception/GlobalExceptionHandler.java:71` |

#### Evidencia — Código fuente

```java
// api_cloud/OrdersController.java:294
// BASELINE-VULN [SEC-REQ-014]: controlador devuelve e.getMessage() revelando detalles de excepcion interna
return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));

// ms-pedidos360-orders/GlobalExceptionHandler.java:71
// BASELINE-VULN [SEC-REQ-014]: manejador global devuelve ex.getMessage() revelando detalles de excepcion interna
body.put("message", ex.getMessage());
```

#### Impacto

Los mensajes crudos de excepciones SQL, JPA o de red pueden exponer nombres de tablas, sintaxis de consultas PostgreSQL, nombres de clases Java y rutas internas a clientes no autorizados.

---

### CASE05-F006 — Puertos de infraestructura y microservicios internos expuestos externamente

| Campo | Valor |
|---|---|
| **ID** | CASE05-F006 |
| **SEC-REQ** | SEC-REQ-014 |
| **Tipo** | Exposure of Internal Services and Databases |
| **CWE** | CWE-200: Exposure of Sensitive Information |
| **OWASP 2021** | A05: Security Misconfiguration |
| **Severidad** | MEDIUM |
| **Método de detección** | Revisión manual — marcadores BASELINE-VULN |
| **Archivo** | `docker-compose.yml` (líneas 13, 38, 142, 163) |

#### Evidencia — Configuración

```yaml
# docker-compose.yml:142
# BASELINE-VULN [SEC-REQ-014]: puerto de base de datos expuesto externamente
ports:
  - "127.0.0.1:5432:5432"

# docker-compose.yml:163
# BASELINE-VULN [SEC-REQ-014]: puerto del broker RabbitMQ expuesto externamente
ports:
  - "127.0.0.1:5672:5672"
  - "127.0.0.1:15672:15672"

# docker-compose.yml:13 & 38
# BASELINE-VULN [SEC-REQ-014]: puerto interno expuesto directamente
ports:
  - "127.0.0.1:8086:8086" # ms-catalog
  - "127.0.0.1:8087:8087" # ms-orders
```

#### Descripción del problema

En una arquitectura BFF, los microservicios de backend (`ms-catalog`, `ms-orders`) y los componentes de datos (`PostgreSQL`, `RabbitMQ`) deben comunicarse exclusivamente dentro de la red interna de Docker (`pedidos360-network`). Exponer sus puertos permite saltarse el BFF y atacar directamente los servicios desprotegidos.

---

### CASE05-F007 — Swagger-UI y Actuator Health Details expuestos sin restricción

| Campo | Valor |
|---|---|
| **ID** | CASE05-F007 |
| **SEC-REQ** | SEC-REQ-014 |
| **Tipo** | Unrestricted Access to Diagnostic & API Documentation Endpoints |
| **CWE** | CWE-200: Exposure of Sensitive Information |
| **OWASP 2021** | A05: Security Misconfiguration |
| **Severidad** | LOW |
| **Método de detección** | Revisión manual — marcadores BASELINE-VULN |
| **Archivos** | `ms-pedidos360-catalog/src/main/resources/application.yml:29`, `ms-pedidos360-orders/src/main/resources/application.yml:37`, `ms-pedidos360-notify/src/main/resources/application.yml:49` |

#### Evidencia — Configuración

```yaml
# ms-pedidos360-catalog & orders:
# BASELINE-VULN [SEC-REQ-014]: swagger-ui habilitado en entorno productivo/general
springdoc:
  swagger-ui:
    enabled: true

# ms-pedidos360-notify:
# BASELINE-VULN [SEC-REQ-014]: health show-details expuesto siempre (fuga de informacion de infraestructura)
management:
  endpoint:
    health:
      show-details: always
```

#### Impacto

Facilita el reconocimiento a un atacante, revelando endpoints disponibles, parámetros y detalles de estado de salud y conectividad de servicios internos (disco, BD, RabbitMQ).

---

## Trazabilidad

| Marcador en código | Archivo:línea | Finding |
|---|---|---|
| Semgrep `missing-user-entrypoint` / `missing-user` | `Dockerfile` (x5) | CASE05-F001 |
| Semgrep `no-new-privileges` / `writable-filesystem` | `notify/docker-compose.yml:4,19` | CASE05-F002 |
| `BASELINE-VULN [SEC-REQ-012]` | `application.yml` (x4), `docker-compose.yml` | CASE05-F003 |
| `BASELINE-VULN [SEC-REQ-013]` | `SecurityConfig.java:72` | CASE05-F004 |
| `BASELINE-VULN [SEC-REQ-014]` (excepciones) | `OrdersController.java:294`, `GlobalExceptionHandler.java:71` | CASE05-F005 |
| `BASELINE-VULN [SEC-REQ-014]` (puertos) | `docker-compose.yml:13,38,142,163` | CASE05-F006 |
| `BASELINE-VULN [SEC-REQ-014]` (swagger/actuator) | `catalog`, `orders`, `notify` `application.yml` | CASE05-F007 |

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa SAST.*  
*Proyecto: Pedidos360 Coffee Store — Ejercicio académico OCY1102.*

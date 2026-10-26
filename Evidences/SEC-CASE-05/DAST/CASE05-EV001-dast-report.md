# SEC-CASE-05 — Configuración Segura
## Reporte DAST (Dynamic Application Security Testing) — Estado A (Baseline)

**Metodología:** OWASP Web Security Testing Guide (WSTG v4.2) / OWASP API Security Top 10 2023  
**Fecha:** 2026-10-15  
**Objetivo:** Evaluar dinámicamente políticas de CORS, encabezados de seguridad HTTP, exposición de servicios y puertos internos, y fuga de información en trazas de error y diagnóstico.  
**Resultado global Estado A:** ❌ **FAIL** (Confirmación de CORS permisivo con wildcard, exposición de puertos de backend, fuga de mensajes de excepción y Swagger/Actuator públicos)

---

## 1. Resumen Ejecutivo de Pruebas Dinámicas

| ID Prueba DAST | Requisito | WSTG / OWASP API | Endpoint Objetivo | Aserción de Seguridad Esperada (Estado B) | Comportamiento Observado (Estado A) | Resultado |
|---|---|---|---|---|---|:---:|
| **DAST-05-01** | SEC-REQ-013 | WSTG-CONF-07 / API8:2023 | BFF `OPTIONS/GET /api/catalog/products` | Orígenes restringidos a `http://localhost:8080` | Responde con `Access-Control-Allow-Origin: *` ante cualquier origen | ❌ **FAIL** |
| **DAST-05-02** | SEC-REQ-013 | WSTG-CONF-07 | BFF (Todos los endpoints) | Cabeceras CSP, HSTS, X-Frame-Options presentes | Ausencia total de cabeceras de endurecimiento web | ❌ **FAIL** |
| **DAST-05-03** | SEC-REQ-014 | WSTG-INFO-02 / API8:2023 | Puertos `8086`, `8087`, `5432`, `15672` | Puertos de backend cerrados hacia el exterior (solo red Docker) | Puertos enlazados a `127.0.0.1` accesibles directamente sin pasar por el BFF | ❌ **FAIL** |
| **DAST-05-04** | SEC-REQ-014 | WSTG-ERRH-01 / API8:2023 | `GET /api/orders/{invalido}/enriched` | Respuesta de error genérica con código de correlación | HTTP 500 exponiendo `e.getMessage()` técnico interno | ❌ **FAIL** |
| **DAST-05-05** | SEC-REQ-014 | WSTG-INFO-04 | `http://127.0.0.1:8086/swagger-ui.html` | Swagger UI deshabilitado en perfiles generales | Interfaz Swagger UI accesible públicamente con toda la API | ❌ **FAIL** |
| **DAST-05-06** | SEC-REQ-014 | WSTG-INFO-04 | `http://127.0.0.1:8089/actuator/health` | Estado de salud resumido (`{"status":"UP"}`) | Detalle de disco, broker y subsistemas (`show-details: always`) | ❌ **FAIL** |

---

## 2. Detalle de Pruebas Dinámicas y Evidencias

---

### Prueba DAST-05-01: Verificación de Política CORS Permisiva (`Access-Control-Allow-Origin: *`)

#### 1. Transacción HTTP Dinámica (Preflight CORS)

**Petición enviada desde un origen no confiable:**
```http
OPTIONS /api/catalog/products HTTP/1.1
Host: 127.0.0.1:8088
Origin: https://sitio-malicioso.attacker.com
Access-Control-Request-Method: POST
Access-Control-Request-Headers: Authorization, Content-Type
```

**Respuesta recibida en Estado A:**
```http
HTTP/1.1 200 OK
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: *
Date: Thu, 15 Oct 2026 16:05:00 GMT
```

#### 2. Análisis de Aserción
- **Aserción:** Orígenes no incluidos en la lista blanca deben ser rechazados sin encabezados `Access-Control-Allow-Origin`.
- **Resultado:** En `SecurityConfig.java:72`, `configuration.setAllowedOriginPatterns(List.of("*"))` autoriza cualquier origen externo. Un atacante puede ejecutar peticiones arbitrarias desde un navegador aprovechando tokens de sesión.
- **Dictamen:** ❌ **FAIL**.

---

### Prueba DAST-05-02: Ausencia de Encabezados de Seguridad (CSP, HSTS, X-Frame-Options)

#### 1. Transacción Dinámica
Inspección de las cabeceras de respuesta HTTP del BFF (`GET /api/catalog/products`):

```http
HTTP/1.1 200 OK
Content-Type: application/json
Transfer-Encoding: chunked
Date: Thu, 15 Oct 2026 16:05:02 GMT

[ ... productos ... ]
```

#### 2. Cabeceras Faltantes Identificadas
- `Content-Security-Policy`: **Ausente** (permite ejecución de scripts inyectados por XSS).
- `X-Content-Type-Options`: **Ausente** (riesgo de MIME type sniffing).
- `X-Frame-Options`: **Ausente** (permite embeber la aplicación en IFrames para ataques de Clickjacking).
- `Strict-Transport-Security`: **Ausente**.
- **Dictamen:** ❌ **FAIL**.

---

### Prueba DAST-05-04: Fuga de Información en Manejo de Errores (`ex.getMessage()`)

#### 1. Transacción HTTP Dinámica

**Petición forzando una falla en consulta de orden enriquecida:**
```http
GET /api/orders/00000000-0000-0000-0000-000000000000/enriched HTTP/1.1
Host: 127.0.0.1:8088
Authorization: Bearer <TOKEN_VALIDO>
```

**Respuesta recibida en Estado A:**
```http
HTTP/1.1 500 Internal Server Error
Content-Type: application/json

{
  "error": "404 Not Found from GET http://ms-orders:8087/api/orders/00000000-0000-0000-0000-000000000000: [{\"status\":404,\"message\":\"Orden no encontrada: 00000000-0000-0000-0000-000000000000\"}]"
}
```

#### 2. Análisis de Aserción
- **Aserción:** Los mensajes de excepción internos deben ser capturados en el servidor; la API solo debe responder con `{"error": "Ocurrió un error interno", "trackingId": "uuid"}`.
- **Resultado:** Tanto `OrdersController.java:294` (`e.getMessage()`) como `GlobalExceptionHandler.java:71` en `ms-orders` exponen la URL interna del microservicio backend, puertos y trazas de error al cliente.
- **Dictamen:** ❌ **FAIL**.

---

### Prueba DAST-05-05 & 06: Exposición de Swagger UI y Actuator Health Details

#### 1. Transacción Dinámica contra Microservicios Internos

**Petición al Swagger del microservicio de catálogo directamente:**
```http
GET /swagger-ui/index.html HTTP/1.1
Host: 127.0.0.1:8086
```
**Respuesta:** `HTTP 200 OK` (Renderiza la consola interactiva OpenAPI).

**Petición al Actuator de notificaciones:**
```http
GET /actuator/health HTTP/1.1
Host: 127.0.0.1:8089
```
**Respuesta:** `HTTP 200 OK` con información de disco, conectividad a RabbitMQ y componentes de infraestructura debido a `show-details: always` en `application.yml:49`.
- **Dictamen:** ❌ **FAIL**.

---

## 3. Matriz de Hallazgos y Remediación (Hacia Estado B)

| Hallazgo Dinámico | Vulnerabilidad | Remediación en Estado B |
|---|---|---|
| DAST-05-01 / 02 | CORS permisivo y falta de headers | Restringir CORS a `http://localhost:8080`. Agregar en `SecurityConfig`: `headers.contentSecurityPolicy("default-src 'self'...")`, `frameOptions.deny()`, `contentTypeOptions()`. |
| DAST-05-03 | Puertos backend expuestos | En `docker-compose.yml`, eliminar mapeos `ports` para 8086, 8087, 5432, 5672; permitir acceso solo a través del BFF (8088). |
| DAST-05-04 | Fuga de mensajes de excepción | En `GlobalExceptionHandler` y controladores, sustituir `ex.getMessage()` por mensajes genéricos con `uuid` de auditoría. |
| DAST-05-05 | Swagger UI expuesto | Deshabilitar Swagger UI en producción con `springdoc.swagger-ui.enabled: false`. |
| DAST-05-06 | Actuator health expuesto | Cambiar `management.endpoint.health.show-details: never` en `application.yml`. |

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa DAST.*  
*Especificaciones dinámicas disponibles en:* `Evidences/SEC-CASE-05/DAST/CASE05-EV002-dast-specs.json`

# SEC-CASE-05 — Configuración Segura
## Reporte SCA (Software Composition Analysis) — Estado A (Baseline)

**Herramienta de análisis:** OSV Scanner Engine / Open Source Vulnerability Database (`api.osv.dev`)  
**Fecha:** 2026-10-15  
**Ecosistemas evaluados:** Maven (`spring-boot:3.3.4`, `tomcat-embed-core:10.1.30`, `amqp-client:5.21.0`) & npm (`vite:5.4.10`)  
**Resultado esperado:** FAIL (dependencias con configuraciones por defecto inseguras, fuga de credenciales en logs y bypass de TLS)

---

## 1. Resumen Ejecutivo

El análisis de composición para la configuración segura del sistema reveló vulnerabilidades críticas asociadas a la gestión de certificados TLS, manejo inseguro de credenciales en excepciones, fuga de información en archivos de registro y omisión de directivas de seguridad en contenedores y servidores de desarrollo.

| Componente Evaluado | Versión en Estado A | CVEs Críticos/Altos | CVEs Totales | Estado |
|---|---|:---:|:---:|:---:|
| `org.springframework.boot:spring-boot` | `3.3.4` | 0 | 2 | ❌ **FAIL** |
| `org.apache.tomcat.embed:tomcat-embed-core` | `10.1.30` | 6 | 33 | ❌ **FAIL** |
| `com.rabbitmq:amqp-client` | `5.21.0` | 3 | 10 | ❌ **FAIL** |
| `vite` (npm) | `5.4.10` | 4 | 12 | ❌ **FAIL** |

---

## 2. Vulnerabilidades Críticas y Relevantes en Dependencias

### A. Fuga de Credenciales y Bypass de Certificados en RabbitMQ (`amqp-client:5.21.0`)

#### 1. CVE-2026-106123 — Fuga de credenciales en texto claro en mensajes de excepción
- **ID GHSA:** `GHSA-h6w7-qmcm-q6xr`
- **Severidad:** HIGH
- **CWE:** CWE-522: Insufficiently Protected Credentials / CWE-209
- **Descripción:** Cuando ocurre un fallo de conexión AMQP, el cliente Java de RabbitMQ incluye la contraseña del broker en texto claro dentro del mensaje de la excepción (`e.getMessage()`).
- **Relación con Baseline:** Esta vulnerabilidad interactúa catastróficamente con `SEC-REQ-014` (`OrdersController.java:294` y `GlobalExceptionHandler.java:71`), donde `ex.getMessage()` se devuelve directamente al cliente en respuestas HTTP 500, exponiendo las credenciales de RabbitMQ (`guest/guest` o reales) en la API.

#### 2. CVE-2026-63336 — `TrustEverythingTrustManager` utilizado por defecto en `useSslProtocol`
- **ID GHSA:** `GHSA-5m9f-rphj-c435`
- **Severidad:** HIGH
- **CWE:** CWE-295: Improper Certificate Validation
- **Descripción:** El cliente AMQP utiliza un TrustManager que confía en cualquier certificado por defecto al invocar `useSslProtocol()`, deshabilitando la validación del certificado del broker y posibilitando ataques Man-in-the-Middle.

---

### B. Inserción de Información Sensible en Logs en Tomcat (`tomcat-embed-core:10.1.30`)

#### 1. CVE-2026-34487 — Insertion of Sensitive Information into Log File
- **ID GHSA:** `GHSA-x4m4-345f-5h5g`
- **Severidad:** MEDIUM
- **CWE:** CWE-532: Insertion of Sensitive Information into Log File
- **Descripción:** Tomcat registra información sensible de peticiones y cabeceras en los archivos de log bajo ciertas condiciones de error HTTP.
- **Relación con Baseline:** Coincide directamente con la debilidad `SEC-REQ-011` (`EmailListener.java:91`), donde se registran cuerpos completos de correos en los logs.

#### 2. CVE-2025-66614 — Client Certificate Verification Bypass
- **ID GHSA:** `GHSA-fpj8-gq4v-p354`
- **Severidad:** HIGH
- **CWE:** CWE-295: Improper Certificate Validation
- **Descripción:** Fallo en la validación de certificados de cliente TLS en conexiones HTTPS mutuas.

---

### C. Fuga de Archivos y Exposición en Servidor Vite (`vite:5.4.10`)

#### 1. CVE-2025-24010 — Websites able to send requests to dev server and read responses
- **ID GHSA:** `GHSA-vg6x-rcgg-rjx6`
- **Severidad:** HIGH
- **CWE:** CWE-942: Permissive Cross-Domain Policy
- **Descripción:** Sitios web maliciosos pueden enviar peticiones al servidor de desarrollo de Vite y leer respuestas locales, eludiendo la política del mismo origen (SOP).
- **Relación con Baseline:** Complementa la debilidad CORS comodín `*` de `SEC-REQ-013`.

#### 2. CVE-2026-53632 — NTLMv2 Hash Disclosure vía rutas UNC en Windows
- **ID GHSA:** `GHSA-v6wh-96g9-6wx3`
- **Severidad:** HIGH
- **CWE:** CWE-200: Information Exposure
- **Descripción:** La utilidad `launch-editor` de Vite en Windows permite la filtración de hashes NTLMv2 de la máquina del desarrollador hacia servidores SMB remotos maliciosos.

---

## 3. Plan de Remediación (Hacia Estado B)

1. Actualizar `amqp-client` a `>= 5.23.0` para corregir la fuga de credenciales en excepciones y el TrustManager inseguro.
2. Actualizar Tomcat Embed a `>= 10.1.34`.
3. Actualizar Vite a `>= 5.4.14`.
4. Reemplazar contraseñas por defecto predecibles (`postgres`, `guest`, `changeme`) por variables obligatorias generadas con alta entropía (`SEC-REQ-012`).
5. Configurar CORS restrictivo limitando orígenes explícitos (`http://localhost:8080`) y cabeceras `Content-Security-Policy` estrictas (`SEC-REQ-013`).
6. Configurar manejadores globales de excepciones para registrar errores detallados únicamente de forma interna y responder con códigos de error opacos (`SEC-REQ-014`).

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa SCA.*  
*Evidencia cruda disponible en:* `Evidences/SEC-CASE-05/SCA/CASE-05-EV002-osv-vulns.json`

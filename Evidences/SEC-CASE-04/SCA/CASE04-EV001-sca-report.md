# SEC-CASE-04 — Validación y Sanitización de Entradas
## Reporte SCA (Software Composition Analysis) — Estado A (Baseline)

**Herramienta de análisis:** OSV Scanner Engine / Open Source Vulnerability Database (`api.osv.dev`)  
**Fecha:** 2026-10-15  
**Ecosistemas evaluados:** Maven (`postgresql:42.7.4`, `tomcat-embed-core:10.1.30`) & npm (`vite:5.4.10`)  
**Resultado esperado:** FAIL (vulnerabilidades de inyección, path traversal y elusión de escape en dependencias)

---

## 1. Resumen Ejecutivo

El análisis de composición para los mecanismos de validación y sanitización de entradas reveló debilidades críticas en el servidor web embebido (Tomcat), el controlador JDBC de base de datos (PostgreSQL Driver) y el servidor de desarrollo/frontend (Vite), que permiten inyección de rutas, evasión de filtros y manipulación de parámetros no sanitizados.

| Componente Evaluado | Versión en Estado A | CVEs Críticos/Altos | CVEs Totales | Estado |
|---|---|:---:|:---:|:---:|
| `org.postgresql:postgresql` | `42.7.4` | 1 | 2 | ❌ **FAIL** |
| `org.apache.tomcat.embed:tomcat-embed-core` | `10.1.30` | 6 | 33 | ❌ **FAIL** |
| `vite` (npm) | `5.4.10` | 4 | 12 | ❌ **FAIL** |

---

## 2. Vulnerabilidades Críticas y Relevantes en Dependencias

### A. Fallas de Validación y Escape en Servidor Embebido (`tomcat-embed-core:10.1.30`)

#### 1. CVE-2026-24734 — Apache Tomcat: Improper Input Validation
- **ID GHSA:** `GHSA-mgp5-rv84-w37q`
- **Severidad:** HIGH
- **CWE:** CWE-20: Improper Input Validation
- **Descripción:** La capa de recepción de peticiones HTTP en Tomcat no valida adecuadamente caracteres de control en encabezados y cuerpos de solicitud, permitiendo que cargas no sanitizadas pasen hacia los filtros de la aplicación.

#### 2. CVE-2025-55754 — Improper Neutralization of Escape, Meta, or Control Sequences
- **ID GHSA:** `GHSA-vfww-5hm6-hx2j`
- **Severidad:** HIGH
- **CWE:** CWE-116: Improper Encoding or Escaping of Output
- **Descripción:** Fallo en la neutralización de secuencias de escape al manejar respuestas dinámicas de error y páginas de diagnóstico, permitiendo inyección de código de control.

#### 3. CVE-2025-55752 — Relative Path Traversal
- **ID GHSA:** `GHSA-wmwf-9ccg-fff5`
- **Severidad:** HIGH
- **CWE:** CWE-23: Relative Path Traversal
- **Descripción:** Procesamiento inadecuado de secuencias relativas (`../`) al resolver recursos estáticos en contextos de servlet.

#### 4. CVE-2026-24880 — HTTP Request/Response Smuggling
- **ID GHSA:** `GHSA-563x-q5rq-57qp`
- **Severidad:** HIGH
- **CWE:** CWE-444: Inconsistent Interpretation of HTTP Requests ('HTTP Request/Response Smuggling')
- **Descripción:** Desincronización de peticiones entre el proxy inverso/BFF y el servidor embebido backend debido a diferencias de delimitación de cabeceras `Content-Length` y `Transfer-Encoding`.

---

### B. Fallas de Autenticación y Conexión en Driver PostgreSQL (`postgresql:42.7.4`)

#### 1. CVE-2025-49146 / CVE-2026-54291 — pgjdbc Insecure Authentication Downgrade
- **ID GHSA:** `GHSA-hq9p-pm7w-8p54` / `GHSA-j92g-9f8w-j867`
- **Severidad:** MEDIUM / HIGH
- **CWE:** CWE-306 / CWE-327
- **Descripción:** El driver PostgreSQL permite silenciosamente un downgrade de autenticación de SCRAM-SHA-256 a mecanismos inseguros sin channel-binding, exponiendo al cliente a ataques Man-in-the-Middle (MitM) en la red de la base de datos.
- **Relación con Baseline:** Se combina con la consulta SQL concatenada no parametrizada (`SEC-REQ-010`) en `ProductController.java:38`.

---

### C. Vulnerabilidades de Path Traversal y Evasión en Vite (`vite:5.4.10`)

#### 1. CVE-2026-39365 — Path Traversal in Optimized Deps `.map` Handling
- **ID GHSA:** `GHSA-4w7w-66w2-5vf9`
- **Severidad:** HIGH
- **CWE:** CWE-22: Improper Limitation of a Pathname to a Restricted Directory
- **Descripción:** Permite a un atacante leer archivos arbitrarios del sistema de archivos host a través del endpoint de source maps de dependencias optimizadas.

#### 2. CVE-2025-62522 / CVE-2026-53571 — `server.fs.deny` Bypass on Windows
- **ID GHSA:** `GHSA-93m4-6634-74q7` / `GHSA-fx2h-pf6j-xcff`
- **Severidad:** HIGH
- **CWE:** CWE-22: Path Traversal
- **Descripción:** En sistemas Windows, las barras invertidas (`\`) y rutas alternativas permiten evadir las directivas de restricción de lectura de archivos (`server.fs.deny`) de Vite, exponiendo archivos del proyecto (`.env`, código fuente).

---

## 3. Plan de Remediación (Hacia Estado B)

1. Actualizar Tomcat Embed a `>= 10.1.34`.
2. Actualizar PostgreSQL Driver a `>= 42.7.5`.
3. Actualizar Vite a `>= 5.4.14` o rama `6.x`.
4. Utilizar `PreparedStatement` o Spring Data JPA con parámetros nombrados en `ProductController.java` para erradicar la inyección SQL concatenada.
5. Reemplazar `dangerouslySetInnerHTML` en `frontend/src/App.tsx` por renderizado seguro nativo de React (`{product.name}`).

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa SCA.*  
*Evidencia cruda disponible en:* `Evidences/SEC-CASE-04/SCA/CASE-04-EV002-osv-vulns.json`

# SEC-CASE-01 — Autenticación y Gestión de Sesiones
## Reporte SCA (Software Composition Analysis) — Estado A (Baseline)

**Herramienta de análisis:** OSV Scanner Engine / Open Source Vulnerability Database (`api.osv.dev`)  
**Fecha:** 2026-10-15  
**Ecosistemas evaluados:** Maven (Java 17, Spring Boot 3.3.4 / 4.1.1) & npm (Vite 5.4.10)  
**Resultado esperado:** FAIL (dependencias desactualizadas con vulnerabilidades conocidas de autenticación/sesiones)

---

## 1. Resumen Ejecutivo

En el análisis de composición de software para los componentes de autenticación y gestión de sesiones se identificaron dependencias en uso que presentan vulnerabilidades públicas conocidas (CVE / GHSA), afectando directamente los mecanismos de control de sesiones y autenticación del contenedor embebido y del framework de seguridad.

| Componente Evaluado | Versión en Estado A | CVEs Críticos/Altos | CVEs Totales | Estado |
|---|---|:---:|:---:|:---:|
| `org.springframework.security:spring-security-core` | `6.3.3` | 1 | 4 | ❌ **FAIL** |
| `org.apache.tomcat.embed:tomcat-embed-core` | `10.1.30` | 6 | 33 | ❌ **FAIL** |

---

## 2. Vulnerabilidades Críticas y Relevantes en Dependencias

### A. Fijación de Sesión y Bypass de Autenticación en Servidor Embebido (Tomcat 10.1.30)

#### 1. CVE-2025-55668 — Apache Tomcat Session Fixation
- **ID GHSA:** `GHSA-23hv-mwm6-g8jf`
- **Severidad:** HIGH
- **CWE:** CWE-384: Session Fixation
- **Descripción:** Fallo en la invalidación y reasignación de identificadores de sesión durante transiciones de estado de autenticación en Apache Tomcat. Un atacante puede fijar el identificador de sesión antes de que el usuario inicie sesión y secuestrar la sesión autenticada.
- **Relación con Baseline:** Agrava el problema de sesiones de larga duración (30 días en `SEC-REQ-003`).

#### 2. CVE-2026-65905 — Authentication Bypass by Capture-replay en Authenticator
- **ID GHSA:** `GHSA-9xv2-5v5q-p794`
- **Severidad:** HIGH
- **CWE:** CWE-294: Authentication Bypass by Capture-replay
- **Descripción:** El mecanismo de autenticación digest/básico en Tomcat permite reutilización de parámetros de autenticación y captura de credenciales por omisión de nonces temporales.

#### 3. CVE-2026-43513 — LockOutRealm trata nombres de usuario de forma sensible a mayúsculas
- **ID GHSA:** `GHSA-5mp6-jrq3-r938`
- **Severidad:** MEDIUM
- **CWE:** CWE-307: Improper Restriction of Excessive Authentication Attempts
- **Descripción:** La protección contra fuerza bruta de Tomcat (`LockOutRealm`) es sensible a mayúsculas/minúsculas, permitiendo eludir los límites de intentos fallidos variando la capitalización del usuario.
- **Relación con Baseline:** Se alinea directamente con la ausencia de bloqueo por fuerza bruta de `SEC-REQ-001`.

#### 4. CVE-2026-42498 — Exposición de encabezados de autenticación en WebSockets
- **ID GHSA:** `GHSA-fv25-8xcx-gqjc`
- **Severidad:** HIGH
- **CWE:** CWE-522: Insufficiently Protected Credentials
- **Descripción:** Exposición de encabezados HTTP con tokens y credenciales durante el upgrade de conexión WebSocket.

---

### B. Vulnerabilidades en Spring Security (`spring-security-core:6.3.3`)

- **Bypass de reglas de autenticación por normalización de rutas:** Discrepancias entre el mapeo de servlets de Tomcat y los matchers de Spring Security permiten acceder a endpoints protegidos sin autenticación bajo ciertas secuencias URI codificadas.

---

## 3. Plan de Remediación (Hacia Estado B)

1. **Actualizar Spring Boot** a la última versión de mantenimiento (>= 3.3.6 o 3.4.x).
2. **Actualizar Tomcat Embed** a versión >= `10.1.34` donde se corrigen `CVE-2025-55668` y `CVE-2026-65905`.
3. **Actualizar Spring Security** a versión >= `6.3.5`.
4. Implementar rotación de tokens y reducción de tiempo de vida del JWT a 15 minutos con refresh token.

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa SCA.*  
*Evidencia cruda disponible en:* `Evidences/SEC-CASE-01/SCA/CASE-01-EV002-osv-vulns.json`

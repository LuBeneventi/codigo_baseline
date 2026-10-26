# SEC-CASE-02 — Autorización y Control de Acceso
## Reporte SCA (Software Composition Analysis) — Estado A (Baseline)

**Herramienta de análisis:** OSV Scanner Engine / Open Source Vulnerability Database (`api.osv.dev`)  
**Fecha:** 2026-10-15  
**Ecosistemas evaluados:** Maven (Spring Security 6.3.3, Tomcat 10.1.30)  
**Resultado esperado:** FAIL (dependencias con vulnerabilidades de elusión de restricciones de acceso)

---

## 1. Resumen Ejecutivo

El análisis de composición de software para los módulos de autorización y control de acceso identificó fallos de seguridad en las dependencias encargadas de evaluar y forzar las restricciones de seguridad (Security Constraints) tanto a nivel de contenedor de servlets como en el motor de filtros de seguridad.

| Componente Evaluado | Versión en Estado A | CVEs Críticos/Altos | CVEs Totales | Estado |
|---|---|:---:|:---:|:---:|
| `org.springframework.security:spring-security-core` | `6.3.3` | 1 | 4 | ❌ **FAIL** |
| `org.apache.tomcat.embed:tomcat-embed-core` | `10.1.30` | 6 | 33 | ❌ **FAIL** |

---

## 2. Vulnerabilidades Críticas y Relevantes en Dependencias

### A. Evasión de Restricciones de Seguridad en Tomcat (`tomcat-embed-core:10.1.30`)

#### 1. CVE-2026-43515 — Apache Tomcat: Security constraints not correctly applied
- **ID GHSA:** `GHSA-5m62-pw8w-7w9f`
- **Severidad:** HIGH
- **CWE:** CWE-284: Improper Access Control
- **Descripción:** Las restricciones de seguridad configuradas en la aplicación no son aplicadas adecuadamente cuando las peticiones contienen combinaciones de patrones de ruta con caracteres comodín o codificaciones específicas.
- **Relación con Baseline:** Complementa las fallas de autorización como el bypass de rol basado en cabeceras (`SEC-REQ-005`).

#### 2. CVE-2026-65182 — Improper Access Control / Incorrect Authorization
- **ID GHSA:** `GHSA-gcx9-497g-6cp6`
- **Severidad:** HIGH
- **CWE:** CWE-285: Improper Authorization
- **Descripción:** Fallo de autorización en la validación de permisos de usuarios authenticated vs unauthenticated, permitiendo a usuarios de bajo privilegio eludir controles de recursos protegidos.
- **Relación con Baseline:** Se relaciona con la ausencia de verificación de propietario en órdenes (`SEC-REQ-004`).

#### 3. CVE-2025-46701 — CGI Security Constraint Bypass
- **ID GHSA:** `GHSA-h2fw-rfh5-95r3`
- **Severidad:** MEDIUM
- **CWE:** CWE-863: Incorrect Authorization
- **Descripción:** Evasión de validaciones de seguridad de recursos estáticos y endpoints de gestión.

#### 4. CVE-2025-49125 — Security constraint bypass for pre/post-resources
- **ID GHSA:** `GHSA-wc4r-xq3c-5cf3`
- **Severidad:** MEDIUM
- **CWE:** CWE-863: Incorrect Authorization
- **Descripción:** Recursos adicionales vinculados a través de wrappers no heredan las políticas de autorización del recurso principal.

---

### B. Vulnerabilidades en Spring Security (`spring-security-core:6.3.3`)

- **Bypass de reglas HttpSecurity `hasRole` / `hasAuthority`:** En configuraciones que usan matchers incompletos o expresiones de cabeceras personalizadas (como el `RoleHeaderFilter` de `SEC-REQ-005`), las reglas de Spring Security pueden ser omitidas permitiendo ejecución no autorizada de endpoints administrativos (`POST /api/catalog/products` y `PUT /api/orders/{id}/status`).

---

## 3. Plan de Remediación (Hacia Estado B)

1. **Actualizar Spring Boot Starter Parent** a `>= 3.3.6` para obtener Tomcat `>= 10.1.34` y Spring Security `>= 6.3.5`.
2. Reemplazar la autorización basada en la cabecera `X-User-Role` por la extracción criptográfica del claim `roles` dentro del JWT decodificado en `JwtAuthenticationToken`.
3. Implementar comprobaciones a nivel de servicio (`@PreAuthorize` o validación de ID de usuario autenticado contra `order.clienteId`) para resolver BOLA/IDOR.

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa SCA.*  
*Evidencia cruda disponible en:* `Evidences/SEC-CASE-02/SCA/CASE-02-EV002-osv-vulns.json`

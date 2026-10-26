# SEC-CASE-03 — Seguridad de API
## Reporte de Security Tests Automatizados — Estado A (Baseline)

**Entorno de ejecución:** Suite Automatizada de Pruebas de Seguridad / CI-CD Verification  
**Fecha:** 2026-10-16  
**Resultado general Estado A:** ❌ **FAIL (4/4 pruebas fallidas — 100% FAIL)**  
**Objetivo de la línea base:** Demostrar el fallo de aserciones de diseño seguro en la API (Mass Assignment, manipulación de precios, fuga de PII/hashes y falta de rate limiting).

---

## 1. Matriz de Ejecución de Pruebas Automatizadas

| ID Test | Requisito | Nombre de la Prueba | Aserción de Seguridad Esperada (Estado B) | Resultado en Estado A | Diagnóstico |
|---|---|---|---|:---:|---|
| **TEST-SEC-006a** | SEC-REQ-006 | Prevención de Mass Assignment en registro | El endpoint debe recibir un DTO inmutable sin campo `role` | ❌ **FAIL** | `AuthController.java:26` recibe directamente `@RequestBody User user`. |
| **TEST-SEC-006b** | SEC-REQ-006 | Validación de precios de catálogo en pedidos | El precio debe obtenerse siempre del catálogo, ignorando al cliente | ❌ **FAIL** | `OrdersController.java:135` respeta el precio enviado por el cliente. |
| **TEST-SEC-007a** | SEC-REQ-007 | Ocultamiento de `passwordHash` | El campo `passwordHash` debe tener `@JsonIgnore` o usar DTO de salida | ❌ **FAIL** | `User.java` no tiene `@JsonIgnore` en `passwordHash`, exponiéndolo en JSON. |
| **TEST-SEC-008** | SEC-REQ-008 | Rate Limiter configurado en BFF | Debe registrarse un filtro de limitación de tasa en la cadena de seguridad | ❌ **FAIL** | `SecurityConfig.java` carece de filtros de limitación de peticiones. |

---

## 2. Detalle de Pruebas y Evidencias de Fallo

### Prueba TEST-SEC-006a: Mass Assignment en Registro de Usuarios

#### Código bajo prueba
`api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java:26-30`:
```java
// BASELINE-VULN [SEC-REQ-006]: el registro recibe la entidad User directamente permitiendo asignar roles arbitrarios
@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody User user) {
    user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
    User saved = userRepository.save(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
}
```

#### Aserción automatizada
```python
assert "@RequestBody User user" not in content
# AssertionError: El endpoint /api/auth/register recibe la entidad JPA User directamente, permitiendo inyectar 'role':'ADMIN'.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Cualquier atacante puede registrarse directamente con rol `ADMIN`.

---

### Prueba TEST-SEC-006b: Manipulación de Precio Unitario

#### Código bajo prueba
`api_cloud/src/main/java/cl/duoc/api/OrdersController.java:134-136`:
```java
// BASELINE-VULN [SEC-REQ-006]: solo completa descripcion y precioUnitario si no vienen; si vienen respeta los del cliente
if (productoId != null && (!itemNode.hasNonNull("descripcion") || !itemNode.hasNonNull("precioUnitario"))) {
    // Solo si falta precioUnitario se consulta al catálogo
```

#### Aserción automatizada
```python
assert "!itemNode.hasNonNull(\"precioUnitario\")" not in ctrl_content
# AssertionError: El BFF solo consulta el catálogo si precioUnitario no viene; si el cliente lo envía, respeta el precio del cliente.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Pérdida financiera por compra de productos a precios arbitrarios (\$0.01).

---

### Prueba TEST-SEC-007a: Exposición de Hashes de Contraseña

#### Código bajo prueba
`api_cloud/src/main/java/cl/duoc/api/auth/User.java`:
```java
private String passwordHash; // Sin anotación @JsonIgnore
```

#### Aserción automatizada
```python
assert "@JsonIgnore" in user_content.split("passwordHash")[0][-100:]
# AssertionError: El campo passwordHash de la entidad User no posee anotación @JsonIgnore ni DTO de proyección seguro.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Filtración del hash BCrypt en las respuestas de `/api/auth/register` y `/api/auth/me`.

---

### Prueba TEST-SEC-008: Ausencia de Rate Limiting en BFF

#### Código bajo prueba
`api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java:27`:
```java
// BASELINE-VULN [SEC-REQ-008]: BFF sin ningun limitador de peticiones (rate limiting)
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { ... }
```

#### Aserción automatizada
```python
assert "RateLimit" in sec_content or "Bucket" in sec_content
# AssertionError: SecurityConfig no registra ningún filtro de limitación de tasa de peticiones (Rate Limiter).
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Vulnerabilidad a ataques de denegación de servicio (DoS) y fuerza bruta en la API.

---

## 3. Plan de Remediación (Hacia Estado B)

1. Crear `RegisterRequestDto` con validaciones estrictas y sin campo `role`.
2. En `OrdersController`, forzar la sobrescritura del `precioUnitario` con el precio vigente obtenido desde el microservicio de catálogo.
3. Anotar `passwordHash` con `@JsonIgnore` y utilizar DTOs de salida (`UserResponseDto`) para el endpoint `/me`.
4. Incorporar un `RateLimitingFilter` en `SecurityConfig` configurado con Bucket4j.

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa Security Tests Automatizados.*  
*Resultados JSON disponibles en:* `Evidences/SEC-CASE-03/tests/CASE-03-EV002-test-results.json`

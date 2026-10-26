# SEC-CASE-01 — Autenticación y Gestión de Sesiones
## Reporte de Security Tests Automatizados — Estado A (Baseline)

**Entorno de ejecución:** Suite Automatizada de Pruebas de Seguridad / CI-CD Verification  
**Fecha:** 2026-10-16  
**Resultado general Estado A:** ❌ **FAIL (2/2 pruebas fallidas — 100% FAIL)**  
**Objetivo de la línea base:** Demostrar que las aserciones de seguridad fallan ante el código intencionalmente vulnerable.

---

## 1. Matriz de Ejecución de Pruebas Automatizadas

| ID Test | Requisito | Nombre de la Prueba | Aserción de Seguridad Esperada (Estado B) | Resultado en Estado A | Diagnóstico |
|---|---|---|---|:---:|---|
| **TEST-SEC-001** | SEC-REQ-001 | Bloqueo por intentos fallidos de login | La lógica de autenticación debe implementar contador de intentos fallidos o rate limiting (Bucket4j/Spring Security) | ❌ **FAIL** | El controlador `AuthController.java:47` procesa credenciales sin límite ni bloqueo. |
| **TEST-SEC-003** | SEC-REQ-003 | Expiración de token JWT de acceso <= 1h | El token JWT debe expirar en $\le 3600$ segundos (1 hora) con mecanismo de refresh | ❌ **FAIL** | `JwtService.java:37` configura `now.plus(30, ChronoUnit.DAYS)` (30 días). |

---

## 2. Detalle de Pruebas y Evidencias de Fallo

### Prueba TEST-SEC-001: Verificación de Mecanismo de Bloqueo en Login

#### Código bajo prueba
`api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java:47`:
```java
// BASELINE-VULN [SEC-REQ-001]: falta de bloqueo por intentos fallidos de autenticacion
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    User user = userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));
    if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
    }
    String token = jwtService.generateToken(user);
    return ResponseEntity.ok(Map.of("token", token, "role", user.getRole()));
}
```

#### Aserción automatizada
```python
assert "maxAttempts" in content or "lockAccount" in content or "Bucket" in content or "RateLimiter" in content
# AssertionError: El endpoint /api/auth/login procesa credenciales sin contador de intentos fallidos ni bloqueo temporal.
```

#### Traza de resultado
- **Estado:** ❌ `FAIL`
- **Impacto:** Un atacante puede ejecutar fuerza bruta ilimitada contra cualquier cuenta en el sistema.

---

### Prueba TEST-SEC-003: Expiración de Token JWT $\le 1\text{ hora}$

#### Código bajo prueba
`api_cloud/src/main/java/cl/duoc/api/auth/JwtService.java:35-37`:
```java
// BASELINE-VULN [SEC-REQ-003]: duracion de sesion excesiva (30 dias) sin renovacion
Instant now = Instant.now();
Instant expiration = now.plus(30, ChronoUnit.DAYS);
```

#### Aserción automatizada
```python
validity_seconds = (expiration - now).total_seconds()
assert validity_seconds <= 3600, f"Token validity {validity_seconds}s exceeds 3600s threshold"
# AssertionError: Token validity 2592000s exceeds 3600s threshold (30 días de vigencia detectados)
```

#### Traza de resultado
- **Estado:** ❌ `FAIL`
- **Impacto:** Si un token es filtrado o capturado en red, el atacante mantiene acceso persistente durante 30 días sin revocación.

---

## 3. Plan de Remediación (Hacia Estado B)

1. En `AuthController`, implementar interceptor con Bucket4j para limitar a 5 intentos fallidos por IP/correo antes de bloquear temporalmente la cuenta.
2. En `JwtService`, reducir la validez del token a 15 minutos: `now.plus(15, ChronoUnit.MINUTES)` e implementar esquema de refresh token con rotación.

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa Security Tests Automatizados.*  
*Resultados JSON disponibles en:* `Evidences/SEC-CASE-01/tests/CASE-01-EV002-test-results.json`

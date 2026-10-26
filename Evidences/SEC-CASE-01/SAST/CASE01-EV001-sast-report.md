# SEC-CASE-01 — Autenticación y Gestión de Sesiones
## Reporte SAST — Estado A (Baseline)

**Herramienta:** Semgrep 1.181.0 + Revisión manual de marcadores BASELINE-VULN  
**Fecha:** 2026-10-14  
**Rama/commit analizado:** Estado A (Baseline) — `D:\Escritorio\CloudComputing_Baseline`  
**Resultado esperado:** FAIL (vulnerabilidades intencionales de laboratorio)

---

## Resumen Ejecutivo

| ID Finding | SEC-REQ | Severidad | CWE | OWASP 2021 | Estado |
|---|---|---|---|---|---|
| CASE01-F001 | SEC-REQ-001 | HIGH | CWE-307 | A07:2021 – Identification & Auth Failures | ❌ FAIL |
| CASE01-F002 | SEC-REQ-003 | MEDIUM | CWE-613 | A07:2021 – Identification & Auth Failures | ❌ FAIL |

**Total hallazgos:** 2  
**Hallazgos por herramienta automatizada (Semgrep):** 0 (reglas estándar no cubren estos patrones específicos)  
**Hallazgos por revisión de código manual (marcadores BASELINE-VULN):** 2  

---

## Hallazgos Detallados

---

### CASE01-F001 — Sin bloqueo por intentos fallidos de autenticación

| Campo | Valor |
|---|---|
| **ID** | CASE01-F001 |
| **SEC-REQ** | SEC-REQ-001 |
| **Tipo** | Falta de control de intentos de login (Brute Force) |
| **CWE** | CWE-307: Improper Restriction of Excessive Authentication Attempts |
| **OWASP 2021** | A07: Identification and Authentication Failures |
| **Severidad** | HIGH |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java` |
| **Línea** | 47 |

#### Evidencia — Código fuente

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

#### Descripción del problema

El endpoint `POST /api/auth/login` no implementa ningún mecanismo de limitación de intentos de autenticación:
- No hay contador de intentos fallidos por usuario o por IP.
- No hay bloqueo temporal de cuentas tras N intentos fallidos.
- No hay CAPTCHA ni factor adicional.
- Un atacante puede realizar ataques de fuerza bruta o password spraying sin restricción alguna.

#### Impacto

Un atacante puede intentar millones de combinaciones de contraseña contra cualquier cuenta sin ser bloqueado, comprometiendo potencialmente cualquier cuenta del sistema.

#### Reproducción

```bash
# Prueba conceptual de brute force — N intentos consecutivos sin bloqueo
for i in $(seq 1 1000); do
  curl -s -X POST http://localhost:8088/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@cafe.test","password":"intento'$i'"}'
done
# Esperado (Estado A): todos los intentos se procesan; no hay bloqueo ni throttling
```

#### Remediación sugerida (Estado B)

- Implementar `spring-security-ratelimiter` o Bucket4j para limitar intentos por IP/usuario.
- Bloquear cuenta temporalmente tras 5 intentos fallidos.
- Registrar intentos fallidos en logs de auditoría.

---

### CASE01-F002 — Duración de sesión JWT excesiva (30 días)

| Campo | Valor |
|---|---|
| **ID** | CASE01-F002 |
| **SEC-REQ** | SEC-REQ-003 |
| **Tipo** | Duración de sesión excesiva / Sin renovación |
| **CWE** | CWE-613: Insufficient Session Expiration |
| **OWASP 2021** | A07: Identification and Authentication Failures |
| **Severidad** | MEDIUM |
| **Método de detección** | Revisión manual — marcador BASELINE-VULN |
| **Archivo** | `api_cloud/src/main/java/cl/duoc/api/auth/JwtService.java` |
| **Línea** | 35 |

#### Evidencia — Código fuente

```java
// BASELINE-VULN [SEC-REQ-003]: duracion de sesion excesiva (30 dias) sin renovacion
.expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30)) // 30 días
```

#### Descripción del problema

El JWT emitido en el login tiene una vigencia de **30 días** sin ningún mecanismo de:
- Refresh token (el token de larga vida es el único mecanismo de sesión).
- Revocación de tokens comprometidos.
- Rotación de tokens.

Si un token es robado (XSS, log exposure, MITM), el atacante tiene acceso válido por 30 días sin que el sistema pueda invalidarlo.

#### Cálculo de vigencia

```
1000 ms × 60 s × 60 min × 24 h × 30 días = 2,592,000,000 ms = 30 días
```

#### Impacto

Un token comprometido otorga acceso al sistema durante 30 días completos. Combinado con el secreto débil `changeme` (SEC-REQ-012), los tokens pueden ser falsificados o robados con facilidad.

#### Reproducción

```bash
# 1. Hacer login y obtener el token
TOKEN=$(curl -s -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"cliente1@cafe.test","password":"Test1234!"}' | \
  python -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 2. Decodificar el payload (sin verificar firma)
echo $TOKEN | cut -d. -f2 | base64 -d 2>/dev/null | python -m json.tool
# Esperado (Estado A): campo "exp" = now + 30 días
```

#### Remediación sugerida (Estado B)

- Reducir vigencia del access token a 15–60 minutos.
- Implementar refresh token con vigencia de 7–14 días y rotación.
- Implementar blacklist de tokens revocados (Redis).

---

## Limitaciones del Análisis Automatizado

Semgrep con los rulesets `p/default`, `p/owasp-top-ten`, `p/security-audit` y `p/secrets` **no detectó** estos hallazgos porque:

1. **SEC-REQ-001:** La ausencia de un mecanismo de rate-limiting no es un patrón de código detectable por análisis estático de flujo; requiere análisis semántico del *comportamiento* del controlador.
2. **SEC-REQ-003:** El valor literal `30` en el cálculo de expiración no está cubierto por reglas de Semgrep estándar para Spring Boot. Requería una regla personalizada para detectar `expiration()` con valores > umbral.

**Conclusión:** Para estos requisitos, la revisión de código manual y los marcadores BASELINE-VULN son el método SAST primario. Las pruebas DAST (fuerza bruta real, análisis del JWT) complementarán estos hallazgos.

---

## Trazabilidad

| Marcador en código | Archivo:línea | Finding |
|---|---|---|
| `BASELINE-VULN [SEC-REQ-001]` | `AuthController.java:47` | CASE01-F001 |
| `BASELINE-VULN [SEC-REQ-003]` | `JwtService.java:35` | CASE01-F002 |

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa SAST.*  
*Proyecto: Pedidos360 Coffee Store — Ejercicio académico OCY1102.*

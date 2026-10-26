# SEC-CASE-01 — Autenticación y Gestión de Sesiones
## Reporte DAST (Dynamic Application Security Testing) — Estado A (Baseline)

**Metodología:** OWASP Web Security Testing Guide (WSTG v4.2) / OWASP API Security Top 10 2023  
**Fecha:** 2026-10-15  
**Objetivo:** Evaluar dinámicamente el comportamiento de los endpoints de autenticación y sesiones del BFF (`api_cloud`, puerto 8088).  
**Resultado global Estado A:** ❌ **FAIL** (Confirmación de vulnerabilidades de fuerza bruta y vigencia excesiva de tokens)

---

## 1. Resumen Ejecutivo de Pruebas Dinámicas

| ID Prueba DAST | Requisito | WSTG / OWASP API | Endpoint Objetivo | Aserción de Seguridad Esperada (Estado B) | Comportamiento Observado (Estado A) | Resultado |
|---|---|---|---|---|---|:---:|
| **DAST-01-01** | SEC-REQ-001 | WSTG-ATHN-03 / API2:2023 | `POST /api/auth/login` | Bloqueo temporal o `429 Too Many Requests` tras 5 intentos fallidos | 10+ intentos fallidos consecutivos retornan `401 Unauthorized` sin restricción ni demora | ❌ **FAIL** |
| **DAST-01-02** | SEC-REQ-003 | WSTG-SESS-07 / API2:2023 | `POST /api/auth/login` | Vigencia de token `<= 3600s` (1 hora) con mecanismo de refresh | Token JWT devuelto con `exp - iat = 2,592,000s` (30 días exactos) | ❌ **FAIL** |

---

## 2. Detalle de Pruebas Dinámicas y Evidencias

---

### Prueba DAST-01-01: Verificación de Mecanismos de Bloqueo y Throttling en Autenticación

#### 1. Objetivo y Alcance
Evaluar si el endpoint de inicio de sesión (`POST /api/auth/login`) restringe el número de intentos fallidos de autenticación para mitigar ataques de diccionario, fuerza bruta y password spraying.

- **Referencia OWASP:** WSTG-ATHN-03 (Testing for Weak Lockout Mechanism).
- **Código analizado:** `api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java:47`.

#### 2. Transacción HTTP Dinámica

**Petición HTTP enviada:**
```http
POST /api/auth/login HTTP/1.1
Host: 127.0.0.1:8088
Content-Type: application/json
Accept: application/json

{
  "email": "admin@cafe.test",
  "password": "PasswordIncorrecto999"
}
```

**Respuesta recibida en Estado A (Iteraciones 1 a 15 consecutivas):**
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json
Date: Thu, 15 Oct 2026 15:45:00 GMT
Connection: keep-alive

{
  "timestamp": "2026-10-15T15:45:00.123+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciales invalidas",
  "path": "/api/auth/login"
}
```

#### 3. Análisis de la Aserción Dinámica
- **Aserción:** Tras $N \ge 5$ peticiones consecutivas con contraseña errónea para la misma cuenta o IP, el servidor debe responder con `429 Too Many Requests` o denegar la solicitud por cuenta bloqueada.
- **Resultado en Estado A:** Las 15 peticiones consecutivas fueron procesadas inmediatamente (tiempo promedio: ~18ms), recibiendo consistentemente código `401 Unauthorized` con el mensaje `"Credenciales invalidas"`. No existe demora exponencial (tarpit), CAPTCHA, ni bloqueo temporal.
- **Dictamen:** ❌ **FAIL** (Vulnerable a Fuerza Bruta).

---

### Prueba DAST-01-02: Inspección Dinámica de Ciclo de Vida y Expiración del Token JWT

#### 1. Objetivo y Alcance
Verificar que la duración de vida del token de acceso (`access_token`) emitido en el login cumpla con los estándares de seguridad de sesiones stateless (tiempo de vida corto, entre 15 y 60 minutos).

- **Referencia OWASP:** WSTG-SESS-07 (Testing Session Timeout).
- **Código analizado:** `api_cloud/src/main/java/cl/duoc/api/auth/JwtService.java:35`.

#### 2. Transacción HTTP Dinámica

**Petición HTTP de login exitoso:**
```http
POST /api/auth/login HTTP/1.1
Host: 127.0.0.1:8088
Content-Type: application/json

{
  "email": "cliente1@cafe.test",
  "password": "Test1234!"
}
```

**Respuesta HTTP recibida:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJlbWFpbCI6ImNsaWVudGUxQGNhZmUudGVzdCIsInJvbGUiOiJDTElFTlRFIiwiaWF0IjoxNzg0MTM2ODAwLCJleHAiOjE3ODY3Mjg4MDB9...",
  "role": "CLIENTE"
}
```

#### 3. Decodificación Dinámica del Payload JWT

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "cliente1@cafe.test",
  "role": "CLIENTE",
  "iat": 1784136800,
  "exp": 1786728800
}
```

#### 4. Análisis de la Aserción Dinámica
- **Cálculo de vigencia:**
  $$\Delta t = \text{exp} - \text{iat} = 1786728800 - 1784136800 = 2,592,000\text{ segundos}$$
  $$\frac{2,592,000}{3600 \times 24} = 30\text{ días exactos}$$
- **Aserción:** $\Delta t \le 3,600\text{ s}$ (1 hora).
- **Resultado en Estado A:** El token emitido posee una validez de 720 horas (30 días) sin mecanismo de revocación ni rotación. Si el token es interceptado o filtrado, el atacante tiene persistencia total durante un mes completo.
- **Dictamen:** ❌ **FAIL** (Expiración de Sesión Insuficiente).

---

## 3. Matriz de Hallazgos y Remediación (Hacia Estado B)

| Hallazgo Dinámico | Vulnerabilidad | Remediación en Estado B |
|---|---|---|
| DAST-01-01 | Ausencia de Rate Limiting en Login | Integrar Bucket4j / Spring Filter con límite de 5 intentos por minuto por IP/email con respuesta `429`. |
| DAST-01-02 | Token JWT con vigencia de 30 días | Reducir el claim `expiration` a 15 minutos (`now + 900_000ms`) e implementar refresh token persistido y revocable. |

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa DAST.*  
*Especificaciones dinámicas disponibles en:* `Evidences/SEC-CASE-01/DAST/CASE01-EV002-dast-specs.json`

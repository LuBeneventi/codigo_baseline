# SEC-CASE-03 — Seguridad de API
## Reporte DAST (Dynamic Application Security Testing) — Estado A (Baseline)

**Metodología:** OWASP API Security Top 10 2023 / WSTG v4.2  
**Fecha:** 2026-10-15  
**Objetivo:** Evaluar dinámicamente vulnerabilidades de Mass Assignment, manipulación de precios comerciales, exposición de datos sensibles y resistencia a ráfagas de tráfico.  
**Resultado global Estado A:** ❌ **FAIL** (Confirmación de asignación masiva de roles, manipulación de precio unitario y fuga de hashes de contraseña)

---

## 1. Resumen Ejecutivo de Pruebas Dinámicas

| ID Prueba DAST | Requisito | OWASP API 2023 | Endpoint Objetivo | Aserción de Seguridad Esperada (Estado B) | Comportamiento Observado (Estado A) | Resultado |
|---|---|---|---|---|---|:---:|
| **DAST-03-01** | SEC-REQ-006 | API3:2023 (BOPM / Mass Assignment) | `POST /api/auth/register` | El rol se fuerza a `CLIENTE` ignorando el body | El campo `"role": "ADMIN"` enviado en el body es persistido en BD | ❌ **FAIL** |
| **DAST-03-02** | SEC-REQ-006 | API3:2023 (Business Logic) | `POST /api/orders` | El precio se toma exclusivamente del catálogo | Se respeta el `precioUnitario: 0.01` enviado arbitrariamente por el cliente | ❌ **FAIL** |
| **DAST-03-03** | SEC-REQ-007 | API3:2023 (Data Exposure) | `GET /api/auth/me` | Retorna únicamente perfil público (sin hashes) | El JSON incluye el campo `passwordHash` en texto claro | ❌ **FAIL** |
| **DAST-03-04** | SEC-REQ-007 | API3:2023 (Data Exposure) | `GET /api/orders/{id}/enriched` | Datos PII (RUT, dirección) restringidos por rol | Expone RUT y dirección física particular a cualquier solicitante | ❌ **FAIL** |
| **DAST-03-05** | SEC-REQ-008 | API4:2023 (Unrestricted Resource) | `GET /api/catalog/products` | Respuestas `429 Too Many Requests` ante ráfagas | 100 req/s procesadas al 100% sin rate limiting | ❌ **FAIL** |

---

## 2. Detalle de Pruebas Dinámicas y Evidencias

---

### Prueba DAST-03-01: Mass Assignment en Registro de Usuarios (`POST /api/auth/register`)

#### 1. Transacción HTTP Dinámica

**Petición enviada con asignación arbitraria de rol:**
```http
POST /api/auth/register HTTP/1.1
Host: 127.0.0.1:8088
Content-Type: application/json

{
  "email": "hacker@test.com",
  "passwordHash": "Hacker1234!",
  "name": "Usuario Malicioso",
  "phone": "+56911223344",
  "role": "ADMIN"
}
```

**Respuesta recibida en Estado A:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "771e8400-e29b-41d4-a716-446655440099",
  "email": "hacker@test.com",
  "passwordHash": "$2a$10$eOQ54xJ7Yk...hash...",
  "name": "Usuario Malicioso",
  "phone": "+56911223344",
  "role": "ADMIN"
}
```

#### 2. Análisis de Aserción
- **Aserción:** Todo usuario registrado mediante endpoint público debe tener `role: "CLIENTE"`.
- **Resultado:** El controlador `AuthController.java:26` deserializa directamente la entidad `User`. El rol `"ADMIN"` es persistido en la base de datos `pedidos360_auth`.
- **Dictamen:** ❌ **FAIL**.

---

### Prueba DAST-03-02: Manipulación de Precios en Creación de Pedido (`POST /api/orders`)

#### 1. Transacción HTTP Dinámica

**Petición enviando un producto cuyo precio de catálogo es $15.000, pero alterando `precioUnitario` a $1.00:**
```http
POST /api/orders HTTP/1.1
Host: 127.0.0.1:8088
Authorization: Bearer <TOKEN_CLIENTE>
Content-Type: application/json

{
  "clienteId": "550e8400-e29b-41d4-a716-446655440001",
  "nombre": "Cliente Uno",
  "correo": "cliente1@cafe.test",
  "rut": "12.345.678-9",
  "direccion": "Calle Falsa 123",
  "items": [
    {
      "productoId": "c3d4e5f6-7890-abcd-ef12-34567890abcd",
      "cantidad": 10,
      "precioUnitario": 1.00
    }
  ]
}
```

**Respuesta recibida en Estado A:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "d1e2f3a4-b5c6-7890-1234-567890abcdef",
  "clienteId": "550e8400-e29b-41d4-a716-446655440001",
  "total": 10.00,
  "estado": "CREADO",
  "items": [
    {
      "productoId": "c3d4e5f6-7890-abcd-ef12-34567890abcd",
      "cantidad": 10,
      "precioUnitario": 1.00,
      "subtotal": 10.00
    }
  ]
}
```

#### 2. Análisis de Aserción
- **Aserción:** El total debe ser $10 \times 15.000 = \$150.000$.
- **Resultado:** El BFF en `OrdersController.java:118` solo consulta el precio al catálogo si `precioUnitario == null`. Al recibir `1.00`, lo respeta y genera una orden por \$10 en lugar de \$150.000.
- **Dictamen:** ❌ **FAIL**.

---

### Prueba DAST-03-03: Fuga de Hash de Contraseña (`GET /api/auth/me`)

#### 1. Transacción HTTP Dinámica

**Petición enviada:**
```http
GET /api/auth/me HTTP/1.1
Host: 127.0.0.1:8088
Authorization: Bearer <TOKEN_VALIDO>
```

**Respuesta recibida en Estado A:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "email": "cliente1@cafe.test",
  "passwordHash": "$2a$10$wT0sZfG8W5Z...bcrypt_hash...",
  "name": "Cliente Uno",
  "phone": "+56912345678",
  "role": "CLIENTE"
}
```

#### 2. Análisis de Aserción
- **Aserción:** El campo `passwordHash` jamás debe viajar en payloads de respuesta HTTP.
- **Resultado:** Se expone el hash BCrypt completo en cada llamada a `/me` y `/register`.
- **Dictamen:** ❌ **FAIL**.

---

## 3. Matriz de Hallazgos y Remediación (Hacia Estado B)

| Hallazgo Dinámico | Vulnerabilidad | Remediación en Estado B |
|---|---|---|
| DAST-03-01 | Mass Assignment | Crear `RegisterRequestDto` que contenga solo `email`, `password`, `name`, `phone`; forzar `user.setRole("CLIENTE")` en el servicio. |
| DAST-03-02 | Manipulación de Precios | En `OrdersController.java`, ignorar cualquier precio proveniente del cliente y consultar siempre el precio canónico a `ms-pedidos360-catalog`. |
| DAST-03-03 | Exposición de `passwordHash` | Crear `UserResponseDto` excluyendo `passwordHash` y anotando el campo en la entidad con `@JsonIgnore`. |
| DAST-03-05 | Sin Rate Limiting | Configurar filtro Bucket4j en el BFF para limitar a 50 peticiones/minuto por cliente/IP. |

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa DAST.*  
*Especificaciones dinámicas disponibles en:* `Evidences/SEC-CASE-03/DAST/CASE03-EV002-dast-specs.json`

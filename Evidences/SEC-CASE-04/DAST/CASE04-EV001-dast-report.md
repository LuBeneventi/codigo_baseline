# SEC-CASE-04 — Validación y Sanitización de Entradas
## Reporte DAST (Dynamic Application Security Testing) — Estado A (Baseline)

**Metodología:** OWASP Web Security Testing Guide (WSTG v4.2) / OWASP API Security Top 10 2023  
**Fecha:** 2026-10-15  
**Objetivo:** Evaluar dinámicamente vulnerabilidades de inyección SQL, inyección de scripts (XSS), inyección HTML en notificaciones y procesamiento de valores numéricos inválidos.  
**Resultado global Estado A:** ❌ **FAIL** (Confirmación de SQL Injection activo en catálogo, validación nula de cantidades/precios y XSS en frontend)

---

## 1. Resumen Ejecutivo de Pruebas Dinámicas

| ID Prueba DAST | Requisito | WSTG / OWASP API | Endpoint Objetivo | Aserción de Seguridad Esperada (Estado B) | Comportamiento Observado (Estado A) | Resultado |
|---|---|---|---|---|---|:---:|
| **DAST-04-01** | SEC-REQ-010 | WSTG-INPV-05 / API8:2023 | `GET /api/catalog/products/search?q=` | Búsqueda parametrizada sin alteración lógica | Carga útil `' OR '1'='1` altera la consulta SQL devolviendo todo el catálogo | ❌ **FAIL** |
| **DAST-04-02** | SEC-REQ-010 | WSTG-INPV-01 / API8:2023 | `POST /api/orders` | `400 Bad Request` ante `cantidad <= 0` | Acepta `cantidad: -5` y crea el pedido sin error de validación | ❌ **FAIL** |
| **DAST-04-03** | SEC-REQ-010 | WSTG-INPV-01 / API8:2023 | `POST /api/catalog/products` | `400 Bad Request` ante `price <= 0` | Acepta `price: -100.00` y `stock: -20` persistiendo valores negativos | ❌ **FAIL** |
| **DAST-04-04** | SEC-REQ-009 | WSTG-INPV-01 / WSTG-CLNT-01 | Frontend (`http://127.0.0.1:8080`) | Nombres y descripciones escapados como texto | Tags HTML inyectados se evalúan en el DOM con `dangerouslySetInnerHTML` | ❌ **FAIL** |
| **DAST-04-05** | SEC-REQ-011 | WSTG-INPV-01 | `ms-pedidos360-notify` (RabbitMQ) | Caracteres `<, >, &` codificados en entidades HTML | Se inyectan bloques HTML arbitrarios dentro del correo generado | ❌ **FAIL** |

---

## 2. Detalle de Pruebas Dinámicas y Evidencias

---

### Prueba DAST-04-01: Inyección SQL Dinámica en Buscador de Catálogo (`GET /api/catalog/products/search`)

#### 1. Objetivo y Alcance
Evaluar si la consulta nativa en `ProductController.java:35` es susceptible a inyección SQL mediante manipulación del parámetro `q`.
- **Código:** `String sql = "SELECT * FROM products WHERE name ILIKE '%" + q + "%'";`

#### 2. Transacción HTTP Dinámica

**Petición 1 (Búsqueda benigna inexistente):**
```http
GET /api/catalog/products/search?q=TerminoQueNoExiste999 HTTP/1.1
Host: 127.0.0.1:8088
Accept: application/json
```
**Respuesta:** `HTTP 200 OK` con `[]` (cero resultados).

**Petición 2 (Inyección SQL tautológica):**
```http
GET /api/catalog/products/search?q=%27%20OR%20%271%27%3D%271 HTTP/1.1
Host: 127.0.0.1:8088
Accept: application/json
```
*Valor decodificado de `q`:* `' OR '1'='1`

**Consulta SQL resultante ejecutada en PostgreSQL:**
```sql
SELECT * FROM products WHERE name ILIKE '%' OR '1'='1%'
```

**Respuesta recibida en Estado A:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "11111111-2222-3333-4444-555555555555",
    "name": "Café Expreso Doble",
    "price": 2500.00,
    "stock": 50
  },
  {
    "id": "22222222-3333-4444-5555-666666666666",
    "name": "Café Latte Vainilla",
    "price": 3200.00,
    "stock": 30
  },
  {
    "id": "33333333-4444-5555-6666-777777777777",
    "name": "Cappuccino Tradicional",
    "price": 2800.00,
    "stock": 40
  }
]
```

#### 3. Análisis de Aserción
- **Aserción:** La consulta debe buscar literalmente el texto o rechazar comillas con sanitización/parametrización.
- **Resultado:** La inyección altera la lógica booleana de la cláusula `WHERE`, retornando la totalidad de registros de la tabla `products`.
- **Dictamen:** ❌ **FAIL** (Vulnerable a SQL Injection).

---

### Prueba DAST-04-02: Creación de Pedido con Valores Numéricos Inválidos (Cantidades Negativas)

#### 1. Transacción HTTP Dinámica

**Petición con `cantidad: -5` y `precioUnitario: -1000`:**
```http
POST /api/orders HTTP/1.1
Host: 127.0.0.1:8088
Authorization: Bearer <TOKEN_VALIDO>
Content-Type: application/json

{
  "clienteId": "550e8400-e29b-41d4-a716-446655440001",
  "items": [
    {
      "productoId": "11111111-2222-3333-4444-555555555555",
      "cantidad": -5,
      "precioUnitario": -1000.00
    }
  ]
}
```

**Respuesta recibida en Estado A:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d",
  "total": 5000.00,
  "estado": "CREADO",
  "items": [
    {
      "cantidad": -5,
      "precioUnitario": -1000.00,
      "subtotal": 5000.00
    }
  ]
}
```

#### 2. Análisis de Aserción
- **Aserción:** El sistema debe responder `400 Bad Request` indicando que `cantidad` debe ser mayor o igual a 1 (`@Min(1)`).
- **Resultado:** En `OrderItemRequestDto.java:23, 28` se eliminaron intencionalmente las validaciones `@Min(1)` y `@Positive`. El pedido con cantidades negativas es aceptado y procesado.
- **Dictamen:** ❌ **FAIL**.

---

### Prueba DAST-04-04: XSS Stored en Interfaz de Usuario Frontend (`dangerouslySetInnerHTML`)

#### 1. Transacción Dinámica
- **Vector inyectado en `name` de producto:** `<img src=x onerror="document.title='XSS-PROVED'">`
- **Renderizado en `frontend/src/App.tsx:352`:**
  ```tsx
  <span dangerouslySetInnerHTML={{ __html: product.name }} />
  ```
- **Resultado en navegador:** El DOM ejecuta inmediatamente el manejador de eventos `onerror`, demostrando la ejecución de código script arbitrario en el contexto del origen de la víctima.
- **Dictamen:** ❌ **FAIL** (Vulnerable a DOM/Stored XSS).

---

## 3. Matriz de Hallazgos y Remediación (Hacia Estado B)

| Hallazgo Dinámico | Vulnerabilidad | Remediación en Estado B |
|---|---|---|
| DAST-04-01 | SQL Injection | Usar consulta parametrizada: `SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))`. |
| DAST-04-02 | Cantidades inválidas | Restaurar anotaciones `@NotNull @Min(1)` en `cantidad` y `@NotNull @Positive` en `precioUnitario` de `OrderItemRequestDto`. |
| DAST-04-03 | Stock/precio negativo | Restaurar `@DecimalMin("0.01")` y `@Min(0)` en `ProductCreateDto` y `Product.java`. |
| DAST-04-04 | Stored DOM XSS | Eliminar `dangerouslySetInnerHTML`; utilizar renderizado de texto seguro nativo `{product.name}` de JSX. |
| DAST-04-05 | Inyección HTML en correo | Aplicar `HtmlUtils.htmlEscape(customerName)` en `EmailSenderService.java:110`. |

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa DAST.*  
*Especificaciones dinámicas disponibles en:* `Evidences/SEC-CASE-04/DAST/CASE04-EV002-dast-specs.json`

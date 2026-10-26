# SEC-CASE-02 — Autorización y Control de Acceso
## Reporte de Security Tests Automatizados — Estado A (Baseline)

**Entorno de ejecución:** Suite Automatizada de Pruebas de Seguridad / CI-CD Verification  
**Fecha:** 2026-10-16  
**Resultado general Estado A:** ❌ **FAIL (3/3 pruebas fallidas — 100% FAIL)**  
**Objetivo de la línea base:** Demostrar que las comprobaciones de autorización fallan ante la ausencia de controles BOLA y el uso de cabeceras de cliente para roles.

---

## 1. Matriz de Ejecución de Pruebas Automatizadas

| ID Test | Requisito | Nombre de la Prueba | Aserción de Seguridad Esperada (Estado B) | Resultado en Estado A | Diagnóstico |
|---|---|---|---|:---:|---|
| **TEST-SEC-004a** | SEC-REQ-004 | Aislamiento multi-inquilino en listOrders | Las consultas deben filtrar por `clienteId` del usuario autenticado en BD | ❌ **FAIL** | `listOrders()` en `OrderServiceImpl.java:122` consulta órdenes sin filtrar por usuario. |
| **TEST-SEC-004b** | SEC-REQ-004 | Comprobación de titularidad en getOrderById | Rechazar con excepción de autorización si `order.clienteId != usuarioAutenticado` | ❌ **FAIL** | `getOrderById()` en `OrderServiceImpl.java:85` recupera por ID sin validar propietario. |
| **TEST-SEC-005a** | SEC-REQ-005 | Autorización de roles basada en token JWT | El rol administrativo debe provenir del token firmado y verificado | ❌ **FAIL** | `RoleHeaderFilter.java:27` lee y confía en la cabecera del cliente `X-User-Role`. |

---

## 2. Detalle de Pruebas y Evidencias de Fallo

### Prueba TEST-SEC-004a: Aislamiento Multi-Inquilino en Listado de Pedidos

#### Código bajo prueba
`ms-pedidos360-orders/src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java:119-140`:
```java
// BASELINE-VULN [SEC-REQ-004]: findAll sin filtrar por usuario autenticado (BOLA/IDOR)
@Override
@Transactional(readOnly = true)
public List<OrderResponseDto> listOrders(OrderStatus status, LocalDateTime from, LocalDateTime to) {
    ...
    } else {
        orders = orderRepository.findAll();
    }
    return orders.stream().map(this::mapToResponseDto).collect(Collectors.toList());
}
```

#### Aserción automatizada
```python
assert "findByClienteId" in content or "findByUsuario" in content
# AssertionError: listOrders() consulta órdenes sin filtrar por el cliente autenticado en el repositorio.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Cualquier cliente autenticado puede ver el historial de pedidos de toda la plataforma.

---

### Prueba TEST-SEC-004b: Comprobación de Titularidad en `getOrderById`

#### Código bajo prueba
`ms-pedidos360-orders/src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java:82-92`:
```java
// BASELINE-VULN [SEC-REQ-004]: no verifica si el pedido pertenece al usuario autenticado (BOLA/IDOR)
@Override
@Transactional(readOnly = true)
public OrderResponseDto getOrderById(Long id) {
    Order order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
    return mapToResponseDto(order);
}
```

#### Aserción automatizada
```python
assert "clienteId" in get_order_block and "SecurityContext" in get_order_block
# AssertionError: getOrderById() recupera y retorna la orden por ID sin contrastar si pertenece al usuario que solicita.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** IDOR horizontal directo; acceso a pedidos individuales conociendo el ID.

---

### Prueba TEST-SEC-005a: Autorización Basada en Cabecera de Cliente

#### Código bajo prueba
`api_cloud/src/main/java/cl/duoc/api/auth/RoleHeaderFilter.java:27`:
```java
// BASELINE-VULN [SEC-REQ-005]: el rol lo declara el cliente, no se saca del token
String role = request.getHeader("X-User-Role");
if (!"ADMIN".equals(role)) {
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado: se requiere rol ADMIN");
    return;
}
```

#### Aserción automatizada
```python
assert 'request.getHeader("X-User-Role")' not in filter_content
# AssertionError: RoleHeaderFilter evalúa el rol desde la cabecera HTTP inyectable X-User-Role en lugar del JWT verificado.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Un usuario con rol `CLIENTE` obtiene privilegios de `ADMIN` inyectando `X-User-Role: ADMIN`.

---

## 3. Plan de Remediación (Hacia Estado B)

1. En `OrderServiceImpl`, sustituir consultas globales por `orderRepository.findByClienteId(authenticatedUserId)`.
2. En `getOrderById`, verificar que `order.getClienteId().equals(authenticatedUserId)` y lanzar `AccessDeniedException` si no coincide.
3. Eliminar `RoleHeaderFilter` y mapear los roles del JWT directamente en el `SecurityContextHolder` mediante un `JwtAuthenticationConverter`.

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa Security Tests Automatizados.*  
*Resultados JSON disponibles en:* `Evidences/SEC-CASE-02/tests/CASE-02-EV002-test-results.json`

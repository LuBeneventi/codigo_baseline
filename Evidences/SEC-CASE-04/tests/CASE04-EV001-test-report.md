# SEC-CASE-04 — Validación y Sanitización de Entradas
## Reporte de Security Tests Automatizados — Estado A (Baseline)

**Entorno de ejecución:** Suite Automatizada de Pruebas de Seguridad / CI-CD Verification  
**Fecha:** 2026-10-16  
**Resultado general Estado A:** ❌ **FAIL (3/3 pruebas fallidas — 100% FAIL)**  
**Objetivo de la línea base:** Demostrar el fallo de aserciones de validación de entradas numéricas, inyección SQL por concatenación y XSS por renderizado HTML crudo.

---

## 1. Matriz de Ejecución de Pruebas Automatizadas

| ID Test | Requisito | Nombre de la Prueba | Aserción de Seguridad Esperada (Estado B) | Resultado en Estado A | Diagnóstico |
|---|---|---|---|:---:|---|
| **TEST-SEC-010a** | SEC-REQ-010 | Consultas SQL parametrizadas en búsqueda | Las consultas nativas deben utilizar parámetros vinculados (`:q`) | ❌ **FAIL** | `ProductController.java:38` concatena `q` directamente en la sentencia SQL. |
| **TEST-SEC-010b** | SEC-REQ-010 | Validación Bean Validation en OrderItemRequestDto | Debe exigir `@Min(1)` en cantidad y `@Positive` en precioUnitario | ❌ **FAIL** | Anotaciones `@Min` y `@Positive` eliminadas en `OrderItemRequestDto.java:24,29`. |
| **TEST-SEC-009** | SEC-REQ-009 | Prevención de DOM XSS en React | No debe utilizarse `dangerouslySetInnerHTML` con datos de usuario/API | ❌ **FAIL** | `App.tsx` utiliza `dangerouslySetInnerHTML` en 12 ubicaciones para renderizar datos. |

---

## 2. Detalle de Pruebas y Evidencias de Fallo

### Prueba TEST-SEC-010a: Detección de SQL Injection por Concatenación

#### Código bajo prueba
`ms-pedidos360-catalog/src/main/java/com/pedidos360/catalog/controller/ProductController.java:35-39`:
```java
// BASELINE-VULN [SEC-REQ-010]: consulta con texto concatenado, sin parametrizar
@GetMapping("/search")
public ResponseEntity<?> search(@RequestParam String q) {
    String sql = "SELECT * FROM products WHERE name ILIKE '%" + q + "%'";
    List<?> results = entityManager.createNativeQuery(sql, Product.class).getResultList();
    return ResponseEntity.ok(results);
}
```

#### Aserción automatizada
```python
assert "ILIKE '%\" + q +" not in prod_content
# AssertionError: ProductController concatena directamente el parámetro 'q' sin parametrizar en createNativeQuery.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Alteración de la semántica de la consulta SQL (`' OR '1'='1`) y extracción de datos mediante UNION SELECT.

---

### Prueba TEST-SEC-010b: Eliminación de Validaciones Bean Validation

#### Código bajo prueba
`ms-pedidos360-orders/src/main/java/com/pedidos360/orders/dto/OrderItemRequestDto.java:23-29`:
```java
    // BASELINE-VULN [SEC-REQ-010]: sin validacion @Min(1) para cantidad
    private Integer cantidad;

    // BASELINE-VULN [SEC-REQ-010]: sin validacion @Positive para precioUnitario
    private BigDecimal precioUnitario;
```

#### Aserción automatizada
```python
assert any(l.startswith("@Min") for l in code_lines) and any(l.startswith("@Positive") for l in code_lines)
# AssertionError: Se eliminaron las anotaciones activas @Min(1) en cantidad y @Positive en precioUnitario.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Permite la creación de pedidos con cantidades negativas (`-5`) y precios negativos o cero.

---

### Prueba TEST-SEC-009: Uso de `dangerouslySetInnerHTML` en Frontend

#### Código bajo prueba
`frontend/src/App.tsx:352, 354, 471, 497, 503, 590`:
```tsx
{/* BASELINE-VULN [SEC-REQ-009]: renderizado inseguro de nombre de producto con dangerouslySetInnerHTML */}
<span dangerouslySetInnerHTML={{ __html: product.name }} />
```

#### Aserción automatizada
```python
assert "dangerouslySetInnerHTML" not in app_content
# AssertionError: App.tsx utiliza 'dangerouslySetInnerHTML' en 12 ubicaciones para renderizar datos dinámicos.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Ejecución de scripts en el navegador de los usuarios al visualizar productos o pedidos (Stored DOM XSS).

---

## 3. Plan de Remediación (Hacia Estado B)

1. En `ProductController`, sustituir `createNativeQuery` por consulta JPQL parametrizada o Spring Data JPA: `findByNombreContainingIgnoreCase(String q)`.
2. Restaurar `@Min(1)` y `@Positive` en `OrderItemRequestDto`.
3. Habilitar nuevamente los tests de validación en `ProductServiceCreationTest.java:100` retirando el `@Disabled`.
4. Reemplazar `dangerouslySetInnerHTML` por el binding nativo y seguro de JSX (`{product.name}`).

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa Security Tests Automatizados.*  
*Resultados JSON disponibles en:* `Evidences/SEC-CASE-04/tests/CASE-04-EV002-test-results.json`

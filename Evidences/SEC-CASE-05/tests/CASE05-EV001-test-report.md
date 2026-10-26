# SEC-CASE-05 — Configuración Segura
## Reporte de Security Tests Automatizados — Estado A (Baseline)

**Entorno de ejecución:** Suite Automatizada de Pruebas de Seguridad / CI-CD Verification  
**Fecha:** 2026-10-16  
**Resultado general Estado A:** ❌ **FAIL (4/4 pruebas fallidas — 100% FAIL)**  
**Objetivo de la línea base:** Demostrar el fallo de aserciones de configuración segura en CORS, secretos en texto claro, exposición de puertos y privilegios de contenedores.

---

## 1. Matriz de Ejecución de Pruebas Automatizadas

| ID Test | Requisito | Nombre de la Prueba | Aserción de Seguridad Esperada (Estado B) | Resultado en Estado A | Diagnóstico |
|---|---|---|---|:---:|---|
| **TEST-SEC-013** | SEC-REQ-013 | Política CORS restrictiva | No debe incluir el comodín `*` en orígenes permitidos | ❌ **FAIL** | `SecurityConfig.java:73` incluye `*` en `setAllowedOrigins`. |
| **TEST-SEC-012** | SEC-REQ-012 | Ausencia de secretos débiles por defecto | Prohibir el uso de `changeme`, `postgres` o `guest` en compose/configs | ❌ **FAIL** | Secretos débiles detectados en `docker-compose.yml`. |
| **TEST-SEC-014a** | SEC-REQ-014 | Aislamiento de puertos internos de backend | Los puertos 8086, 8087, 5432, 5672 no deben mapearse al host | ❌ **FAIL** | Puertos de backend expuestos directamente en `127.0.0.1`. |
| **TEST-SEC-014b** | SEC-REQ-014 | Contenedores ejecutándose como no-root | Todo Dockerfile debe declarar explícitamente la instrucción `USER` | ❌ **FAIL** | Dockerfiles sin usuario no-root especificado. |

---

## 2. Detalle de Pruebas y Evidencias de Fallo

### Prueba TEST-SEC-013: Detección de Wildcard en Política CORS

#### Código bajo prueba
`api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java:72-73`:
```java
// BASELINE-VULN [SEC-REQ-013]: configuracion CORS permisiva con comodin wildcard (*) y ausencia de Content-Security-Policy
config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000", "*"));
```

#### Aserción automatizada
```python
assert '"*"' not in sec_content or "setAllowedOrigins" not in sec_content
# AssertionError: SecurityConfig configura CORS con comodín '*' permitiendo cualquier origen arbitrario.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Permite a sitios web de terceros realizar peticiones a la API desde el navegador de un usuario autenticado.

---

### Prueba TEST-SEC-012: Detección de Secretos y Contraseñas Débiles

#### Código bajo prueba
`docker-compose.yml:21, 51, 102, 146`:
```yaml
# BASELINE-VULN [SEC-REQ-012]: credenciales por defecto predecibles en variables de entorno (postgres)
- POSTGRES_PASSWORD=postgres
# BASELINE-VULN [SEC-REQ-012]: secreto JWT por defecto debil (changeme)
- JWT_SECRET=changeme
# BASELINE-VULN [SEC-REQ-012]: credenciales por defecto de RabbitMQ (guest/guest)
- RABBITMQ_DEFAULT_PASS=guest
```

#### Aserción automatizada
```python
assert not any(s in compose_content for s in ["changeme", "guest", "postgres"])
# AssertionError: docker-compose.yml contiene secretos predecibles en claro: JWT_SECRET=changeme, RABBITMQ_DEFAULT_PASS=guest, POSTGRES_PASSWORD=postgres.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Falsificación trivial de tokens JWT HS256 y acceso no autorizado a bases de datos y colas de mensajería.

---

### Prueba TEST-SEC-014a: Puertos Internos Expuestos al Host

#### Código bajo prueba
`docker-compose.yml:13, 38, 142, 163`:
```yaml
ports:
  - "127.0.0.1:8086:8086" # ms-catalog
  - "127.0.0.1:8087:8087" # ms-orders
  - "127.0.0.1:5432:5432" # postgres
  - "127.0.0.1:5672:5672" # rabbitmq
```

#### Aserción automatizada
```python
assert not any(p in exposed_ports for p in ["8086", "8087", "5432", "5672", "15672"])
# AssertionError: Puertos internos de backend expuestos al host: 8086, 8087, 5432, 5672, 15672.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Salto del BFF para interactuar directamente con microservicios sin autenticación perimetral.

---

### Prueba TEST-SEC-014b: Contenedores Ejecutándose como Root

#### Código bajo prueba
`frontend/Dockerfile`, `api_cloud/Dockerfile`, `ms-pedidos360-catalog/Dockerfile`, etc.:
```dockerfile
# Ausencia de directiva USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Aserción automatizada
```python
assert "USER " in dockerfile_content
# AssertionError: Dockerfiles sin usuario no-root especificado.
```
- **Estado:** ❌ `FAIL`
- **Impacto:** Ejecución con privilegios máximos (UID 0) aumentando el riesgo de escape de contenedor.

---

## 3. Plan de Remediación (Hacia Estado B)

1. En `SecurityConfig`, restringir `setAllowedOrigins` a `http://localhost:8080` e incorporar encabezados CSP.
2. Reemplazar contraseñas predeterminadas por variables de entorno obligatorias con validación de longitud/complejidad mínima.
3. En `docker-compose.yml`, suprimir las secciones `ports` para servicios internos (`catalog`, `orders`, `postgres`, `rabbitmq`), manteniendo únicamente expuestos el BFF (8088) y el frontend (8080).
4. Agregar directivas `USER 10001` en todos los Dockerfiles de microservicios.

---

*Reporte generado como parte de Fase VII – Security Testing, Etapa Security Tests Automatizados.*  
*Resultados JSON disponibles en:* `Evidences/SEC-CASE-05/tests/CASE-05-EV002-test-results.json`

# Quickstart: Validación y Ejecución del Microservicio de Pedidos

**Feature**: CRUD de Pedidos y Ciclo de Vida (`001-orders-crud-lifecycle`)  
**Fecha**: 2026-09-03

Esta guía detalla los pasos para levantar el servicio localmente y validar los flujos de creación, consulta, filtrado y máquina de estados del microservicio de pedidos.

---

## 1. Requisitos Previos

- **Java Development Kit (JDK)**: 21 LTS instalado (`java -version`).
- **Apache Maven**: 3.9+ instalado (`mvn -version`).
- **Base de Datos Cloud / Local**: PostgreSQL en ejecución (o perfil con base de datos H2 para pruebas automatizadas).
- **Herramienta HTTP**: `curl`, `httpie` o Postman.

---

## 2. Configuración de Variables de Entorno

Exportar las variables de entorno de conexión a la base de datos (según el Principio II de la constitución):

```bash
# En entornos Linux / MacOS / Git Bash:
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=pedidos360_orders
export DB_USER=pedidos_app
export DB_PASSWORD=secret_db_password

# En PowerShell (Windows):
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="pedidos360_orders"
$env:DB_USER="pedidos_app"
$env:DB_PASSWORD="secret_db_password"
```

---

## 3. Compilación y Ejecución

```bash
# Compilar y ejecutar pruebas unitarias/integración
mvn clean test

# Iniciar la aplicación Spring Boot
mvn spring-boot:run
```

El servicio iniciará por defecto en `http://localhost:8080`.
La documentación Swagger UI estará disponible en:
- `http://localhost:8080/swagger-ui/index.html`

---

## 4. Escenarios de Validación End-to-End

### Escenario 1: Crear un nuevo pedido (Estado inicial `CREADO`)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "CLI-100",
    "items": [
      {
        "descripcion": "Hamburguesa Doble Queso",
        "cantidad": 2,
        "precioUnitario": 8.50
      },
      {
        "descripcion": "Papas Fritas Grandes",
        "cantidad": 1,
        "precioUnitario": 3.00
      }
    ]
  }'
```
- **Resultado Esperado**: HTTP `201 Created` con JSON que contiene `id: 1`, `estado: "CREADO"`, `total: 20.00` y marcas de tiempo.

---

### Escenario 2: Consultar pedido por ID
```bash
curl -X GET http://localhost:8080/api/orders/1
```
- **Resultado Esperado**: HTTP `200 OK` con el detalle completo del pedido.

---

### Escenario 3: Transición de Estado Válida (`CREADO` $\rightarrow$ `ACEPTADO`)
```bash
curl -X PUT http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{
    "nuevoEstado": "ACEPTADO"
  }'
```
- **Resultado Esperado**: HTTP `200 OK` con `estado: "ACEPTADO"` y fecha de actualización renovada.

---

### Escenario 4: Transición de Estado Inválida (Intento de saltar a `DESPACHADO` sin pasar por `EN_PREPARACION`)
```bash
curl -X PUT http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{
    "nuevoEstado": "DESPACHADO"
  }'
```
- **Resultado Esperado**: HTTP `409 Conflict` con cuerpo de error indicando:
  `"Transición inválida: No es posible cambiar el estado de ACEPTADO a DESPACHADO. Estados permitidos: [EN_PREPARACION, CANCELADO]"`.

---

### Escenario 5: Flujo Completo hasta `ENTREGADO` e intento de mutación
1. Avanzar a `EN_PREPARACION`: `PUT /api/orders/1/status` con `"nuevoEstado": "EN_PREPARACION"` $\rightarrow$ HTTP `200 OK`.
2. Avanzar a `DESPACHADO`: `PUT /api/orders/1/status` con `"nuevoEstado": "DESPACHADO"` $\rightarrow$ HTTP `200 OK`.
3. Avanzar a `ENTREGADO`: `PUT /api/orders/1/status` con `"nuevoEstado": "ENTREGADO"` $\rightarrow$ HTTP `200 OK`.
4. Intentar cancelar pedido entregado:
   ```bash
   curl -X PUT http://localhost:8080/api/orders/1/status \
     -H "Content-Type: application/json" \
     -d '{ "nuevoEstado": "CANCELADO" }'
   ```
   - **Resultado Esperado**: HTTP `409 Conflict` con error indicando que un pedido `ENTREGADO` no permite modificaciones.

---

### Escenario 6: Listar pedidos con filtros
```bash
curl -X GET "http://localhost:8080/api/orders?status=ENTREGADO&from=2026-09-01T00:00:00&to=2026-09-30T23:59:59"
```
- **Resultado Esperado**: HTTP `200 OK` con array de pedidos que coincidan con los criterios.

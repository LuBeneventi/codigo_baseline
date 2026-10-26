# ms-pedidos360-orders

Microservicio de gestión de pedidos y ciclo de vida de **Pedidos360**.

Construido bajo los principios constitucionales de arquitectura limpia en capas, Spring Boot 3+, Java 21, persistencia cloud con Spring Data JPA y documentación OpenAPI/Swagger.

---

## 🏛️ Principios Constitucionales del Proyecto

1. **Stack y Arquitectura en Capas**: Java 21 LTS y Spring Boot 3.3.x. Estructura desacoplada en paquetes `controller`, `dto`, `service`, `repository`, `entity`.
2. **Persistencia Cloud Desacoplada**: Spring Data JPA / Hibernate configurado mediante propiedades y variables de entorno externalizadas (sin credenciales en el repositorio).
3. **Máquina de Estados Determinista**:
   - Secuencia nominal: `CREADO` $\rightarrow$ `ACEPTADO` $\rightarrow$ `EN_PREPARACION` $\rightarrow$ `DESPACHADO` $\rightarrow$ `ENTREGADO`.
   - Cancelación: `CANCELADO` admitido desde cualquier estado previo a `ENTREGADO`.
   - Pedidos `ENTREGADO` y `CANCELADO` son estados terminales e inmutables.
   - Toda transición no autorizada responde con código **HTTP 409 Conflict** y mensaje explicativo.
4. **Documentación OpenAPI 3.0**: Swagger UI interactivo expuesto en `/swagger-ui.html`.
5. **Higiene del Repositorio**: `.gitignore` estricto excluyendo `target/`, metadatos de IDEs y secretos.

---

## 🚀 Compilación y Ejecución

```bash
# Compilar y ejecutar todas las pruebas automatizadas (con H2 en memoria)
mvn clean test

# Iniciar la aplicación localmente
mvn spring-boot:run
```

Una vez en ejecución, la consola interactiva Swagger UI estará disponible en:
- `http://localhost:8080/swagger-ui.html`
- Especificación OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## 📡 Endpoints REST Principales

### 1. Registrar un Pedido
- **POST** `/api/orders`
- **Request Body**:
  ```json
  {
    "clienteId": "CLI-98234",
    "items": [
      {
        "descripcion": "Pizza Pepperoni Familiar",
        "cantidad": 2,
        "precioUnitario": 12.50
      }
    ]
  }
  ```
- **Respuesta (201 Created)**: Retorna el pedido con `id`, estado inicial `CREADO`, total calculado (`25.00`) y fechas de auditoría.

### 2. Consultar Pedido por ID
- **GET** `/api/orders/{id}`
- **Respuesta**: `200 OK` con la información completa o `404 Not Found`.

### 3. Actualizar Estado de un Pedido
- **PUT** `/api/orders/{id}/status`
- **Request Body**:
  ```json
  {
    "nuevoEstado": "ACEPTADO"
  }
  ```
- **Respuestas**:
  - `200 OK`: Transición aceptada y persistida.
  - `409 Conflict`: Transición inválida según la máquina de estados.

### 4. Listar Pedidos con Filtros
- **GET** `/api/orders?status=CREADO&from=2026-09-01T00:00:00&to=2026-09-30T23:59:59`
- Filtros opcionales combinables por estado y rango temporal de creación.

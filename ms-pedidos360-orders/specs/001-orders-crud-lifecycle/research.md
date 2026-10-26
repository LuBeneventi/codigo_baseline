# Research: Arquitectura y Tecnologías del Microservicio de Pedidos

**Feature**: CRUD de Pedidos y Ciclo de Vida (`001-orders-crud-lifecycle`)  
**Fecha**: 2026-09-03  
**Estado**: Completado

## 1. Stack Tecnológico Base y Compatibilidad

- **Decisión**: Utilizar **Java 21 (LTS)** con **Spring Boot 3.3.x** y **Maven 3.9+**.
- **Rationale**: Java 21 ofrece soporte a largo plazo, Virtual Threads y mejoras de rendimiento. Spring Boot 3.x requiere Java 17+ de forma nativa y adopta las especificaciones de Jakarta EE 10.
- **Alternativas consideradas**:
  - *Java 17*: Aunque es LTS, Java 21 es el estándar establecido en la constitución del proyecto (Principio I) y ofrece mejores características de concurrencia y modernidad.
  - *Spring Boot 2.7*: Descartado por estar fuera de soporte general y no alinearse con Jakarta EE ni con la constitución.

---

## 2. Persistencia Cloud y Acceso a Datos

- **Decisión**: **Spring Data JPA** con **Hibernate** como ORM, compatible con PostgreSQL (o bases relacionales cloud estándar como AWS Aurora / Cloud SQL / Azure Database) y soporte para H2 en memoria durante pruebas de integración.
- **Configuración Externalizada**: Configuración en `application.yml` mediante placeholders de variables de entorno:
  - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.
  - Drivers JDBC configurados dinámicamente sin credenciales en duro en el repositorio (cumplimiento estricto del Principio II de la constitución).
- **Rationale**: Spring Data JPA simplifica drásticamente el desarrollo de repositorios mediante interfaces y query methods derivados (`findByEstadoAndFechaCreacionBetween`), además de proveer soporte para auditoría (`@CreationTimestamp`, `@UpdateTimestamp`).
- **Alternativas consideradas**:
  - *MyBatis / JDBC Template*: Requieren más código boilerplate para el mapeo relacional y gestión de relaciones de ítems (`@OneToMany`).
  - *Credenciales fijas en application.yml*: Prohibido explícitamente por la constitución y normas de seguridad cloud.

---

## 3. Implementación de la Máquina de Estados del Pedido

- **Decisión**: Implementar una clase de servicio especializada `OrderStateMachine` que encapsule la lógica de validación de transiciones de estado utilizando un mapa determinista de transiciones válidas (`Map<OrderStatus, Set<OrderStatus>>`).
- **Reglas de Transición**:
  - `CREADO` $\rightarrow$ `ACEPTADO`, `CANCELADO`
  - `ACEPTADO` $\rightarrow$ `EN_PREPARACION`, `CANCELADO`
  - `EN_PREPARACION` $\rightarrow$ `DESPACHADO`, `CANCELADO`
  - `DESPACHADO` $\rightarrow$ `ENTREGADO`, `CANCELADO`
  - `ENTREGADO` $\rightarrow$ Ninguna (estado terminal)
  - `CANCELADO` $\rightarrow$ Ninguna (estado terminal)
- **Manejo de Errores de Transición**: Si se intenta una transición no autorizada, `OrderStateMachine` lanza una excepción de dominio `InvalidStateTransitionException`, la cual es capturada por `@RestControllerAdvice` retornando HTTP **409 Conflict** con un mensaje explicativo (e.g. *"No es posible transicionar el pedido de CREADO a DESPACHADO. Estados permitidos: [ACEPTADO, CANCELADO]"*).
- **Rationale**: La máquina de estados basada en servicio y mapas de conjuntos es liviana, transparente, altamente testeable con pruebas unitarias puras y no añade la sobrecarga ni complejidad de configuración de dependencias pesadas como `spring-statemachine`.
- **Alternativas consideradas**:
  - *Spring State Machine Framework*: Sobredimensionado para una máquina lineal de 6 estados; añade complejidad innecesaria en la curva de aprendizaje y trazabilidad.
  - *Lógica dispersa en controladores*: Viola la separación de capas y el Principio I y III de la constitución.

---

## 4. Documentación de API y Contratos REST

- **Decisión**: Integrar **SpringDoc OpenAPI 2.6.x** (`springdoc-openapi-starter-webmvc-ui`) para exponer la especificación OpenAPI 3.0 en `/v3/api-docs` y la consola interactiva Swagger UI en `/swagger-ui.html`.
- **Rationale**: Cumple el Principio IV de la constitución. Genera documentación viva basada en anotaciones de código (`@Tag`, `@Operation`, `@ApiResponse`, `@Schema`), reduciendo la discrepancia entre el contrato y la implementación.
- **Alternativas consideradas**:
  - *Springfox*: Obsoleto, incompatible con Spring Boot 3+.
  - *Documentación manual Markdown/Postman*: Proensa a desincronización y requiere mantenimiento manual.

---

## 5. Manejo Centralizado de Excepciones y Formato de Errores

- **Decisión**: Implementar `GlobalExceptionHandler` anotado con `@RestControllerAdvice` para transformar excepciones de negocio en respuestas estructuradas (`ErrorResponseDto` o `ProblemDetail` RFC 7807):
  - `InvalidStateTransitionException` $\rightarrow$ **HTTP 409 Conflict**
  - `OrderNotFoundException` $\rightarrow$ **HTTP 404 Not Found**
  - `MethodArgumentNotValidException` / `IllegalArgumentException` $\rightarrow$ **HTTP 400 Bad Request**
  - `Exception` general no controlada $\rightarrow$ **HTTP 500 Internal Server Error**
- **Rationale**: Otorga consistencia a los clientes API y garantiza que las transiciones inválidas devuelvan el código 409 requerido por la especificación.

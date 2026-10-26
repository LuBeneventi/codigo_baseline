# Tasks: CRUD de Pedidos y Ciclo de Vida

**Feature**: `001-orders-crud-lifecycle` | **Date**: 2026-09-03 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialización del proyecto Java/Maven con Spring Boot 3.3.x, dependencias base y configuración de entornos.

- [X] T001 Initialize Maven project descriptor with Java 21, Spring Boot 3.3.x and dependencies (Web, Data JPA, Validation, SpringDoc OpenAPI, PostgreSQL, H2, Test) in pom.xml
- [X] T002 [P] Configure .gitignore to exclude target/, .idea/, *.iml, .vscode/, credentials and environment files per constitution in .gitignore
- [X] T003 [P] Create Spring Boot application main entrypoint in src/main/java/com/pedidos360/orders/MsPedidos360OrdersApplication.java
- [X] T004 [P] Configure application settings with externalized cloud datasource environment variables in src/main/resources/application.yml
- [X] T005 [P] Configure test application settings with H2 in-memory datasource for automated testing in src/test/resources/application-test.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entidades JPA del agregado de pedidos, enumeración de estados, repositorio base y manejo global de excepciones.

**⚠️ CRITICAL**: Ninguna historia de usuario puede comenzar hasta que esta fase esté completada.

- [X] T006 [P] Implement OrderStatus enumeration with states (CREADO, ACEPTADO, EN_PREPARACION, DESPACHADO, ENTREGADO, CANCELADO) in src/main/java/com/pedidos360/orders/entity/OrderStatus.java
- [X] T007 [P] Implement OrderItem JPA entity with validation constraints and relationship mapping in src/main/java/com/pedidos360/orders/entity/OrderItem.java
- [X] T008 Implement Order JPA entity with aggregate root mappings, @OneToMany cascade items, audit timestamps, and total in src/main/java/com/pedidos360/orders/entity/Order.java
- [X] T009 [P] Implement OrderRepository interface extending JpaRepository<Order, Long> and JpaSpecificationExecutor<Order> in src/main/java/com/pedidos360/orders/repository/OrderRepository.java
- [X] T010 [P] Create common error response DTO ErrorResponseDto in src/main/java/com/pedidos360/orders/dto/ErrorResponseDto.java
- [X] T011 [P] Create domain exceptions OrderNotFoundException and InvalidStateTransitionException in src/main/java/com/pedidos360/orders/service/exception/OrderNotFoundException.java and src/main/java/com/pedidos360/orders/service/exception/InvalidStateTransitionException.java
- [X] T012 Implement GlobalExceptionHandler with @RestControllerAdvice mapping InvalidStateTransitionException to HTTP 409 Conflict, OrderNotFoundException to HTTP 404, and validation errors to HTTP 400 in src/main/java/com/pedidos360/orders/controller/exception/GlobalExceptionHandler.java

**Checkpoint**: Base de datos, entidades, repositorio y manejo de errores listos. La implementación de historias de usuario puede comenzar.

---

## Phase 3: User Story 1 - Creación y Consulta Individual de Pedidos (Priority: P1) 🎯 MVP

**Goal**: Permitir registrar un nuevo pedido con cliente e ítems en estado inicial CREADO, calculando el importe total y marcas de tiempo, y consultar pedidos por su identificador único.

**Independent Test**: Registrar un pedido mediante `POST /api/orders` y verificar que retorne HTTP 201 con ID asignado, estado `CREADO` y total calculado; posteriormente consultar `GET /api/orders/{id}` y verificar que retorne HTTP 200 con los datos exactos del pedido.

### Tests for User Story 1

- [X] T013 [P] [US1] Create unit tests for order creation, calculation of totals, and order retrieval in src/test/java/com/pedidos360/orders/service/OrderServiceTest.java
- [X] T014 [P] [US1] Create web integration tests for POST /api/orders and GET /api/orders/{id} in src/test/java/com/pedidos360/orders/controller/OrderControllerTest.java

### Implementation for User Story 1

- [X] T015 [P] [US1] Implement DTOs for creation and retrieval (OrderItemRequestDto, OrderItemResponseDto, OrderRequestDto, OrderResponseDto) in src/main/java/com/pedidos360/orders/dto/OrderRequestDto.java
- [X] T016 [US1] Implement OrderService interface and OrderServiceImpl logic for creating orders (validating items, computing total, setting initial CREADO status) and retrieving by ID in src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java
- [X] T017 [US1] Implement REST controller endpoints POST /api/orders and GET /api/orders/{id} with OpenAPI documentation annotations in src/main/java/com/pedidos360/orders/controller/OrderController.java

**Checkpoint**: User Story 1 completamente funcional y testeable de forma independiente (MVP alcanzado).

---

## Phase 4: User Story 2 - Control del Ciclo de Vida y Transiciones de Estado (Priority: P1)

**Goal**: Administrar las transiciones válidas del ciclo de vida del pedido mediante `OrderStateMachine`, impidiendo transiciones no permitidas o modificaciones sobre pedidos entregados/cancelados y respondiendo HTTP 409 Conflict.

**Independent Test**: Crear un pedido y transicionar secuencialmente `CREADO` $\rightarrow$ `ACEPTADO` $\rightarrow$ `EN_PREPARACION` $\rightarrow$ `DESPACHADO` $\rightarrow$ `ENTREGADO`. Probar transiciones inválidas (e.g. `CREADO` a `DESPACHADO` o cancelar un pedido `ENTREGADO`) y verificar que retornen HTTP 409 Conflict con mensaje explicativo.

### Tests for User Story 2

- [X] T018 [P] [US2] Implement unit tests for OrderStateMachine validating allowed transitions and verifying 409 Conflict exceptions on illegal transitions in src/test/java/com/pedidos360/orders/service/OrderStateMachineTest.java
- [X] T019 [P] [US2] Implement controller integration tests for PUT /api/orders/{id}/status testing valid transitions (200 OK) and invalid transitions (409 Conflict) in src/test/java/com/pedidos360/orders/controller/OrderStatusControllerTest.java

### Implementation for User Story 2

- [X] T020 [P] [US2] Create status change request DTO UpdateStatusRequestDto with validation in src/main/java/com/pedidos360/orders/dto/UpdateStatusRequestDto.java
- [X] T021 [US2] Implement OrderStateMachine interface and OrderStateMachineImpl with transition validation rules (CREADO->ACEPTADO->EN_PREPARACION->DESPACHADO->ENTREGADO, CANCELADO except from ENTREGADO) in src/main/java/com/pedidos360/orders/service/impl/OrderStateMachineImpl.java
- [X] T022 [US2] Add updateOrderStatus(Long id, OrderStatus nuevoEstado) method to OrderService and OrderServiceImpl invoking OrderStateMachine before saving updated status and timestamp in src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java
- [X] T023 [US2] Implement REST endpoint PUT /api/orders/{id}/status with OpenAPI annotations in src/main/java/com/pedidos360/orders/controller/OrderController.java

**Checkpoint**: User Story 1 y 2 operativas e integradas. Máquina de estados validada y protegida ante conflictos.

---

## Phase 5: User Story 3 - Listado y Filtrado de Pedidos (Priority: P2)

**Goal**: Proveer endpoint de consulta masiva con filtros opcionales por estado (`status`) y rango de fechas de creación (`from`, `to`).

**Independent Test**: Ejecutar consultas a `GET /api/orders` con combinaciones de filtros (`status=EN_PREPARACION`, rango de fechas `from` y `to`) y corroborar que la respuesta retorne exactamente los pedidos coincidentes con código HTTP 200 OK.

### Tests for User Story 3

- [X] T024 [P] [US3] Create unit and repository tests for queries by status and creation date range in src/test/java/com/pedidos360/orders/repository/OrderRepositoryTest.java
- [X] T025 [P] [US3] Create controller integration tests for GET /api/orders with query parameters (status, from, to) in src/test/java/com/pedidos360/orders/controller/OrderFilterControllerTest.java

### Implementation for User Story 3

- [X] T026 [US3] Implement repository query methods or dynamic JPA specifications for status and date range filtering in src/main/java/com/pedidos360/orders/repository/OrderRepository.java
- [X] T027 [US3] Implement listOrders(OrderStatus status, LocalDateTime from, LocalDateTime to) method in OrderService and OrderServiceImpl in src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java
- [X] T028 [US3] Implement REST endpoint GET /api/orders with @RequestParam and OpenAPI documentation in src/main/java/com/pedidos360/orders/controller/OrderController.java

**Checkpoint**: Todas las historias de usuario (US1, US2, US3) funcionales y testeadas.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verificación de contratos OpenAPI, validación contra quickstart y documentación general.

- [X] T029 [P] Verify OpenAPI 3.0 / Swagger UI documentation contract matching contracts/orders-api.yaml at /swagger-ui/index.html
- [X] T030 [P] Create comprehensive README.md with architecture overview, environment setup, and execution instructions in README.md
- [X] T031 Execute end-to-end scenarios per quickstart.md and verify all tests pass with mvn clean test

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias; inicia de inmediato.
- **Foundational (Phase 2)**: Depende de Phase 1 completada. Bloquea la implementación de todas las historias de usuario.
- **User Story 1 (Phase 3)**: Depende de Phase 2. Establece el MVP funcional base.
- **User Story 2 (Phase 4)**: Depende de Phase 2 y se integra sobre las órdenes de User Story 1.
- **User Story 3 (Phase 5)**: Depende de Phase 2 y consulta las órdenes creadas en User Story 1 y 2.
- **Polish (Phase 6)**: Depende de la finalización de las historias de usuario seleccionadas.

### User Story Dependencies

- **User Story 1 (P1)**: Inicia tras Foundational (Phase 2). No depende de otras historias.
- **User Story 2 (P1)**: Inicia tras Foundational (Phase 2). Reutiliza las entidades `Order` y endpoints creados en US1.
- **User Story 3 (P2)**: Inicia tras Foundational (Phase 2). Puede desarrollarse en paralelo con US2 o de forma secuencial.

### Within Each User Story

- Pruebas unitarias/integración se diseñan y ejecutan primero.
- DTOs y validaciones antes de los servicios.
- Lógica de negocio en servicios antes de los endpoints en controladores.
- Anotaciones OpenAPI integradas en los controladores.

### Parallel Opportunities

- En Phase 1: `T002`, `T003`, `T004`, `T005` pueden desarrollarse en paralelo una vez creado el `pom.xml` (`T001`).
- En Phase 2: `T006` (Enum), `T007` (OrderItem), `T010` (Error DTO) y `T011` (Excepciones) pueden ejecutarse en paralelo.
- En Phase 3 (US1): Las pruebas `T013` y `T014`, junto con los DTOs `T015`, pueden desarrollarse en paralelo.
- En Phase 4 (US2): Las pruebas `T018` y `T019`, y el DTO `T020`, pueden desarrollarse en paralelo.
- En Phase 5 (US3): Las pruebas `T024` y `T025` pueden desarrollarse en paralelo.
- En Phase 6: `T029` y `T030` pueden desarrollarse en paralelo.

---

## Parallel Example: User Story 1

```bash
# Lanzar diseño de pruebas para User Story 1 en paralelo:
Task: "Create unit tests for order creation in src/test/java/com/pedidos360/orders/service/OrderServiceTest.java"
Task: "Create web integration tests in src/test/java/com/pedidos360/orders/controller/OrderControllerTest.java"

# Lanzar DTOs en paralelo con las entidades base:
Task: "Implement DTOs for creation and retrieval in src/main/java/com/pedidos360/orders/dto/OrderRequestDto.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar **Phase 1: Setup** (Maven, dependencias, .gitignore y configuración).
2. Completar **Phase 2: Foundational** (Entidades `Order`, `OrderItem`, `OrderStatus`, repositorio y excepciones).
3. Completar **Phase 3: User Story 1** (Registro y consulta individual de pedidos).
4. **Validación**: Ejecutar `mvn clean test` y probar endpoints `POST /api/orders` y `GET /api/orders/{id}`. (¡MVP listo para demostración!).

### Incremental Delivery

1. Setup + Foundational $\rightarrow$ Base arquitectónica sólida lista.
2. Añadir User Story 1 $\rightarrow$ Crear y consultar pedidos (MVP).
3. Añadir User Story 2 $\rightarrow$ Máquina de estados completa con validación 409 Conflict.
4. Añadir User Story 3 $\rightarrow$ Listado y filtrado avanzado por estado y rango de fechas.
5. Polish $\rightarrow$ Validación end-to-end con `quickstart.md` y documentación OpenAPI verificada.

# Feature Specification: CRUD de Pedidos y Ciclo de Vida

**Feature Branch**: `001-orders-crud-lifecycle`

**Created**: 2026-09-03

**Status**: Draft

**Input**: User description: "Construye el CRUD de pedidos del microservicio ms-pedidos360-orders. Un pedido tiene: id, clienteId, lista de items, total, estado y fechas de creación/actualización. Se puede crear un pedido (queda en estado CREADO), consultarlo por id, listarlo con filtros por estado y rango de fechas, y cambiar su estado. El cambio de estado debe respetar la máquina de estados: no se puede despachar sin antes aceptar, no se puede modificar un pedido ya entregado. Si se intenta una transición de estado inválida, el sistema responde con un error claro (409 Conflict) explicando por qué no es posible. Todos los pedidos se persisten en una base de datos cloud."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Creación y Consulta Individual de Pedidos (Priority: P1)

Como cliente o sistema emisor de pedidos, quiero registrar un nuevo pedido con su detalle de ítems y consultar su información completa usando su identificador único para dar seguimiento y confirmar la compra.

**Why this priority**: Es la funcionalidad nuclear (MVP) del microservicio. Sin la capacidad de registrar y consultar pedidos individuales, no es posible ejecutar ninguna otra operación ni avanzar en el ciclo de vida.

**Independent Test**: Se registra un pedido con un cliente e ítems válidos; el sistema responde con el pedido creado en estado `CREADO`, total calculado y fechas de auditoría. Posteriormente se consulta por el identificador asignado y se verifica la coincidencia exacta de los datos.

**Acceptance Scenarios**:

1. **Given** datos válidos de un pedido (`clienteId`, lista de ítems con descripción, cantidad positiva y precio unitario positivo), **When** se solicita la creación del pedido, **Then** el pedido queda registrado en estado `CREADO`, con su importe total calculado consistentemente, marcas temporales de creación y actualización, y un identificador único.
2. **Given** un pedido existente en el sistema con un identificador determinado, **When** se consulta dicho pedido por su identificador, **Then** el sistema retorna la información completa del pedido con sus ítems, total, estado y fechas.
3. **Given** un identificador que no corresponde a ningún pedido registrado, **When** se realiza la consulta por dicho identificador, **Then** el sistema informa que el pedido no fue encontrado.

---

### User Story 2 - Control del Ciclo de Vida y Transiciones de Estado (Priority: P1)

Como operador logístico o sistema de procesamiento, quiero actualizar el estado de un pedido a lo largo de su ciclo de vida respetando las transiciones válidas para garantizar la integridad operativa y evitar despachos prematuros o modificaciones sobre pedidos entregados.

**Why this priority**: La máquina de estados es la regla de negocio crítica del dominio. Garantiza que los pedidos sigan el flujo operacional estricto (`CREADO` -> `ACEPTADO` -> `EN_PREPARACION` -> `DESPACHADO` -> `ENTREGADO`) y que la cancelación solo se admita en pedidos no entregados.

**Independent Test**: Se crea un pedido y se progresa secuencialmente por cada estado válido. Se comprueba que cualquier intento de transición inválida (como saltar de `CREADO` a `DESPACHADO` o cancelar un pedido `ENTREGADO`) sea rechazado con un mensaje de conflicto (409 Conflict).

**Acceptance Scenarios**:

1. **Given** un pedido en estado `CREADO`, **When** el operador solicita pasarlo a `ACEPTADO`, **Then** el sistema actualiza el estado a `ACEPTADO` y renueva la fecha de última actualización.
2. **Given** un pedido en estado `ACEPTADO`, **When** se solicita pasarlo a `EN_PREPARACION`, **Then** el sistema actualiza el estado a `EN_PREPARACION`.
3. **Given** un pedido en estado `EN_PREPARACION`, **When** se solicita pasarlo a `DESPACHADO`, **Then** el sistema actualiza el estado a `DESPACHADO`.
4. **Given** un pedido en estado `DESPACHADO`, **When** se solicita pasarlo a `ENTREGADO`, **Then** el sistema actualiza el estado a `ENTREGADO`.
5. **Given** un pedido en cualquiera de los estados `CREADO`, `ACEPTADO`, `EN_PREPARACION` o `DESPACHADO`, **When** se solicita su cancelación, **Then** el estado pasa a `CANCELADO`.
6. **Given** un pedido en estado `CREADO`, **When** se intenta cambiar su estado directamente a `DESPACHADO` sin antes haber sido aceptado y preparado, **Then** el sistema rechaza la solicitud indicando que la transición es inválida (409 Conflict).
7. **Given** un pedido en estado `ENTREGADO`, **When** se intenta cambiar su estado a `CANCELADO` o a cualquier otro estado, **Then** el sistema rechaza la solicitud indicando que un pedido ya entregado no permite modificaciones de estado (409 Conflict).

---

### User Story 3 - Listado y Filtrado de Pedidos (Priority: P2)

Como gestor o supervisor de operaciones, quiero listar los pedidos del sistema aplicando filtros por estado y por rango de fechas de creación para monitorear la demanda y gestionar el trabajo pendiente.

**Why this priority**: Permite la supervisión y operación masiva sobre los pedidos registrados una vez que el ciclo individual de pedidos es funcional.

**Independent Test**: Se consultan pedidos especificando un estado determinado (e.g. `EN_PREPARACION`) y un intervalo de fechas; se corrobora que el resultado contenga únicamente los pedidos que cumplan ambos filtros.

**Acceptance Scenarios**:

1. **Given** múltiples pedidos en diversos estados en el sistema, **When** se solicita el listado filtrando por un estado específico, **Then** el sistema retorna únicamente los pedidos que coinciden con dicho estado.
2. **Given** pedidos creados en diferentes fechas, **When** se solicita el listado especificando un rango de fechas (fecha inicial y fecha final), **Then** el sistema retorna únicamente los pedidos creados dentro del intervalo indicado.
3. **Given** una búsqueda con filtros por estado y rango de fechas combinados, **When** ningún pedido coincide con el criterio, **Then** el sistema retorna una lista vacía sin producir errores.

---

### Edge Cases

- ¿Qué sucede si se intenta crear un pedido con una lista de ítems vacía o ítems con cantidad menor o igual a cero? El sistema debe rechazar la creación mediante un mensaje claro de validación de datos de entrada.
- ¿Qué sucede si se solicita una transición hacia el mismo estado en el que ya se encuentra el pedido? El sistema debe rechazar la operación indicando que el pedido ya se encuentra en dicho estado.
- ¿Qué sucede si se intenta cambiar el estado de un pedido que ya está en estado `CANCELADO`? El sistema debe rechazar la solicitud informando que un pedido cancelado es terminal y no permite transiciones adicionales.
- ¿Qué sucede si el rango de fechas proporcionado tiene una fecha de inicio posterior a la fecha de fin? El sistema debe retornar un mensaje de error indicando la inconsistencia en el rango de fechas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de un nuevo pedido proporcionando el identificador del cliente (`clienteId`) y una lista no vacía de ítems con descripción, cantidad positiva y precio unitario positivo.
- **FR-002**: Al registrar un nuevo pedido, el sistema DEBE asignarle automáticamente el estado inicial `CREADO`, generar un identificador único inmutable y asignar las fechas de creación y actualización correspondientes.
- **FR-003**: El sistema DEBE calcular y registrar de manera consistente el importe total del pedido a partir de la sumatoria de sus ítems (cantidad multiplicada por precio unitario).
- **FR-004**: El sistema DEBE permitir consultar la información completa de un pedido específico a partir de su identificador único.
- **FR-005**: El sistema DEBE permitir listar pedidos permitiendo opcionalmente filtrar por estado (`CREADO`, `ACEPTADO`, `EN_PREPARACION`, `DESPACHADO`, `ENTREGADO`, `CANCELADO`) y por rango de fechas de creación (fecha desde / fecha hasta).
- **FR-006**: El sistema DEBE controlar las transiciones de estado de acuerdo con la máquina de estados oficial:
  - Transición secuencial estándar: `CREADO` -> `ACEPTADO` -> `EN_PREPARACION` -> `DESPACHADO` -> `ENTREGADO`.
  - Transición de cancelación: Permitida hacia `CANCELADO` desde `CREADO`, `ACEPTADO`, `EN_PREPARACION` y `DESPACHADO`.
- **FR-007**: El sistema DEBE rechazar cualquier intento de cambiar el estado de un pedido que se encuentre en estado final `ENTREGADO` o `CANCELADO`.
- **FR-008**: Si se intenta realizar una transición de estado inválida, el sistema DEBE responder con un error claro de conflicto (409 Conflict), detallando el estado actual, el estado solicitado y el motivo por el cual no es posible realizar la transición.
- **FR-009**: Cada cambio de estado exitoso DEBE refrescar automáticamente la fecha de última actualización del pedido.
- **FR-010**: Todos los pedidos y sus ítems DEBEN almacenarse de forma persistente y duradera en una base de datos cloud.

### Key Entities

- **Pedido**: Entidad principal que representa una orden de compra. Posee los atributos: `id` (identificador único), `clienteId` (identificador del cliente), `items` (colección de ítems que componen el pedido), `total` (monto total acumulado), `estado` (estado actual en el ciclo de vida), `fechaCreacion` (marca temporal de alta) y `fechaActualizacion` (marca temporal de la última modificación).
- **Ítem de Pedido**: Representa una línea individual dentro del pedido. Posee los atributos: `id` (identificador del ítem), `descripcion` (detalle del producto o servicio), `cantidad` (número entero mayor a cero), `precioUnitario` (monto monetario positivo) y `subtotal` (cantidad multiplicada por precio unitario).
- **Estado de Pedido**: Enumeración de los estados válidos del ciclo de vida: `CREADO`, `ACEPTADO`, `EN_PREPARACION`, `DESPACHADO`, `ENTREGADO`, `CANCELADO`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los pedidos creados exitosamente quedan registrados en estado `CREADO` con total calculado correctamente y marcas de tiempo válidas.
- **SC-002**: El 100% de los intentos de transiciones de estado no permitidas (incluyendo saltos de etapa o modificaciones sobre pedidos entregados/cancelados) son interceptados y devuelven un error explicativo de conflicto (409 Conflict).
- **SC-003**: La consulta individual de un pedido por identificador retorna la información completa y precisa en menos de 1 segundo bajo condiciones operativas normales.
- **SC-004**: Los filtros de listado por estado y rango de fechas garantizan un 100% de coincidencia exacta con los criterios suministrados, sin excluir registros pertinentes ni incluir registros no coincidentes.

## Assumptions

- La autenticación de clientes y validación de identidad se efectúa previamente en la capa perimetral o gateway; el microservicio asume que el `clienteId` recibido es legítimo.
- Todos los valores monetarios se gestionan en una misma divisa base estándar dentro de la plataforma.
- La gestión física del stock o inventario es responsabilidad de servicios externos complementarios; este microservicio se enfoca en el registro y ciclo de vida del pedido.
- El identificador del pedido (`id`) es inmutable una vez creado.

# Implementation Plan: CRUD de Pedidos y Ciclo de Vida

**Branch**: `001-orders-crud-lifecycle` | **Date**: 2026-09-03 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-orders-crud-lifecycle/spec.md`

## Summary

Implementación del microservicio `ms-pedidos360-orders` para la gestión integral del ciclo de vida y operaciones CRUD de pedidos en Pedidos360.
La solución adopta una arquitectura en capas desacoplada con **Spring Boot 3.3.x** y **Java 21**, persistencia relacional cloud mediante **Spring Data JPA**, validación estricta del flujo de estados a través de una clase de servicio especializada `OrderStateMachine` (que responde con HTTP `409 Conflict` ante transiciones no permitidas), documentación viva de contratos vía **SpringDoc OpenAPI 3.0 / Swagger**, y configuración de conexión externalizada mediante variables de entorno en `application.yml`.

## Technical Context

**Language/Version**: Java 21 (LTS)  
**Primary Dependencies**: Spring Boot 3.3.x (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `springdoc-openapi-starter-webmvc-ui` 2.6.x)  
**Storage**: Base de datos relacional Cloud (PostgreSQL) con Hibernate como proveedor JPA; base de datos H2 en memoria para suites de pruebas automatizadas  
**Testing**: JUnit 5, Mockito, `spring-boot-starter-test` (MockMvc, `@DataJpaTest`, `@SpringBootTest`)  
**Target Platform**: Linux Container (Docker / Cloud Kubernetes / AWS / GCP)  
**Project Type**: RESTful Web Service / Microservice  
**Performance Goals**: Tiempo de respuesta p95 < 200ms para operaciones CRUD y cambios de estado bajo carga nominal  
**Constraints**: 
- Prohibición estricta de credenciales en el repositorio; inyección vía variables de entorno (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`)
- Transiciones de estado estrictamente gobernadas: `CREADO` $\rightarrow$ `ACEPTADO` $\rightarrow$ `EN_PREPARACION` $\rightarrow$ `DESPACHADO` $\rightarrow$ `ENTREGADO`, o `CANCELADO` desde cualquier estado previo a `ENTREGADO`
- Transiciones inválidas retornan HTTP 409 Conflict con mensaje explícito de causa  
**Scale/Scope**: Microservicio nuclear transaccional de pedidos para la plataforma Pedidos360

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Requerimiento Constitucional | Estado en Diseño | Evaluación |
|---|---|---|---|
| **Principio I** | Java 21, Spring Boot 3+, arquitectura en capas (`controller`, `service`, `repository`, `entity`) sin accesos cruzados | Estructura de paquetes estricta con DTOs, interfaces de servicio, repositorios JPA e implementación desacoplada | **PASS** |
| **Principio II** | Persistencia cloud con Spring Data JPA; propiedades de conexión externalizadas (no hardcodeadas) | `application.yml` configurado con variables de entorno (`${DB_HOST:...}`, etc.); soporte para datasource PostgreSQL | **PASS** |
| **Principio III** | Máquina de estados: `CREADO` $\rightarrow$ `ACEPTADO` $\rightarrow$ `EN_PREPARACION` $\rightarrow$ `DESPACHADO` $\rightarrow$ `ENTREGADO`, cancelación permitida salvo en `ENTREGADO`; 409 Conflict ante infracciones | Encapsulado en `OrderStateMachine` y manejador global de excepciones mapeando `InvalidStateTransitionException` a 409 Conflict | **PASS** |
| **Principio IV** | Documentación OpenAPI / Swagger expuesta | Integración de SpringDoc OpenAPI UI en `/swagger-ui.html` y contrato en `contracts/orders-api.yaml` | **PASS** |
| **Principio V** | Solo artefactos Java/Maven en repositorio; `.gitignore` configurado excluyendo `target/`, `.idea/`, credenciales | `.gitignore` estándar Maven con exclusiones de compilación, metadatos de IDEs y secretos | **PASS** |

*Resultado*: **TODOS LOS GATES APROBADOS (5/5). Ninguna violación detectada.**

## Project Structure

### Documentation (this feature)

```text
specs/001-orders-crud-lifecycle/
├── spec.md              # Especificación de requisitos funcionales y casos de uso
├── checklists/
│   └── requirements.md  # Checklist de calidad de requisitos
├── plan.md              # Plan de implementación técnica (este documento)
├── research.md          # Investigación técnica y decisiones arquitectónicas
├── data-model.md        # Modelo de datos, diagramas ER y matriz de estados
├── quickstart.md        # Guía de validación y ejecución end-to-end
└── contracts/
    └── orders-api.yaml  # Contrato OpenAPI 3.0 de endpoints REST
```

### Source Code (repository root)

```text
pom.xml
.gitignore
README.md
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── pedidos360/
│   │           └── orders/
│   │               ├── MsPedidos360OrdersApplication.java
│   │               ├── controller/
│   │               │   ├── OrderController.java
│   │               │   └── exception/
│   │               │       └── GlobalExceptionHandler.java
│   │               ├── dto/
│   │               │   ├── OrderRequestDto.java
│   │               │   ├── OrderResponseDto.java
│   │               │   ├── OrderItemRequestDto.java
│   │               │   ├── OrderItemResponseDto.java
│   │               │   ├── UpdateStatusRequestDto.java
│   │               │   └── ErrorResponseDto.java
│   │               ├── service/
│   │               │   ├── OrderService.java
│   │               │   ├── OrderStateMachine.java
│   │               │   ├── impl/
│   │               │   │   ├── OrderServiceImpl.java
│   │               │   │   └── OrderStateMachineImpl.java
│   │               │   └── exception/
│   │               │       ├── OrderNotFoundException.java
│   │               │       └── InvalidStateTransitionException.java
│   │               ├── repository/
│   │               │   └── OrderRepository.java
│   │               └── entity/
│   │                   ├── Order.java
│   │                   ├── OrderItem.java
│   │                   └── OrderStatus.java
│   └── resources/
│       ├── application.yml
│       └── application-test.yml
└── test/
    └── java/
        └── com/
            └── pedidos360/
                └── orders/
                    ├── controller/
                    │   └── OrderControllerTest.java
                    ├── service/
                    │   ├── OrderStateMachineTest.java
                    │   └── OrderServiceTest.java
                    └── repository/
                        └── OrderRepositoryTest.java
```

**Structure Decision**: Se adopta la estructura estándar de microservicio Java/Maven con Spring Boot en capas desacopladas (`controller`, `dto`, `service`, `repository`, `entity`), ubicando las clases de infraestructura y utilidades de forma transversal para garantizar mantenibilidad e independencia de pruebas.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No se registran violaciones a la constitución. El diseño arquitectónico cumple al 100% con todos los principios ratificados.*

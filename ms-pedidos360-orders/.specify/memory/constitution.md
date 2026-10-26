<!--
Sync Impact Report:
- Version change: Template Draft -> 1.0.0
- List of modified principles:
  - [PRINCIPLE_1_NAME] -> I. Arquitectura en Capas y Stack Base (Spring Boot 3+ / Java 21)
  - [PRINCIPLE_2_NAME] -> II. Persistencia Cloud Desacoplada y Externalización de Credenciales
  - [PRINCIPLE_3_NAME] -> III. Ciclo de Vida y Máquina de Estados Estricta del Pedido
  - [PRINCIPLE_4_NAME] -> IV. Documentación de API OpenAPI / Swagger
  - [PRINCIPLE_5_NAME] -> V. Control de Versiones Limpio e Higiene del Repositorio
- Added sections:
  - Estándares Técnicos y Restricciones del Servicio
  - Flujo de Trabajo y Criterios de Aceptación
- Removed sections:
  - None
- Follow-up TODOs:
  - None
-->

# Pedidos360 - Microservicio de Pedidos Constitution

## Core Principles

### I. Arquitectura en Capas y Stack Base (Spring Boot 3+ / Java 21)
El microservicio DEBE desarrollarse utilizando **Java 21** (LTS) y el framework **Spring Boot 3.x**.
La base del código DEBE estructurarse siguiendo una arquitectura en capas estricta con
responsabilidades desacopladas:
- **`controller`**: Controladores REST para la recepción de peticiones HTTP, validación de
  parámetros de entrada y serialización de respuestas DTO.
- **`service`**: Capa que encapsula la lógica del negocio, las reglas del dominio y la
  gestión transaccional.
- **`repository`**: Interfaces basadas en Spring Data JPA para el acceso a la base de datos.
- **`entity` / `model`**: Modelos de persistencia mapeados a tablas de base de datos.

Las capas superiores NO DEBEN eludir capas intermedias (los Controllers no interactúan
directamente con Repositories; toda operación debe canalizarse mediante la capa Service).
*Rationale*: Asegura mantenibilidad, modularidad, alta testabilidad unitaria y estandarización
corporativa utilizando características modernas de Java 21 y Spring Boot 3+.

### II. Persistencia Cloud Desacoplada y Externalización de Credenciales
El acceso a datos DEBE implementarse a través de **Spring Data JPA** (con Hibernate) sobre una base
de datos cloud. Todas las propiedades de conexión (URL JDBC, usuario, contraseña, pool de conexiones)
DEBEN suministrarse mediante variables de entorno o archivos de configuración de entorno
(`application.yml` / `application.properties`).
QUEDA ESTRICTAMENTE PROHIBIDO (MUST NOT) incluir valores de conexión, credenciales o secretos en duro
(hardcoded) en el código fuente o en archivos sometidos a control de versiones.
*Rationale*: Garantiza la portabilidad entre entornos de ejecución (desarrollo, testing, staging y
cloud production) y salvaguarda la seguridad del sistema frente a filtraciones de secretos.

### III. Ciclo de Vida y Máquina de Estados Estricta del Pedido
El ciclo de vida de un pedido DEBE estar gobernado por una máquina de estados determinista y
controlada en la capa de negocio (`service`). Las transiciones de estado permitidas se restringen a:
- Flujo nominal secuencial: `CREADO` -> `ACEPTADO` -> `EN_PREPARACION` -> `DESPACHADO` -> `ENTREGADO`.
- Cancelación: Transición a `CANCELADO` permitida desde cualquier estado (`CREADO`, `ACEPTADO`,
  `EN_PREPARACION`, `DESPACHADO`), EXCEPTO desde el estado final `ENTREGADO`.

Cualquier solicitud de transición fuera de estas reglas DEBE ser rechazada de forma explícita mediante
excepciones de negocio y traducida a un código HTTP de error consistente (e.g., 409 Conflict o
400 Bad Request).
*Rationale*: Protege la consistencia de los pedidos y previene inconsistencias operativas o
logísticas en la plataforma Pedidos360.

### IV. Documentación de API OpenAPI / Swagger
Todos los endpoints REST expuestos por el microservicio DEBEN estar completamente documentados e
interactivos mediante la especificación **OpenAPI 3.0 / Swagger** (SpringDoc OpenAPI).
Cada operación DEBE detallar su descripción, parámetros de ruta/consulta, cuerpo de petición (DTO),
códigos de respuesta HTTP esperados (200, 201, 400, 404, 409, 500) y esquemas de error.
La interfaz visual Swagger UI DEBE estar disponible y accesible en las rutas estándar para facilitar
la prueba y el consumo por parte de clientes y servicios dependientes.
*Rationale*: Facilita la interoperabilidad en la arquitectura de microservicios de Pedidos360 y
garantiza documentación viva y sincronizada con el código fuente.

### V. Control de Versiones Limpio e Higiene del Repositorio
El repositorio Git DEBE contener exclusivamente los artefactos requeridos para construir y ejecutar
el proyecto bajo el estándar Java / Maven (`pom.xml`, `src/`, configuraciones del proyecto).
DEBE mantenerse configurado un archivo `.gitignore` estricto en la raíz del proyecto que excluya:
- Artefactos y directorios de compilación (`target/`, `*.class`, `*.jar`, `*.war`).
- Archivos y directorios de configuración de entornos de desarrollo (`.idea/`, `*.iml`, `.vscode/`,
  `.settings/`, `.project`, `.classpath`).
- Archivos de credenciales, secretos, variables de entorno locales (`.env`, `credentials.json`,
  `*.pem`, `*.key`) y artefactos temporales del sistema operativo (`.DS_Store`, `Thumbs.db`).
*Rationale*: Previene la contaminación del repositorio, minimiza conflictos de fusión entre
desarrolladores y evita la exposición inadvertida de credenciales y archivos binarios.

## Estándares Técnicos y Restricciones del Servicio

- **Manejo Centralizado de Excepciones**: Los errores de negocio y de validación DEBEN gestionarse
  mediante un controlador global de excepciones (`@RestControllerAdvice`) devolviendo una estructura
  de respuesta de error estandarizada y predecible.
- **Validación de Datos**: Las solicitudes entrantes DEBEN validarse en los DTOs utilizando Jakarta
  Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, etc.).
- **Gestión Transaccional**: Toda operación que altere el estado de entidades en la base de datos
  DEBE estar delimitada con `@Transactional` en la capa de servicio.

## Flujo de Trabajo y Criterios de Aceptación

- **Compilación Limpia**: Todo commit o entrega de código DEBE compilar limpiamente mediante
  `mvn clean compile` y pasar la suite de pruebas automatizadas sin advertencias críticas.
- **Cobertura de Pruebas de Estados**: Las transiciones válidas e inválidas del ciclo de vida del
  pedido DEBEN estar cubiertas por pruebas automatizadas (unitarias y de integración).
- **Consistencia de Contrato**: Cualquier cambio en la API o modelos de datos DEBE actualizar la
  documentación OpenAPI correspondiente antes de considerarse terminado.

## Governance

Esta constitución es la norma suprema de gobernanza técnica para el microservicio
`ms_pedidos360_orders`. Toda propuesta de diseño, desarrollo de funcionalidad o refactorización
DEBE cumplir estrictamente con los principios aquí establecidos.
Cualquier enmienda a este documento DEBE seguir el proceso formal de revisión y cumplir con el
versionado semántico (SemVer):
- **MAJOR**: Modificaciones incompatibles en las reglas de gobernanza o eliminación de principios.
- **MINOR**: Incorporación de nuevos principios, requerimientos arquitectónicos o secciones adicionales.
- **PATCH**: Correcciones tipográficas, refinamientos de redacción o aclaraciones no semánticas.

Toda revisión de código (PR) o ciclo de especificación/implementación DEBE verificar la conformidad
con esta constitución antes de proceder.

**Version**: 1.0.0 | **Ratified**: 2026-09-03 | **Last Amended**: 2026-09-03

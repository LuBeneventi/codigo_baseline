# Informe de Seguridad de Software — Pedidos360 Coffee Store

## Índice
1. [Reglas del proyecto](#reglas-del-proyecto)
   - [Objetivo de la aplicación](#objetivo-de-la-aplicación)
   - [Arquitectura](#arquitectura)
   - [Stack tecnológico](#stack-tecnológico)
   - [Activos críticos](#activos-críticos)
   - [Trust boundaries](#trust-boundaries)
   - [Supuestos y restricciones](#supuestos-y-restricciones)
2. [Fase I — Investigación de seguridad](#fase-i--investigación-de-seguridad)
   - [Matriz de seguridad](#matriz-de-seguridad)
   - [Justificación técnica de los 5 casos seleccionados](#justificación-técnica-de-los-5-seleccionados)
3. [Fase II — Security Requirements](#fase-ii--security-requirements)
   - [Caso 01 · Autenticación y gestión de sesiones (SEC-CASE-01)](#caso-01--autenticación-y-gestión-de-sesiones-sec-case-01)
   - [Caso 02 · Autorización y control de acceso (SEC-CASE-02)](#caso-02--autorización-y-control-de-acceso-sec-case-02)
   - [Caso 03 · APIs (SEC-CASE-03)](#caso-03--apis-sec-case-03)
   - [Caso 04 · Validación y sanitización de entradas (SEC-CASE-04)](#caso-04--validación-y-sanitización-de-entradas-sec-case-04)
   - [Caso 05 · Configuración segura (SEC-CASE-05)](#caso-05--configuración-segura-sec-case-05)
4. [Fase III — Threat Modeling](#fase-iii--threat-modeling)
   - [1. Actores](#1-actores)
   - [2. Puntos de entrada](#2-puntos-de-entrada)
   - [3. Flujos de datos](#3-flujos-de-datos)
   - [4. DFD con trust boundaries](#4-dfd-con-trust-boundaries)
   - [5. ¿Qué puede salir mal en cada frontera? (STRIDE)](#5-qué-puede-salir-mal-en-cada-frontera-stride)

---

## Reglas del proyecto

### Objetivo de la aplicación
**Pedidos360** es una plataforma web distribuida basada en microservicios diseñada para la venta y gestión de café en línea. Resuelve la problemática de comercialización, procesamiento y seguimiento de pedidos de productos de café de especialidad de manera centralizada y ágil.

Los usuarios pueden registrarse, autenticarse, consultar el catálogo interactivo de cafés e insumos, gestionar ítems en su cesta y formalizar órdenes de compra. El sistema orquesta de forma asíncrona la notificación del estado del pedido al cliente y brinda a los administradores herramientas para la gestión y actualización del catálogo comercial.

#### Funcionalidades clave:
1. **Registro e inicio de sesión de usuarios** con emisión de tokens de acceso JWT.
2. **Exploración de catálogo:** Consulta de productos (cafés, granos, accesorios) con detalles de precio, descripción y disponibilidad de stock.
3. **Gestión de órdenes de compra:** Creación de pedidos, cálculo de totales unitarios y seguimiento de estado de la compra.
4. **Notificaciones asíncronas:** Emisión de confirmaciones y resúmenes de compra vía mensajería de colas y despacho de correo electrónico.
5. **Administración de catálogo:** Creación, actualización y mantenimiento de productos restringido a perfiles administradores.

---

### Arquitectura

La plataforma sigue una arquitectura de microservicios coordinados a través de un patrón BFF (*Backend For Frontend*) y comunicación desacoplada basada en eventos:

| Componente | Responsabilidad |
|---|---|
| **Frontend SPA (React + Vite)** | Interfaz gráfica de usuario, catálogo visual, carro de compras y consumo de API REST. |
| **BFF / API Gateway (`api_cloud`)** | Punto único de entrada al backend; autenticación de usuarios, emisión de JWT, validación perimetral y enrutamiento hacia microservicios de dominio. |
| **Microservicio Catálogo (`ms-pedidos360-catalog`)** | Gestión del ciclo de vida de productos (CRUD de café, control de stock y precios unitarios). |
| **Microservicio Órdenes (`ms-pedidos360-orders`)** | Creación y consulta de pedidos, asociación con el cliente y publicación de eventos de orden creada. |
| **Microservicio Notificaciones (`ms-pedidos360-notify`)** | Consumo asíncrono de eventos de órdenes desde RabbitMQ y despacho de correos electrónicos transaccionales. |
| **Base de Datos Relacional (`PostgreSQL`)** | Persistencia transaccional de usuarios, catálogo y órdenes de compra. |
| **Broker de Mensajería (`RabbitMQ`)** | Cola de mensajería (AMQP) para comunicación desacoplada y asíncrona entre microservicios. |
| **Servidor SMTP de pruebas (`Mailpit`)** | Captura y visualización de correos de confirmación en entorno controlado de laboratorio. |

---

### Stack tecnológico

| Capa | Tecnología / Herramienta | Versión / Detalle |
|---|---|---|
| **Lenguaje Backend** | Java | OpenJDK 17 LTS |
| **Framework Microservicios** | Spring Boot | 3.3.4 (`catalog`, `orders`, `notify`) |
| **BFF (Backend For Frontend)** | Spring Boot | 4.1.1 (`api_cloud`) |
| **Seguridad Backend** | Spring Security + JJWT | Autenticación stateless vía JSON Web Tokens |
| **Capa de Persistencia** | Spring Data JPA / Hibernate | Mapeo objeto-relacional y consultas transaccionales |
| **Base de Datos** | PostgreSQL | 15 / 16 (puerto default 5432) |
| **Broker de Mensajería** | RabbitMQ | Protocolo AMQP 0-9-1 (puertos 5672 / 15672) |
| **Frontend** | React 18, TypeScript, Vite | 5.4.10 |
| **Servicio de Correo (Lab)** | Mailpit (SMTP / Web UI) | Puertos 1025 (SMTP) y 8025 (Web) |
| **Contenedores** | Docker, Docker Compose | Orquestación completa multicontenedor |
| **Testing de Seguridad** | Semgrep, OSV Scanner, Pytest/Python Security Suite | SAST, SCA y Pruebas Automatizadas de Seguridad |

---

### Activos críticos

| ID | Activo | Por qué importa |
|---|---|---|
| **A-01** | **Credenciales de usuario (Hash BCrypt)** | Su filtración permite suplantar la identidad de compradores o administradores del sistema. |
| **A-02** | **Tokens de sesión (JWT)** | Portador de autorización; quien lo intercepte o posea un token con vigencia desmedida actúa en nombre de la víctima. |
| **A-03** | **Datos personales de clientes (PII: Nombre, Correo, RUT, Dirección)** | Información sensible expuesta durante el checkout y consulta de órdenes; regulada por privacidad de datos. |
| **A-04** | **Integridad del catálogo (Precios y Stock)** | La alteración maliciosa de precios o stock provoca pérdidas financieras directas o inconsistencias de inventario. |
| **A-05** | **Órdenes de compra del cliente** | Integridad y confidencialidad transaccional: un cliente solo debe tener visibilidad y control sobre sus propios pedidos. |
| **A-06** | **Secretos de configuración del sistema (`JWT_SECRET`, credenciales BD/RabbitMQ)** | Llaves maestras que comprometen la totalidad de la infraestructura y permiten falsificar firmas de seguridad. |
| **A-07** | **Logs y mensajes de error internos del sistema** | La filtración de trazas de excepción expone versiones de componentes, librerías y topología interna a atacantes. |
| **A-08** | **Cola de mensajería (RabbitMQ)** | Contiene los eventos de negocio con los payloads completos de clientes y órdenes en tránsito. |
| **A-09** | **Disponibilidad del BFF y Microservicios** | Capacidad del sistema para procesar compras legítimas sin ser degradado por abusos o ráfagas masivas. |

---

### Trust boundaries

| ID | Frontera | Qué cambia |
|---|---|---|
| **TB-1** | **Internet / Cliente ↔ Frontend SPA** | Entorno no confiable del navegador web del usuario final; expuesto a manipulaciones del DOM y secuencias maliciosas. |
| **TB-2** | **Frontend ↔ BFF API Gateway (`api_cloud`)** | Frontera perimetral crítica: las peticiones HTTP/JSON cruzan desde una red pública a la infraestructura; aquí se valida identidad y sesión. |
| **TB-3** | **BFF Gateway ↔ Microservicios Internos (`catalog`, `orders`)** | Red interna Docker. El BFF propaga contexto de usuario y roles; los microservicios internos deben exigir autorización sin asumir confianza ciega en cabeceras. |
| **TB-4** | **Microservicios ↔ PostgreSQL** | Conexiones a la base de datos centralizada; depende de consultas parametrizadas y segregación de credenciales. |
| **TB-5** | **Microservicio Órdenes ↔ RabbitMQ** | Encolado de mensajes de integración asíncrona; el contenido debe viajar protegido de accesos no autorizados en el broker. |
| **TB-6** | **Microservicio Notificaciones ↔ Mailpit (SMTP)** | Salida de información hacia plantillas de correo; los datos inyectados deben ser saneados para evitar inyección en el correo o clientes destinatarios. |

---

### Supuestos y restricciones

#### Supuestos
1. Todo el trabajo y análisis se desarrolla en un entorno de laboratorio controlado y local con datos de prueba ficticios.
2. El sistema operativo anfitrión y el daemon de Docker se consideran confiables y fuera del alcance de compromiso previo.
3. El adversario modelado es un atacante remoto que puede interactuar de forma anónima o autenticada utilizando navegadores, proxies HTTP (ZAP, Burp) o scripts automatizados.
4. Las dependencias externas de librerías provienen de repositorios oficiales (Maven Central y npm registry).
5. El servidor SMTP de laboratorio (Mailpit) almacena y procesa localmente los correos sin retransmitir mensajes a casillas públicas reales.
6. La sincronización horaria de los contenedores es consistente, permitiendo evaluar la expiración temporal de tokens.

#### Restricciones
1. **Ámbito académico:** Trabajo individual acotado al calendario académico de la asignatura OCY1102 (Ciberseguridad en el Desarrollo).
2. **Alcance funcional:** Quedan fuera del proyecto pagos reales mediante pasarelas financieras, autenticación mediante terceros (OAuth2/OpenID Connect), notificaciones push y aplicaciones móviles.
3. **Despliegue:** Ejecución estrictamente local mediante Docker Compose sobre interfaces de loopback (`127.0.0.1`), sin orquestadores cloud (Kubernetes).
4. **Herramientas de evaluación:** Empleo exclusivo de utilitarios libres, de código abierto o scripts de desarrollo propio (Semgrep, OSV API, Python, Curl).
5. **Alcance de seguridad:** Quedan fuera de alcance ataques de Denegación de Servicio (DoS) volumétricos de capa de red, ingeniería social y auditorías físicas de hardware.

---

## Fase I — Investigación de seguridad

### Matriz de seguridad

| Riesgo | Componente | Referencia OWASP | CWE Asociados | Impacto | Probabilidad | Seleccionado | Justificación técnica |
|---|---|---|---|:---:|:---:|:---:|---|
| **Caso 1** | **Autenticación y gestión de sesiones** | A07:2021 Identification & Auth Failures | CWE-307, CWE-613 | **Alto** | **Alta** | **Sí** | El login en `api_cloud` es el vector primario de ingreso. La falta de bloqueo facilita fuerza bruta y la vigencia excesiva de tokens JWT amplifica la ventana de secuestro de sesiones. |
| **Caso 2** | **Autorización y control de acceso** | A01:2021 Broken Access Control | CWE-639, CWE-862 | **Alto** | **Alta** | **Sí** | Las órdenes pertenecen a clientes individuales. La falta de verificación de identidad (BOLA) y la confianza ciega en cabeceras de rol externas comprometen la privacidad y permiten escalación de privilegios. |
| **Caso 3** | **Seguridad de API** | A04:2021 Insecure Design / OWASP API 2023 | CWE-915, CWE-200, CWE-770 | **Medio** | **Alta** | **Sí** | La API expone endpoints donde se aceptan precios manipulados (Mass Assignment), se filtran datos personales innecesarios y carece de tasa límite ante ráfagas. |
| **Caso 4** | **Validación y sanitización de entradas** | A03:2021 Injection | CWE-89, CWE-20, CWE-79 | **Alto** | **Alta** | **Sí** | Parámetros de búsqueda en catálogo pueden concatenarse hacia la BD (SQLi), la omisión de validaciones permite precios/cantidades negativas, y los correos generados admiten inyección HTML. |
| **Caso 5** | **Configuración segura** | A05:2021 Security Misconfiguration | CWE-1188, CWE-942, CWE-209 | **Medio** | **Alta** | **Sí** | Contenedores ejecutando como root, puertos de servicios internos abiertos en el host, secretos débiles por defecto en `application.yml` y políticas CORS excesivamente permisivas. |
| **Caso 6** | Manejo seguro de errores | A05:2021 Security Misconfiguration | CWE-209, CWE-755 | Medio | Media | No | Considerado parcialmente dentro del Caso 5 en el contexto de la exposición de excepciones en el BFF. |
| **Caso 7** | Logging y auditoría | A09:2021 Security Logging & Monitoring Failures | CWE-778, CWE-532 | Medio | Media | No | Se analiza de forma transversal en el Caso 4 con el registro indebido de payloads de correo en los logs de microservicios. |
| **Caso 8** | Criptografía y secretos | A02:2021 Cryptographic Failures | CWE-798, CWE-312 | Alto | Media | No | Abordado e integrado de forma prioritaria en el Caso 5 mediante el análisis de variables de entorno y secretos fijos. |
| **Caso 9** | Dependencias de terceros (SCA) | A06:2021 Vulnerable & Outdated Components | CWE-1395, CWE-1104 | Alto | Media | No | Evaluado como fase metodológica transversal (SCA) aplicada a todos los microservicios Maven y frontend npm. |
| **Caso 10** | Integridad de pipeline y despliegue | A08:2021 Software & Data Integrity Failures | CWE-829, CWE-494 | Medio | Baja | No | Excluido por tratarse de un despliegue puramente local en Docker Compose de laboratorio. |

---

### Justificación técnica de los 5 seleccionados

1. **Autenticación y gestión de sesiones (SEC-CASE-01):** Representa el perímetro de acceso a la tienda. Al procesar compras y pedidos, la ausencia de límites de intentos fallidos en `/auth/login` permite ataques de diccionario automatizados. Además, la emisión de tokens JWT con 30 días de vigencia sin revocación deja a los usuarios expuestos a suplantación persistente.
2. **Autorización y control de acceso (SEC-CASE-02):** La relación cliente-orden es el núcleo de negocio. Si `OrderServiceImpl` devuelve todas las órdenes sin filtrar por el sujeto autenticado (`BOLA`), o si un usuario regular puede enviar `X-User-Role: admin` para desbloquear rutas de catálogo privilegiadas, se destruye la segregación de funciones.
3. **Seguridad de API (SEC-CASE-03):** El BFF debe actuar como filtro defensivo. Permitir que el cliente defina el `precioUnitario` en el body al crear una orden permite defraudar a la tienda pagando valores arbitrarios. Asimismo, devolver datos personales completos (RUT, dirección) o exponer el hash de contraseña en `/me` viola la minimización de datos.
4. **Validación y sanitización de entradas (SEC-CASE-04):** El microservicio de catálogo recibe criterios de búsqueda susceptibles a inyección SQL en consultas no parametrizadas. A su vez, admitir cantidades de café negativas altera el stock e inventario, y renderizar nombres de cliente sin escapar en las notificaciones HTML posibilita ataques de inyección en clientes de correo.
5. **Configuración segura (SEC-CASE-05):** El entorno Docker y los archivos `.yml` configuran la postura defensiva base. Mantener puertos como 5432 (Postgres) o 5672 (RabbitMQ) expuestos en el host con credenciales por defecto (`postgres`, `guest/guest`), junto a comodines CORS (`*`), expone los servicios a pivoteos y peticiones forzadas entre sitios.

---

## Fase II — Security Requirements

### Caso 01 · Autenticación y gestión de sesiones (SEC-CASE-01)

| ID | Activo | Amenaza | Requisito de Seguridad | Mecanismo de Mitigación | Validación / Aserción |
|---|---|---|---|---|---|
| **SEC-REQ-001** | A-01 | Fuerza bruta contra login (CWE-307) | El endpoint `/auth/login` debe bloquear o retardar peticiones tras 5 intentos fallidos consecutivos sobre una misma cuenta o IP durante 15 minutos. | Contador en memoria o almacenamiento de intentos fallidos / Rate Limiting en BFF. | Ejecutar 6 intentos con contraseña errónea; el 6.° debe responder con HTTP 429 Too Many Requests o bloqueo explícito. |
| **SEC-REQ-003** | A-02 | Tokens JWT con expiración desmedida (CWE-613) | Los tokens de acceso JWT deben tener un tiempo de vida máximo de 3600 segundos (1 hora), requiriendo reautenticación o refresco. | Configuración de `ChronoUnit.HOURS` o `ChronoUnit.MINUTES` en `JwtService`. | Decodificar el claim `exp` de un JWT generado; comprobar que `exp - iat` sea $\le 3600$ segundos. |

---

### Caso 02 · Autorización y control de acceso (SEC-CASE-02)

| ID | Activo | Amenaza | Requisito de Seguridad | Mecanismo de Mitigación | Validación / Aserción |
|---|---|---|---|---|---|
| **SEC-REQ-004** | A-05 | BOLA / IDOR en consulta y creación de pedidos (CWE-639) | Las operaciones sobre órdenes deben resolver el `clienteId` estrictamente desde los claims del token JWT verificado en el backend, no desde el body HTTP. El listado de pedidos debe filtrar exclusivamente los del solicitante. | Inyección del usuario autenticado desde `SecurityContextHolder` en la consulta JPA. | El usuario A consulta `GET /orders/{id_B}` o `GET /orders`; el sistema debe devolver 403 Forbidden o aislar el resultado al usuario A. |
| **SEC-REQ-005** | A-04, A-05 | Escalación de privilegios por inyección de cabecera de rol (CWE-862) | Los microservicios internos no deben confiar en cabeceras `X-User-Role` enviadas desde clientes externos; los permisos deben validarse desde el token firmado. | Sanitización y supresión perimetral de cabeceras en el BFF antes de contactar a microservicios. | Usuario con rol cliente envía `X-User-Role: admin` a endpoints de gestión de catálogo; debe recibir 403 Forbidden. |

---

### Caso 03 · APIs (SEC-CASE-03)

| ID | Activo | Amenaza | Requisito de Seguridad | Mecanismo de Mitigación | Validación / Aserción |
|---|---|---|---|---|---|
| **SEC-REQ-006** | A-04, A-05 | Manipulación de precios por Mass Assignment (CWE-915) | Al crear una orden, el precio unitario y descripción de cada ítem deben obtenerse forzosamente desde el catálogo oficial en base de datos, ignorando cualquier valor provisto en el payload del cliente. | Consulta de validación contra `ms-catalog` en la lógica de negocio de órdenes antes de persistir. | Enviar orden con ítem de café a precio `1.0`; verificar que la orden persistida aplique el precio real oficial (ej. `4990.0`). |
| **SEC-REQ-007** | A-01, A-03 | Exposición excesiva de datos sensibles en respuestas de API (CWE-200) | Las respuestas de consulta de órdenes y perfil deben filtrar campos privados innecesarios como RUT, dirección, o `passwordHash`. | Implementación de DTOs de salida específicos (`OrderSummaryResponseDto`, `@JsonIgnore`). | Inspeccionar respuesta JSON de `GET /me` y `GET /orders`; validar ausencia estricta de `passwordHash` y datos de terceros. |
| **SEC-REQ-008** | A-09 | Denegación de servicio por consumo abusivo de recursos (CWE-770) | El API Gateway / BFF debe implementar una tasa máxima global de peticiones por dirección IP (ej. 100 req/min). | Filtro de Rate Limiting con Bucket4j o Spring Cloud Gateway en `SecurityConfig`. | Disparar ráfaga de 120 peticiones HTTP en menos de 1 minuto; verificar que las excedentes respondan con HTTP 429. |

---

### Caso 04 · Validación y sanitización de entradas (SEC-CASE-04)

| ID | Activo | Amenaza | Requisito de Seguridad | Mecanismo de Mitigación | Validación / Aserción |
|---|---|---|---|---|---|
| **SEC-REQ-009** | A-04, A-08 | Inyección SQL y Cross-Site Scripting (CWE-89, CWE-79) | Todas las búsquedas en catálogo deben emplear queries JPA parametrizadas; las plantillas HTML de correo deben escapar todo valor provisto por usuarios. | Consultas JPQL parametrizadas (`:filtro`) y uso de `HtmlUtils.htmlEscape()` en `EmailSenderService`. | Enviar búsqueda con `' OR '1'='1` (no debe alterar el filtro) e inyectar `<script>alert(1)</script>` en nombre de cliente (debe escapar a `&lt;script&gt;`). |
| **SEC-REQ-010** | A-04, A-05 | Parámetros de negocio fuera de rango o negativos (CWE-20) | Las entidades y DTOs de producto y orden deben validar que cantidades sean $\ge 1$ y precios $\gt 0$ mediante Bean Validation. | Anotaciones `@Min(1)`, `@Positive`, `@NotNull` y `@Valid` en controladores Spring. | Enviar creación de producto u orden con `cantidad: -5` o `precio: -100`; el sistema debe responder HTTP 400 Bad Request. |
| **SEC-REQ-011** | A-08 | Filtración de datos sensibles en logs de eventos (CWE-532) | El listener de RabbitMQ en el servicio de notificaciones no debe volcar el payload JSON íntegro del pedido a los registros de consola. | Logging selectivo y ofuscado de identificadores mínimos de transacción. | Inspeccionar bitácora de ejecución de `ms-notify`; corroborar que no se impriman cuerpos completos con datos personales. |

---

### Caso 05 · Configuración segura (SEC-CASE-05)

| ID | Activo | Amenaza | Requisito de Seguridad | Mecanismo de Mitigación | Validación / Aserción |
|---|---|---|---|---|---|
| **SEC-REQ-012** | A-06 | Secretos débiles por defecto en configuración (CWE-1188, CWE-798) | Los servicios no deben contener valores por defecto predecibles (`changeme`, `postgres`, `guest/guest`) para claves JWT ni bases de datos. | Externalización obligatoria vía variables de entorno sin fallbacks inseguros. | Análisis de archivos `application.yml` y `docker-compose.yml` asegurando que no existan credenciales predeterminadas débiles. |
| **SEC-REQ-013** | A-02, A-09 | Política de CORS permisiva y falta de cabeceras de seguridad (CWE-942, CWE-16) | El backend debe restringir orígenes permitidos al dominio del frontend (`http://localhost:8080`) e inyectar cabeceras CSP, HSTS y `X-Content-Type-Options`. | Configuración de `CorsConfigurationSource` específico y cabeceras de seguridad en Spring Security. | Comprobar respuestas HTTP ante orígenes arbitrarios; no debe figurar `Access-Control-Allow-Origin: *`. Cabeceras de seguridad presentes. |
| **SEC-REQ-014** | A-06, A-07 | Exposición de puertos de persistencia y trazas de error (CWE-1188, CWE-209) | PostgreSQL y RabbitMQ no deben publicar puertos en el host anfitrión; los contenedores deben ejecutar sin privilegios de root y los errores deben ser genéricos. | Eliminar directivas `ports` para componentes internos en `docker-compose.yml`, configurar directiva `USER` en Dockerfiles y `GlobalExceptionHandler`. | Validar con `docker ps` que los puertos 5432 y 5672 no se expongan fuera de la red interna Docker. Forzar excepción y verificar ausencia de stack traces. |

---

## Fase III — Threat Modeling

### 1. Actores

| ID | Actor | Nivel de Confianza | Descripción y Objetivos |
|---|---|:---:|---|
| **AC-01** | **Visitante Anónimo** | Nula | Usuario no autenticado que explora el catálogo de café e intenta registrarse. |
| **AC-02** | **Cliente Autenticado** | Media | Usuario legítimo que inicia sesión, añade productos a su carro y crea pedidos de café. |
| **AC-03** | **Usuario Malicioso (Autenticado)** | Baja | Posee cuenta válida de cliente e intenta acceder o alterar órdenes de otros clientes (IDOR/BOLA), forzar precios o manipular cabeceras de rol. |
| **AC-04** | **Administrador del Sistema** | Alta | Personal autorizado para el alta, baja y modificación de precios y stock en el catálogo. |
| **AC-05** | **Atacante Externo Remoto** | Nula | Actor hostil en Internet que ejecuta escaneos de puertos, ataques de fuerza bruta al login, inyecciones de payloads o saturación de peticiones. |

---

### 2. Puntos de entrada

| ID | Punto de Entrada | Protocolo / Transporte | Activos Expuestos |
|---|---|---|---|
| **EP-01** | `POST /auth/register` y `POST /auth/login` | HTTP REST (JSON) | A-01, A-02 |
| **EP-02** | `GET /catalog/products` (Búsqueda y listado) | HTTP REST (JSON) | A-04 |
| **EP-03** | `POST /orders` (Creación de pedidos) | HTTP REST (JSON) | A-04, A-05 |
| **EP-04** | `GET /orders` y `GET /orders/{id}` | HTTP REST (JSON) | A-03, A-05 |
| **EP-05** | `POST /catalog/products`, `PUT /catalog/products/{id}` | HTTP REST (JSON) | A-04 |
| **EP-06** | `GET /me` (Perfil de usuario) | HTTP REST (JSON) | A-01, A-03 |
| **EP-07** | Puertos de infraestructura expuestos (5432, 5672, 15672, 8086, 8087) | TCP directo | A-06, A-07 |
| **EP-08** | Intercambio de mensajería RabbitMQ (`orders.queue`) | Protocolo AMQP | A-08 |

---

### 3. Flujos de datos

| ID | Flujo | Protocolo y Datos Transportados | Frontera Involucrada |
|---|---|---|---|
| **DF-01** | Usuario final ↔ Frontend SPA | HTTPS; clics, navegación e interacción en la interfaz web. | **TB-1** |
| **DF-02** | Frontend SPA ⇄ BFF API Gateway (`api_cloud`) | HTTP REST; credenciales, órdenes en JSON y tokens JWT en cabecera `Authorization`. | **TB-2** |
| **DF-03** | BFF Gateway ⇄ Microservicio Catálogo (`ms-catalog`) | HTTP REST interno; consulta de ítems y stock, propagación de contexto de rol. | **TB-3** |
| **DF-04** | BFF Gateway ⇄ Microservicio Órdenes (`ms-orders`) | HTTP REST interno; envío de pedidos para procesamiento y persistencia. | **TB-3** |
| **DF-05** | Microservicio Órdenes → RabbitMQ Broker | Protocolo AMQP; publicación de evento de orden creada con datos de cliente y detalle. | **TB-5** |
| **DF-06** | RabbitMQ Broker → Microservicio Notificaciones (`ms-notify`) | Protocolo AMQP; consumo asíncrono del evento de orden para despacho. | **TB-5** |
| **DF-07** | Microservicio Notificaciones → Mailpit | Protocolo SMTP; envío de correo electrónico con formato HTML de confirmación de compra. | **TB-6** |
| **DF-08** | Microservicios (`api_cloud`, `catalog`, `orders`) ⇄ PostgreSQL | Protocolo PostgreSQL (TCP); consultas de persistencia y lectura de usuarios, stock y pedidos. | **TB-4** |

---

### 4. DFD con trust boundaries

```text
 ┌───────────────────────────────────────────┐
 │       Usuario / Comprador / Admin         │
 │            (AC-01 … AC-05)                │
 └─────────────────────┬─────────────────────┘
                       │ DF-01 (HTTPS / Interacción Web)
 ══════════════════════╪═══════════════════════════════════════════ TB-1
                       ▼
 ┌───────────────────────────────────────────┐
 │          Frontend SPA (React + Vite)      │
 │                  Puerto 8080              │
 └─────────────────────┬─────────────────────┘
                       │ DF-02 (API REST + Bearer JWT / JSON)
 ══════════════════════╪═══════════════════════════════════════════ TB-2
                       ▼
 ┌───────────────────────────────────────────┐
 │          BFF Gateway (api_cloud)          │
 │         Autenticación / Seguridad         │
 └──────────┬─────────────────────┬──────────┘
            │ DF-03 (HTTP interno)│ DF-04 (HTTP interno)
 ═══════════╪═════════════════════╪═══════════════════════════════ TB-3
            ▼                     ▼
 ┌──────────────────────┐ ┌──────────────────────┐
 │ ms-pedidos360-catalog│ │  ms-pedidos360-orders│
 │     Puerto 8086      │ │     Puerto 8087      │
 └──────────┬───────────┘ └──────────┬───────────┘
            │                        │ DF-05 (AMQP: Evento Orden Creada)
            │             ═══════════╪════════════════════════════ TB-5
            │                        ▼
            │             ┌──────────────────────┐
            │             │  RabbitMQ Broker     │
            │             │  Puertos 5672/15672  │
            │             └──────────┬───────────┘
            │                        │ DF-06 (AMQP: Consumo Asíncrono)
            │                        ▼
            │             ┌──────────────────────┐
            │             │ ms-pedidos360-notify │
            │             │     Puerto 8088      │
            │             └──────────┬───────────┘
            │                        │ DF-07 (SMTP: Correo Notificación)
            │             ═══════════╪════════════════════════════ TB-6
            │                        ▼
            │             ┌──────────────────────┐
            │             │ Mailpit (SMTP Lab)   │
            │             │ Puertos 1025 / 8025  │
            │             └──────────────────────┘
            │ DF-08 (SQL Relacional)
 ═══════════╪═════════════════════════════════════════════════════ TB-4
            ▼
 ┌───────────────────────────────────────────┐
 │          PostgreSQL Relacional            │
 │           Base de Datos :5432             │
 └───────────────────────────────────────────┘
```

---

### 5. ¿Qué puede salir mal en cada frontera? (STRIDE)

| ID | Frontera | Categoría STRIDE | Amenaza / Qué puede salir mal | Activos Afectados | Control Requerido | SEC-REQ |
|---|---|:---:|---|---|---|:---:|
| **T-01** | **TB-2** | **S** (Spoofing) | Ataques de fuerza bruta y diccionario contra el endpoint `/auth/login` debido a la ausencia de bloqueo de intentos. | A-01 | Implementar bloqueo progresivo de cuenta tras 5 intentos fallidos y Rate Limiting por IP. | 001 |
| **T-02** | **TB-2** | **S** (Spoofing) | Uso prolongado de credenciales comprometidas por emisión de tokens JWT con validez excesiva de 30 días. | A-02 | Reducir la vida útil del JWT de acceso a $\le 1$ hora y adoptar tokens de refresco. | 003 |
| **T-03** | **TB-2 / TB-3** | **E** (Elevation of Privilege) | Acceso indebido a pedidos de terceros (`BOLA/IDOR`): un cliente lista todas las órdenes o visualiza pedidos ajenos en `ms-orders`. | A-05, A-03 | Filtrar órdenes en capa de base de datos resolviendo estrictamente el ID del cliente desde el JWT verificado. | 004 |
| **T-04** | **TB-3** | **E** (Elevation of Privilege) | Escalación de privilegios hacia funciones de catálogo administrativo inyectando la cabecera `X-User-Role: admin`. | A-04 | Microservicios deben validar roles a partir del token firmado, eliminando confianza en cabeceras externas del cliente. | 005 |
| **T-05** | **TB-2 / TB-3** | **T** (Tampering) | Manipulación de precios por asignación masiva (`Mass Assignment`): el cliente define el `precioUnitario` en el body al comprar. | A-04, A-05 | El backend debe ignorar el precio provisto por el cliente y fijarlo consultando el valor oficial en `ms-catalog`. | 006 |
| **T-06** | **TB-2** | **I** (Information Disclosure) | Exposición indebida de datos personales de clientes (RUT, dirección) o de contraseñas (`passwordHash` en `/me`) en respuestas JSON. | A-01, A-03 | Aplicar DTOs de respuesta con minimización de datos y supresión de atributos sensibles con `@JsonIgnore`. | 007 |
| **T-07** | **TB-2** | **D** (Denial of Service) | Saturación o agotamiento de hilos del BFF ante ráfagas descontroladas de peticiones HTTP por falta de limitación de tasa. | A-09 | Configurar middleware de Rate Limiting con políticas estrictas de peticiones por minuto. | 008 |
| **T-08** | **TB-4** | **T / I** (Tampering / Info) | Inyección SQL en parámetros de búsqueda en catálogo de café mediante concatenación de sentencias. | A-04, A-01 | Empleo exclusivo de consultas preparadas y parametrizadas en repositorios Spring Data JPA. | 009 |
| **T-09** | **TB-6** | **T** (Tampering) | Inyección de código HTML / XSS almacenado en la plantilla de correo de confirmación de pedido al insertar el nombre del cliente sin escapar. | A-08, A-03 | Aplicar sanitización y codificación de entidades con `HtmlUtils.htmlEscape()` antes del renderizado del correo. | 009, 011 |
| **T-10** | **TB-2** | **T** (Tampering) | Introducción de inconsistencias en el negocio al enviar órdenes o productos con stock negativo o precios inferiores o iguales a cero. | A-04, A-05 | Validación estricta con Bean Validation (`@Min`, `@Positive`) en los DTOs de entrada. | 010 |
| **T-11** | **TB-5 / TB-6** | **I** (Information Disclosure) | Exposición involuntaria de información personal de clientes al registrar el payload completo del evento en logs de consola de `ms-notify`. | A-03, A-08 | Registrar únicamente identificadores de transacción y ofuscar detalles sensibles en las bitácoras. | 011 |
| **T-12** | **TB-2 / TB-4** | **I** (Information Disclosure) | Uso de contraseñas y llaves predeterminadas inseguras (`changeme`, `postgres`, `guest`) en `application.yml` y `docker-compose.yml`. | A-06 | Exigir llaves criptográficas de alta entropía y credenciales inyectadas forzosamente por variables de entorno. | 012 |
| **T-13** | **TB-1 / TB-2** | **I** (Information Disclosure) | Política CORS con comodín (`*`) que habilita la lectura de recursos de la API desde cualquier dominio de origen arbitrario. | A-02, A-09 | Restringir orígenes cruzados únicamente a la URL oficial del frontend y definir directivas CSP restrictivas. | 013 |
| **T-14** | **TB-4 / TB-5** | **E / I** (Elevation / Info) | Exposición directa al anfitrión de puertos internos de infraestructura de base de datos (5432) y mensajería (5672/15672). | A-06, A-07 | Eliminar la publicación de puertos de base de datos y broker en el host, aislándolos en la red interna Docker. | 014 |
| **T-15** | **TB-2** | **I** (Information Disclosure) | Fuga de detalles de implementación interna, nombres de clases y base de datos al devolver trazas de error (`ex.getMessage()`) en respuestas 500. | A-07 | Configurar manejador global de excepciones con respuestas estandarizadas y mensajes de error genéricos. | 014 |
| **T-16** | **TB-2** | **I** (Information Disclosure) | Interfaz de documentación Swagger UI accesible públicamente en entornos de despliegue productivo. | A-07 | Deshabilitar la interfaz Swagger UI en perfiles productivos mediante `springdoc.swagger-ui.enabled=false`. | 014 |
| **T-17** | **TB-3** | **E** (Elevation of Privilege) | Ejecución de procesos de microservicios con privilegios de superusuario (`root`) dentro de los contenedores Docker. | A-09 | Declarar directivas de usuario no privilegiado (`USER`) en cada archivo `Dockerfile`. | 014 |
| **T-18** | **TB-1 / TB-2** | **T** (Tampering) | Vulnerabilidad ante Clickjacking y ataques basados en MIME-type por ausencia de cabeceras de protección en las respuestas HTTP. | A-02 | Configurar en Spring Security las cabeceras `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff` y `HSTS`. | 013 |

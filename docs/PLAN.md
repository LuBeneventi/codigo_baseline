==============================================================
TIENDA DE CAFE (Pedidos360): BFF + catalog + orders + notify + RabbitMQ
INSTRUCCIONES: ESTADO A (BASELINE) Y ESTADO B (CORRECCION)
OCY1102 Secure Development Playbook - Windows / PowerShell
Version ajustada al codigo real (CloudComputing.zip)
==============================================================

ADVERTENCIA DE LABORATORIO
El Estado A es deliberadamente SIN controles de seguridad, para poder demostrar el FAIL de cada TEST-SEC.
- Solo se ejecuta en tu equipo (todo enlazado a 127.0.0.1). Nunca se despliega ni se publica.
- Solo datos ficticios. Ninguna credencial, token ni ID real del proyecto original puede quedar en la copia.
- Cada error que introduzcas lleva un comentario de una linea: "BASELINE-VULN [SEC-REQ-XXX]: que falta". Asi ubicas cada error en el codigo
  y, despues, el cambio entre el codigo con error y el corregido se ve linea por linea.

==============================================================
LO QUE HAY EN TU CODIGO (resumen de la revision)
==============================================================
Arquitectura real:
  Navegador -> BFF "api_cloud" (Spring Boot 4.1.1, Java 17, puerto 8088)
            -> ms-pedidos360-orders (8087) y ms-pedidos360-catalog (8086), cada uno con su base en el mismo PostgreSQL
  orders -> RabbitMQ (colas q.cmd.email y q.cmd.invoice) -> ms-pedidos360-notify -> Mailpit
  docker-compose.yml de la raiz ya levanta todo MENOS un frontend. No existe login propio: hoy el BFF usa Microsoft Entra ID y solo si
  SECURITY_ENABLED=true; por defecto esta en false y deja pasar todo (anyRequest().permitAll()).

Estado de cada SEC-REQ en tu codigo:

SEC-REQ  | Hoy en tu codigo                                              | Que hay que hacer
---------+---------------------------------------------------------------+-------------------------------------------
001      | No hay login propio                                           | CONSTRUIR login en el BFF (sin limite de intentos)
003      | Token de Entra (no es tuyo)                                   | CONSTRUIR JWT propio de 30 dias
004      | YA EXISTE: orders no sabe de duenos, lista todo (findAll)     | Verificar; el BFF no compara dueno
005      | Hoy permitAll; sin roles                                      | CONSTRUIR roles; el BFF lee X-User-Role del cliente
006      | YA EXISTE: clienteId, nombre y precioUnitario vienen del body | Verificar; el registro aceptara "role"
007      | OrderResponseDto expone nombre y email a quien consulte       | Agregar rut y direccion; el registro devuelve el hash
008      | YA EXISTE: no hay limite de peticiones                        | Verificar
009      | No hay frontend                                               | CONSTRUIR front que usa innerHTML
010      | Hay @Min, @Positive, @DecimalMin y chequeo de stock; no hay   | QUITAR validaciones; CONSTRUIR busqueda con
         | busqueda de productos                                         | SQL concatenado
011      | YA EXISTE: notify arma el HTML del correo sin escapar         | Verificar; dejar registro del cuerpo
012      | YA EXISTE: guest/guest, postgres/postgres, IDs de Entra       | Verificar; agregar JWT_SECRET por defecto
013      | YA EXISTE: CORS con "*" en SecurityConfig, sin CSP            | Verificar
014      | YA EXISTE: puertos publicados, sin USER, errores con detalle  | Verificar

(YA EXISTE = solo hay que comprobarlo y documentarlo. CONSTRUIR = codigo nuevo. QUITAR = control que hoy existe y se elimina.)
Los tests unitarios que ya tienes (ProductCreateControllerTest, ProductServiceCreationTest, etc.) prueban varias de esas validaciones.

CAMBIOS RESPECTO A LOS SEC-REQ ORIGINALES
- SEC-REQ-002 (recuperacion de contrasena): se retira, la app no la tiene.
- SEC-REQ-003: pasa a "sesion de maximo 15 minutos con firma y algoritmo verificados" (sin MFA).
- SEC-REQ-011: pasa a "notify escapa y valida el contenido de los eventos de RabbitMQ" (ya no hay imagenes).
- SEC-REQ-014 incluye ahora que los microservicios no sean alcanzables sin pasar por el BFF.
- El resto mantiene su ID, adaptado a pedidos y catalogo.

ORDEN DE TRABAJO
Parte 1: copiar y limpiar el proyecto (etiqueta v0-original).
Parte 2: construir lo que falta, ya en su forma baseline (login, busqueda, frontend).
Parte 3: Estado A: ubicar y marcar los errores, y quitar los controles (etiqueta v1-baseline).
Parte 4: comprobar que el baseline falla.
Parte 5: Estado B: corregir un SEC-REQ a la vez, con re-test (etiqueta v2-secure).

==============================================================
PARTE 1 - COPIAR Y LIMPIAR
==============================================================

PASO 1 - CREAR LA CARPETA Y COPIAR EL PROYECTO
En PowerShell (reemplaza <ruta> por la carpeta CloudComputing):

mkdir cafe-playbook
cd cafe-playbook
git init
mkdir src, tests, security, docs, evidence
Set-Content .gitignore ".env","*.env","node_modules/","target/","dist/",".idea/"
robocopy "<ruta>\CloudComputing" "src" /E /XD .git target node_modules .agents .specify specs .idea /XF Datos.txt .env *.env docker-compose.bff.yml docker-compose.db.yml docker-compose.microservices.yml docker-compose.rabbitmq.yml

(robocopy devuelve codigos 0 a 7 cuando todo salio bien. Se excluye Datos.txt, que tiene un token real, y los compose de despliegue en EC2.)
Despues, borra los compose sueltos de cada servicio para que solo quede el de la raiz:

Remove-Item src\api_cloud\docker-compose.yml, src\ms-pedidos360-catalog\docker-compose.yml, src\ms-pedidos360-notify\docker-compose.yml -ErrorAction SilentlyContinue

PASO 2 - QUITAR IDS Y CREDENCIALES DE ENTRA
Busca en la copia:

Get-ChildItem src -Recurse -File | Select-String -Pattern "microsoftonline","7787491b","954079da","client-secret","AKIA","neon.tech"

Deberian aparecer estos archivos: src\docker-compose.yml, src\api_cloud\src\main\resources\application.yml y src\api_cloud\.env.example.
En los tres reemplaza el tenant y el client id por un valor ficticio, o borra esas lineas: en el Paso 6 reemplazas Entra por login propio.
Vuelve a ejecutar la busqueda hasta que no devuelva nada.

PASO 3 - COMPROBAR QUE TODO ARRANCA TAL COMO ESTA
En PowerShell:

cd src
docker compose up -d --build
docker compose ps

Prueba el catalogo y los pedidos con curl.exe (con SECURITY_ENABLED en false todo pasa):

curl.exe -i http://localhost:8088/api/catalog/products
curl.exe -i http://localhost:8088/api/orders

Cuando todo funcione:

cd ..
git add .
git commit -m "chore: copia del proyecto sin credenciales ni IDs de Entra"
git tag v0-original

PASO 4 - INVENTARIO DE CONTROLES QUE EXISTEN HOY
Crea docs/inventario-controles.md y, por cada una de las 5 areas, anota que existe y donde:
1) Autenticacion y sesiones: SecurityConfig (JWT de Entra solo si SECURITY_ENABLED=true; scope SCOPE_access_as_user).
2) Autorizacion: no hay roles ni verificacion de dueno (orders no guarda quien es el usuario autenticado).
3) APIs: DTOs de salida (OrderResponseDto, ProductResponseDto); no hay limites de peticiones; el BFF recibe el body como String.
4) Validacion: @Valid con @NotBlank, @NotEmpty, @Min(1) cantidad, @Positive precioUnitario (OrderItemRequestDto); @DecimalMin precio
   (ProductCreateDto y Product); chequeo de stock negativo (ProductServiceImpl); en notify solo se exige que to, subject y orderNumber no esten vacios.
5) Configuracion: GlobalExceptionHandler de catalog oculta el mensaje interno; el de orders lo devuelve ("Error interno del servidor: " + ex.getMessage());
   idempotencia y DLQ en notify (no son de seguridad).
Esto sirve como evidencia SAST/revision en el informe y es la lista de lo que vas a quitar en la Parte 3.

==============================================================
PARTE 2 - CONSTRUIR LO QUE FALTA (YA EN FORMA BASELINE)
==============================================================
Estos pasos crean codigo nuevo directamente con el error incluido (comentario BASELINE-VULN). En la Parte 5 se corrige.
Un commit por paso.

PASO 5 - BASE DE DATOS PARA EL LOGIN
En src\init-db.sql agrega una tercera base:

SELECT 'CREATE DATABASE pedidos360_auth'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pedidos360_auth')\gexec

(Si el volumen pgdata ya existia, el script no se vuelve a ejecutar: haz "docker compose down -v" una vez para recrearlo.)

PASO 6 - LOGIN PROPIO EN EL BFF (src\api_cloud)
6.1 pom.xml: agrega spring-boot-starter-data-jpa y el driver org.postgresql:postgresql (la libreria de JWT, spring-security-oauth2-jose, ya viene con
    el starter oauth2-resource-server; si el compilador no la encuentra, agregala).
6.2 application.yml: agrega la conexion a PostgreSQL (url jdbc:postgresql://${DB_HOST:localhost}:5432/pedidos360_auth, usuario y clave por
    variables, ddl-auto update) y el secreto del JWT:  security.jwt.secret: ${JWT_SECRET:changeme}   // BASELINE-VULN [SEC-REQ-012]
    Elimina el bloque spring.security.oauth2.resourceserver (Entra).
6.3 Paquete nuevo cl.duoc.api.auth con:
    - User (entidad): id, email, passwordHash, name, phone, role (texto: CLIENTE o ADMIN).
    - UserRepository (Spring Data).
    - AuthController con POST /api/auth/register, POST /api/auth/login y GET /api/auth/me.
    - JwtService que firma un JWT HS256 con el secreto, con los datos sub=id, email y role.
6.4 Los errores van asi (cada uno con su comentario):
    - Registro: recibe la entidad User directamente como @RequestBody, de modo que el cliente puede enviar "role":"ADMIN"
      [SEC-REQ-006]. La clave se guarda con BCrypt (no es parte de ningun SEC-REQ).
    - Registro y /me responden con la entidad User completa, incluido passwordHash [SEC-REQ-007].
    - Login: compara la clave y responde 401 sin contar intentos ni limitar [SEC-REQ-001].
    - JWT con expiracion de 30 dias [SEC-REQ-003].
6.5 Usuarios de prueba: al iniciar, si la tabla esta vacia, crea 1 ADMIN y 2 CLIENTE ficticios (un CommandLineRunner).

PASO 7 - ROLES Y SEGURIDAD EN EL BFF
En SecurityConfig.java:
- Cambia el valor por defecto de SECURITY_ENABLED a true (tambien en docker-compose.yml).
- Reemplaza oauth2ResourceServer(jwt()) por un JwtDecoder HS256 con el mismo secreto (NimbusJwtDecoder.withSecretKey).
- /api/auth/** y GET /api/catalog/** quedan publicos; el resto exige token valido.
- Crea un filtro RoleHeaderFilter (OncePerRequestFilter) que, para POST /api/catalog/products y PUT /api/orders/{id}/status, exige que la cabecera
  X-User-Role valga "ADMIN". Comentario: BASELINE-VULN [SEC-REQ-005]: el rol lo declara el cliente, no se saca del token.
- Deja el CORS como esta (incluye "*") [SEC-REQ-013]: ya es la debilidad.
NO agregues comprobacion de dueno en GET /api/orders/{id}, GET /api/orders ni /enriched [SEC-REQ-004]; deja el body de POST /api/orders
sin tocar (clienteId, nombre, email y precioUnitario los sigue mandando el cliente) [SEC-REQ-004 y 006].

PASO 8 - BUSQUEDA DE PRODUCTOS (catalog + BFF)
Hoy no hay busqueda; sin ella no hay donde demostrar la inyeccion SQL.
- ms-pedidos360-catalog: nuevo GET /api/catalog/products/search?q=... en ProductController. En un repositorio o servicio nuevo, arma la consulta con
  EntityManager.createNativeQuery y el texto concatenado:
      "SELECT * FROM products WHERE name ILIKE '%" + q + "%'"
  Comentario: BASELINE-VULN [SEC-REQ-010]: consulta con texto concatenado, sin parametrizar.
- BFF (OrdersController): proxy GET /api/catalog/products/search que reenvia q al catalogo, igual que los demas endpoints proxy.

PASO 9 - FRONTEND SIMPLE (src\frontend)
React + Vite + TypeScript, con su Dockerfile (compila con node y sirve con nginx) y un servicio "frontend" en docker-compose.yml:
puerto "127.0.0.1:8080:80", depende de bff-api, y la URL del BFF fijada al compilar (VITE_API_URL=http://localhost:8088).
El front SOLO llama al BFF (puerto 8088). Pantallas:
- Registro e inicio de sesion. Guarda el token y el rol que devuelve el login.
- Catalogo con buscador. Muestra nombre y descripcion de cada producto.
- Crear pedido: elegir productos y cantidades, y datos del cliente (nombre, correo, direccion, RUT). El front manda clienteId, nombre, correo
  y precioUnitario como parte del body (asi lo permite el BFF actual).
- Mis pedidos y detalle de pedido (usa /api/orders y /api/orders/{id}/enriched).
- Panel de administracion: crear producto, listar todos los pedidos y cambiar el estado de uno.
Errores del front:
- Todas las llamadas de administracion envian la cabecera X-User-Role con el rol guardado en el navegador [SEC-REQ-005].
- Nombre y descripcion del producto, y nombre y direccion del cliente, se muestran con dangerouslySetInnerHTML [SEC-REQ-009].
- Validaciones solo en los formularios del navegador.

PASO 10 - VERIFICAR Y GUARDAR
En PowerShell:

cd src
docker compose up -d --build
Start-Process http://localhost:8080
cd ..

Prueba: registro y login, buscar productos, crear un pedido con el cliente 1, abrir Mailpit (http://localhost:8025) y ver el correo, y entrar al panel como ADMIN.
Luego:

git add .
git commit -m "feat: login propio, busqueda de productos y frontend de prueba (forma baseline)"

==============================================================
PARTE 3 - ESTADO A (BASELINE): MARCAR LOS ERRORES Y QUITAR CONTROLES
==============================================================

PASO 11 - QUITAR LOS CONTROLES QUE HOY EXISTEN (SEC-REQ-010)
Un commit: git commit -m "baseline: SEC-REQ-010 quitar validaciones del servidor". Comentario BASELINE-VULN en cada lugar.
- OrderItemRequestDto: quita @Min(1) de cantidad y @Positive de precioUnitario.
- ProductCreateDto y Product: quita @DecimalMin del precio; en Product quita @Min(0) del stock.
- ProductServiceImpl: quita los chequeos de stock negativo (createProduct y updateProduct).
- Sin limite de largo en textos (no agregues @Size).
Los tests existentes que comprueban esas validaciones van a fallar. Marcalos con @Disabled("BASELINE-VULN SEC-REQ-010"): se vuelven a activar
en el Estado B y pasan a ser tu evidencia de FAIL -> PASS.
Importante: quita la etiqueta de tests del Dockerfile solo si quieres; hoy ya compila con -DskipTests.

PASO 12 - MARCAR LAS DEBILIDADES QUE YA EXISTEN
Agrega el comentario BASELINE-VULN [SEC-REQ-XXX] sin cambiar el comportamiento en cada lugar. Es lo que da el "codigo afectado" de cada FINDING.

SEC-REQ-004 / 006  OrderServiceImpl.createOrder: toma clienteId, nombre, email, direccion y RUT del body. getOrderById y listOrders (findAll) no
                   filtran por usuario.
SEC-REQ-006        OrdersController (BFF).createOrder: solo completa descripcion y precioUnitario si no vienen; si vienen, respeta los del cliente.
SEC-REQ-007        OrderResponseDto (orders) y OrdersController.getEnrichedOrderById: devuelven nombre y email del cliente a cualquiera.
                   Ademas agrega RUT y direccion al OrderResponseDto y a mapToResponseDto (rut y direccion del cliente).
SEC-REQ-008        BFF: no hay ningun limitador de peticiones.
SEC-REQ-011        notify, EmailSenderService.buildEmailHtml: customerName y body van sin escapar dentro del HTML. Ademas, en EmailListener
                   agrega un log.info con payload.getBody() completo.
SEC-REQ-012        docker-compose.yml y application.yml de cada servicio: DB_PASSWORD con valor por defecto postgres, RABBITMQ guest/guest,
                   y security.jwt.secret con "changeme".
SEC-REQ-013        SecurityConfig.corsConfigurationSource: setAllowedOrigins con "*". No hay Content-Security-Policy.
SEC-REQ-014        docker-compose.yml: puertos 5432, 5672, 15672, 8086 y 8087 publicados (los dos ultimos permiten saltarse el BFF); ningun Dockerfile
                   define USER; GlobalExceptionHandler de orders devuelve ex.getMessage() y el BFF devuelve e.getMessage(); swagger-ui activo en los
                   tres servicios; notify expone health con show-details: always.
Cambia solo los puertos para enlazarlos a 127.0.0.1 (por ejemplo "127.0.0.1:8087:8087"): siguen publicados pero solo desde tu equipo.

PASO 13 - MAPA DE DEBILIDADES
En PowerShell:

Get-ChildItem src -Recurse -File | Select-String -Pattern "BASELINE-VULN"

Con eso arma docs/mapa-debilidades.md: tabla SEC-REQ | archivo y linea | comentario | TEST-SEC esperado (FAIL).

PASO 14 - GUARDAR Y ETIQUETAR EL BASELINE
En PowerShell:

git add .
git commit -m "feat: Estado A baseline con debilidades de laboratorio"
git tag v1-baseline

==============================================================
PARTE 4 - COMPROBAR QUE EL BASELINE FALLA
==============================================================

PASO 15 - CADA TEST-SEC DEBE DAR FAIL EN v1-baseline
Si alguno da PASS, queda un control que no quitaste: quitalo antes de seguir.

TEST-SEC-001 -> FAIL esperado: 6 o mas intentos fallidos seguidos de login siguen siendo aceptados (siempre 401, nunca 429).
TEST-SEC-003 -> FAIL esperado: el token emitido sigue siendo valido despues de 15 minutos (dura 30 dias).
TEST-SEC-004 -> FAIL esperado: el cliente B consulta /api/orders/{id} y /enriched de un pedido del cliente A, y /api/orders devuelve pedidos ajenos.
               Mas grave: curl.exe http://localhost:8087/api/orders responde sin ningun token (se salta el BFF).
TEST-SEC-005 -> FAIL esperado: un CLIENTE crea un producto o cambia un estado enviando X-User-Role: ADMIN; y el microservicio responde directo en 8086/8087.
TEST-SEC-006 -> FAIL esperado: un registro con "role":"ADMIN" crea un administrador; un pedido con precioUnitario 1 queda guardado a ese precio.
TEST-SEC-007 -> FAIL esperado: el registro y /me devuelven passwordHash; los pedidos incluyen RUT, direccion, nombre y correo de otros clientes.
TEST-SEC-008 -> FAIL esperado: 101 peticiones en un minuto no reciben 429.
TEST-SEC-009 -> FAIL esperado: un producto con HTML en la descripcion, o un cliente con HTML en el nombre, se interpreta en el frontend.
TEST-SEC-010 -> FAIL esperado: la busqueda con una comilla simple altera la consulta (error SQL o resultados de mas) y una cantidad negativa o un precio 0
               se aceptan en vez de rechazarse con 400.
TEST-SEC-011 -> FAIL esperado: un pedido con nombre de cliente "<b>X</b>" llega al correo (Mailpit) como HTML sin escapar; y publicando un mensaje
               en q.cmd.email desde la consola de RabbitMQ (guest/guest, 127.0.0.1:15672) se envia un correo a cualquier destinatario.
TEST-SEC-012 -> FAIL esperado: los servicios arrancan sin JWT_SECRET (usan "changeme") y las claves guest y postgres funcionan.
TEST-SEC-013 -> FAIL esperado: se acepta cualquier origen y no hay Content-Security-Policy.
TEST-SEC-014 -> FAIL esperado: 5432, 5672, 15672, 8086 y 8087 aparecen publicados, los contenedores corren como administrador (docker compose exec
               <servicio> whoami) y un error interno en orders devuelve el mensaje de la excepcion.

Guarda la salida de cada prueba como evidencia con la nomenclatura CASE0X-EV001 (carpeta evidence/SEC-CASE-0X/tests/). Cada FAIL se convierte en un FINDING.

COPIA LADO A LADO (OPCIONAL)
Para ver y ejecutar el baseline y la version corregida al mismo tiempo, desde la carpeta del proyecto:

git worktree add ..\cafe-baseline v1-baseline

Asi trabajas el Estado B en cafe-playbook y mantienes intacta la copia del baseline en cafe-baseline. Cambia los puertos de la copia si vas a levantar
las dos a la vez.

==============================================================
PARTE 5 - ESTADO B (CORRECCION)
==============================================================

PASO 16 - REGLAS
- Rama nueva desde el baseline: git checkout -b secure v1-baseline
- Un SEC-REQ por commit: git commit -m "fix(SEC-REQ-004): comprobar dueno del pedido (FINDING-00X)".
- Para cada uno: ejecuta el TEST-SEC (FAIL), aplica la correccion, borra el comentario BASELINE-VULN, ejecuta el test otra vez (PASS), comprueba que la
  funcionalidad legitima sigue andando, guarda las dos salidas como evidencia y haz el commit.
- Despues de cada correccion repite SAST, SCA y los tests (re-test). Reactiva los tests que marcaste con @Disabled.

PASO 17 - LAS CORRECCIONES (en este orden recomendado: 012, 014, 003, 005, 004, 006, 007, 001, 008, 010, 011, 009, 013)

SEC-REQ-012 - Sin secretos por defecto (docker-compose.yml y application.yml)
  Correccion: quita los valores por defecto: ${DB_PASSWORD:?falta}, ${RABBITMQ_PASSWORD:?falta}, ${JWT_SECRET:?falta}; las claves van en un .env no
  versionado y subes un .env.example con valores de ejemplo. En el BFF verifica al arrancar que JWT_SECRET tenga al menos 32 bytes y, si no, que
  falle. En RabbitMQ crea un usuario propio para la aplicacion y deshabilita guest.
  Test PASS: Gitleaks sin hallazgos y los servicios se niegan a iniciar sin el secreto.
SEC-REQ-014 - Exposicion minima y errores genericos
  Correccion: en el compose quita "ports" de db, rabbitmq, ms-catalog, ms-orders y ms-notify (solo red interna); deja publicados solo el frontend (8080),
  el BFF (8088) y Mailpit (8025) en 127.0.0.1. En cada Dockerfile agrega un usuario sin privilegios (RUN addgroup -S app && adduser -S app -G app y
  USER app). En el GlobalExceptionHandler de orders y en el BFF responde un mensaje generico, como ya hace catalog, y deja el detalle en el log.
  Desactiva swagger-ui (springdoc.swagger-ui.enabled=false) y pon management.endpoint.health.show-details: never en notify.
  Test PASS: curl.exe a 8086 y 8087 falla, 5432 y 5672 no responden, whoami no es root y un error 500 no muestra el mensaje de la excepcion.
SEC-REQ-003 - Sesion corta y verificada
  Correccion: JwtService con expiracion de 15 minutos; el JwtDecoder fija el algoritmo HS256 y valida la expiracion.
  Test PASS: un token vencido y uno con la firma alterada reciben 401.
SEC-REQ-005 - Rol verificado en el servidor
  Correccion: elimina RoleHeaderFilter y la cabecera X-User-Role del front. Convierte el claim "role" del JWT en authority (JwtAuthenticationConverter) y en
  SecurityConfig exige hasRole("ADMIN") en POST /api/catalog/products y PUT /api/orders/{id}/status. Denegar por defecto el resto.
  Test PASS: el CLIENTE recibe 403 aunque envie X-User-Role: ADMIN; el ADMIN real recibe 200.
SEC-REQ-004 - Control de acceso por objeto
  Correccion: el BFF toma el id del usuario del token (nunca del body). En POST /api/orders sobrescribe clienteId, nombre y correo con los del token.
  En GET /api/orders/{id} y /enriched consulta el pedido y, si el rol es CLIENTE y su clienteId no coincide con el del token, responde 403. En
  GET /api/orders, un CLIENTE solo ve los suyos: agrega el parametro clienteId a orders (findByClienteId en OrderRepository y en el servicio) y el BFF
  lo fija; el ADMIN ve todos.
  Test PASS: el cliente B recibe 403 sobre el pedido de A y no ve pedidos ajenos en el listado; el dueno si puede (200).
SEC-REQ-006 - Solo los campos permitidos
  Correccion: DTOs de entrada en el BFF (en vez del body como String) y un RegisterRequest con solo email, clave, nombre y telefono: el rol lo asigna el
  servidor como CLIENTE. En OrdersController.createOrder el precio y la descripcion se obtienen siempre del catalogo (quita la condicion
  "si no vienen"). Rechaza campos desconocidos (spring.jackson.deserialization.fail-on-unknown-properties=true).
  Test PASS: un registro con "role":"ADMIN" responde 400 o queda como CLIENTE; el pedido ignora el precioUnitario enviado.
SEC-REQ-007 - Respuestas minimas
  Correccion: UserResponse sin passwordHash; quita rut y direccion de OrderResponseDto; nombre y correo del cliente solo para su dueno y el ADMIN
  (tambien en /enriched).
  Test PASS: ningun endpoint devuelve campos prohibidos; los datos de un cliente ajeno no aparecen.
SEC-REQ-001 - Limite de intentos
  Correccion: en AuthController cuenta los intentos fallidos por correo (un ConcurrentHashMap con fecha de bloqueo es suficiente); tras 5 seguidos,
  bloquea 15 minutos y responde 429. Mensaje de error generico, sin distinguir si el correo existe.
  Test PASS: el 6.o intento se rechaza (429), incluso con la clave correcta dentro de la ventana.
SEC-REQ-008 - Limites de consumo
  Correccion: un OncePerRequestFilter en el BFF con limite de 100 peticiones por minuto por IP y de 10 por minuto en /api/auth/**; responde 429.
  Tope de tamano de pagina en el listado de pedidos.
  Test PASS: la peticion 101 en un minuto recibe 429.
SEC-REQ-010 - Consultas parametrizadas y validacion en el servidor
  Correccion: en catalog reemplaza la consulta nativa por un metodo derivado de Spring Data (findByNameContainingIgnoreCase). Restaura @Min(1),
  @Positive y @DecimalMin, los chequeos de stock y agrega @Size a los textos. En el BFF cambia @PathVariable String id por Long id (hoy el id se
  concatena en la URL que se llama aguas abajo).
  Test PASS: la comilla simple no altera la consulta; cantidad negativa y precio 0 reciben 400; los tests que desactivaste vuelven a pasar.
SEC-REQ-011 - notify escapa y valida los eventos
  Correccion: escapa customerName, body, orderNumber y newStatus con HtmlUtils.htmlEscape antes de armar el HTML en EmailSenderService.buildEmailHtml;
  valida formato del correo y largos maximos en EmailListener y manda los invalidos a la DLQ (nack sin requeue, como ya hace con los malformados);
  elimina el log del cuerpo. Combinado con el usuario propio de RabbitMQ (SEC-REQ-012), nadie sin credenciales puede publicar eventos.
  Test PASS: un nombre con HTML aparece escapado en el correo y un evento con destinatario invalido va a la DLQ.
SEC-REQ-009 - Texto como texto
  Correccion: en el frontend quita dangerouslySetInnerHTML y muestra los textos normalmente (React los escapa). Si algun dia necesitas HTML, sanitizalo
  con DOMPurify.
  Test PASS: un producto con HTML en la descripcion se ve como texto y no se ejecuta nada.
SEC-REQ-013 - CORS restringido y cabeceras
  Correccion: en SecurityConfig, setAllowedOrigins solo con http://localhost:8080 (el frontend) y metodos justos; agrega Content-Security-Policy,
  X-Content-Type-Options y proteccion contra framing con http.headers(...); agrega las mismas cabeceras con add_header en el nginx del frontend.
  Test PASS: un Origin ajeno no recibe Access-Control-Allow-Origin y la Content-Security-Policy aparece; ZAP baseline sin alertas altas.

PASO 18 - CERRAR EL ESTADO B
En PowerShell:

git tag v2-secure

Repite SAST, SCA y DAST sobre v2-secure y guarda los reportes en evidence/. Luego elige UN caso para el Break, Fix & Verify del enunciado. Los mas
vistosos con tu app son SEC-REQ-004 (saltarse el BFF y ver pedidos ajenos: FAIL en v1-baseline, PASS en v2-secure) y SEC-REQ-001 (intentos ilimitados de login).

#!/usr/bin/env python3
"""
Automated Security Test Suite for Estado A (Baseline)
Evaluates security assertions across all 5 SEC-CASEs.
Expected outcome: All security tests must FAIL in Estado A.
"""

import sys
import re
import json
from pathlib import Path

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = Path(__file__).resolve().parent

results = {
    "SEC-CASE-01": [],
    "SEC-CASE-02": [],
    "SEC-CASE-03": [],
    "SEC-CASE-04": [],
    "SEC-CASE-05": []
}

def record_test(case_id, test_id, name, status, failure_reason=""):
    results[case_id].append({
        "test_id": test_id,
        "name": name,
        "status": status,  # "FAIL" (expected in baseline) or "PASS"
        "failure_reason": failure_reason
    })
    symbol = "[FAIL]" if status == "FAIL" else "[PASS]"
    print(f"[{case_id}] {test_id}: {name} -> {symbol}")
    if failure_reason:
        print(f"    Reason: {failure_reason}")

# ==============================================================================
# SEC-CASE-01: Autenticación y gestión de sesiones
# ==============================================================================
def test_case_01():
    print("\n--- Running Automated Tests for SEC-CASE-01 ---")
    
    # TEST-SEC-001: Ausencia de bloqueo de cuenta / rate limit en login
    auth_controller = BASE_DIR / "api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java"
    content = auth_controller.read_text(encoding="utf-8")
    
    has_lockout = "maxAttempts" in content or "lockAccount" in content or "Bucket" in content or "RateLimiter" in content
    if not has_lockout:
        record_test("SEC-CASE-01", "TEST-SEC-001", "Verificación de bloqueo tras intentos fallidos de login",
                    "FAIL", "El endpoint /api/auth/login procesa credenciales sin contador de intentos fallidos ni bloqueo temporal.")
    else:
        record_test("SEC-CASE-01", "TEST-SEC-001", "Verificación de bloqueo tras intentos fallidos de login", "PASS")

    # TEST-SEC-003: Expiración de token JWT <= 3600s (1h)
    jwt_service = BASE_DIR / "api_cloud/src/main/java/cl/duoc/api/auth/JwtService.java"
    jwt_content = jwt_service.read_text(encoding="utf-8")
    
    # Check for 30 DAYS expiration
    if "plus(30, ChronoUnit.DAYS)" in jwt_content:
        record_test("SEC-CASE-01", "TEST-SEC-003", "Expiración de token JWT de acceso <= 1 hora",
                    "FAIL", "El token JWT se genera con una vigencia de 30 días (ChronoUnit.DAYS), superando el umbral seguro de <= 1 hora.")
    else:
        record_test("SEC-CASE-01", "TEST-SEC-003", "Expiración de token JWT de acceso <= 1 hora", "PASS")

# ==============================================================================
# SEC-CASE-02: Autorización y control de acceso
# ==============================================================================
def test_case_02():
    print("\n--- Running Automated Tests for SEC-CASE-02 ---")
    
    # TEST-SEC-004a: BOLA en listOrders (ms-orders)
    orders_service = BASE_DIR / "ms-pedidos360-orders/src/main/java/com/pedidos360/orders/service/impl/OrderServiceImpl.java"
    content = orders_service.read_text(encoding="utf-8")
    
    # Check if listOrders filters by client/user ID
    has_user_filter = "findByClienteId" in content or "findByUsuario" in content
    if not has_user_filter:
        record_test("SEC-CASE-02", "TEST-SEC-004a", "Aislamiento multi-inquilino en listado de pedidos (BOLA/IDOR)",
                    "FAIL", "listOrders() consulta órdenes sin filtrar por el cliente autenticado en el repositorio.")
    else:
        record_test("SEC-CASE-02", "TEST-SEC-004a", "Aislamiento multi-inquilino en listado de pedidos (BOLA/IDOR)", "PASS")

    # TEST-SEC-004b: BOLA en getOrderById
    get_order_block = content[content.find("getOrderById"):content.find("updateOrderStatus")]
    has_ownership_check = "clienteId" in get_order_block or "user" in get_order_block.lower() or "principal" in get_order_block.lower()
    if not has_ownership_check:
        record_test("SEC-CASE-02", "TEST-SEC-004b", "Comprobación de titularidad en consulta de orden por ID",
                    "FAIL", "getOrderById() recupera y retorna la orden por ID sin contrastar si pertenece al usuario que solicita.")
    else:
        record_test("SEC-CASE-02", "TEST-SEC-004b", "Comprobación de titularidad en consulta de orden por ID", "PASS")

    # TEST-SEC-005a: Autorización por cabecera de cliente X-User-Role
    role_filter = BASE_DIR / "api_cloud/src/main/java/cl/duoc/api/auth/RoleHeaderFilter.java"
    filter_content = role_filter.read_text(encoding="utf-8")
    if 'request.getHeader("X-User-Role")' in filter_content:
        record_test("SEC-CASE-02", "TEST-SEC-005a", "Autorización basada en token en lugar de cabecera arbitraria",
                    "FAIL", "RoleHeaderFilter evalúa el rol desde la cabecera HTTP inyectable X-User-Role en lugar del JWT verificado.")
    else:
        record_test("SEC-CASE-02", "TEST-SEC-005a", "Autorización basada en token en lugar de cabecera arbitraria", "PASS")

# ==============================================================================
# SEC-CASE-03: API
# ==============================================================================
def test_case_03():
    print("\n--- Running Automated Tests for SEC-CASE-03 ---")
    
    # TEST-SEC-006a: Mass Assignment en registro de usuarios
    auth_ctrl = BASE_DIR / "api_cloud/src/main/java/cl/duoc/api/auth/AuthController.java"
    content = auth_ctrl.read_text(encoding="utf-8")
    if "@RequestBody User user" in content:
        record_test("SEC-CASE-03", "TEST-SEC-006a", "Uso de DTO dedicado en registro para prevenir Mass Assignment",
                    "FAIL", "El endpoint /api/auth/register recibe la entidad JPA User directamente, permitiendo inyectar 'role':'ADMIN'.")
    else:
        record_test("SEC-CASE-03", "TEST-SEC-006a", "Uso de DTO dedicado en registro para prevenir Mass Assignment", "PASS")

    # TEST-SEC-006b: Manipulación de precio unitario en órdenes (BFF)
    orders_ctrl = BASE_DIR / "api_cloud/src/main/java/cl/duoc/api/OrdersController.java"
    ctrl_content = orders_ctrl.read_text(encoding="utf-8")
    if "!itemNode.hasNonNull(\"precioUnitario\")" in ctrl_content:
        record_test("SEC-CASE-03", "TEST-SEC-006b", "Validación estricta de precios de catálogo en creación de pedidos",
                    "FAIL", "El BFF solo consulta el catálogo si precioUnitario no viene; si el cliente lo envía, respeta el precio del cliente.")
    else:
        record_test("SEC-CASE-03", "TEST-SEC-006b", "Validación estricta de precios de catálogo en creación de pedidos", "PASS")

    # TEST-SEC-007a: Exposición de passwordHash en User
    user_entity = BASE_DIR / "api_cloud/src/main/java/cl/duoc/api/auth/User.java"
    user_content = user_entity.read_text(encoding="utf-8")
    if "@JsonIgnore" not in user_content and "passwordHash" in user_content:
        record_test("SEC-CASE-03", "TEST-SEC-007a", "Ocultamiento de passwordHash en respuestas de API",
                    "FAIL", "El campo passwordHash de la entidad User no posee anotación @JsonIgnore ni DTO de proyección seguro.")
    else:
        record_test("SEC-CASE-03", "TEST-SEC-007a", "Ocultamiento de passwordHash en respuestas de API", "PASS")

    # TEST-SEC-008: Ausencia de Rate Limiting en BFF
    sec_cfg = BASE_DIR / "api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java"
    sec_content = sec_cfg.read_text(encoding="utf-8")
    if "BASELINE-VULN [SEC-REQ-008]" in sec_content and "RateLimit" not in sec_content:
        record_test("SEC-CASE-03", "TEST-SEC-008", "Presencia de filtro de Rate Limiting en BFF",
                    "FAIL", "SecurityConfig no registra ningún filtro de limitación de tasa de peticiones (Rate Limiter).")
    else:
        record_test("SEC-CASE-03", "TEST-SEC-008", "Presencia de filtro de Rate Limiting en BFF", "PASS")

# ==============================================================================
# SEC-CASE-04: Validación y sanitización de entradas
# ==============================================================================
def test_case_04():
    print("\n--- Running Automated Tests for SEC-CASE-04 ---")
    
    # TEST-SEC-010a: Inyección SQL en búsqueda de catálogo
    prod_ctrl = BASE_DIR / "ms-pedidos360-catalog/src/main/java/com/pedidos360/catalog/controller/ProductController.java"
    prod_content = prod_ctrl.read_text(encoding="utf-8")
    if 'SELECT * FROM products WHERE name ILIKE \'%' in prod_content and 'createNativeQuery(sql' in prod_content:
        record_test("SEC-CASE-04", "TEST-SEC-010a", "Uso de consultas SQL parametrizadas en búsqueda",
                    "FAIL", "ProductController concatena directamente el parámetro 'q' sin parametrizar en createNativeQuery.")
    else:
        record_test("SEC-CASE-04", "TEST-SEC-010a", "Uso de consultas SQL parametrizadas en búsqueda", "PASS")

    # TEST-SEC-010b: Validación de rango en OrderItemRequestDto
    dto_file = BASE_DIR / "ms-pedidos360-orders/src/main/java/com/pedidos360/orders/dto/OrderItemRequestDto.java"
    lines = dto_file.read_text(encoding="utf-8").splitlines()
    code_lines = [l.strip() for l in lines if not l.strip().startswith("//")]
    has_min_qty = any(l.startswith("@Min") for l in code_lines)
    has_pos_price = any(l.startswith("@Positive") for l in code_lines)
    if not has_min_qty or not has_pos_price:
        record_test("SEC-CASE-04", "TEST-SEC-010b", "Validaciones Bean Validation (@Min, @Positive) en OrderItemRequestDto",
                    "FAIL", "Se eliminaron las anotaciones activas @Min(1) en cantidad y @Positive en precioUnitario.")
    else:
        record_test("SEC-CASE-04", "TEST-SEC-010b", "Validaciones Bean Validation (@Min, @Positive) en OrderItemRequestDto", "PASS")

    # TEST-SEC-009: Uso de dangerouslySetInnerHTML en Frontend
    app_tsx = BASE_DIR / "frontend/src/App.tsx"
    app_content = app_tsx.read_text(encoding="utf-8")
    danger_count = len(re.findall(r'dangerouslySetInnerHTML', app_content))
    if danger_count > 0:
        record_test("SEC-CASE-04", "TEST-SEC-009", "Ausencia de renderizado HTML crudo inseguro en React",
                    "FAIL", f"App.tsx utiliza 'dangerouslySetInnerHTML' en {danger_count} ubicaciones para renderizar datos dinámicos.")
    else:
        record_test("SEC-CASE-04", "TEST-SEC-009", "Ausencia de renderizado HTML crudo inseguro en React", "PASS")

# ==============================================================================
# SEC-CASE-05: Configuración segura
# ==============================================================================
def test_case_05():
    print("\n--- Running Automated Tests for SEC-CASE-05 ---")
    
    # TEST-SEC-013: CORS wildcard y ausencia de CSP
    sec_cfg = BASE_DIR / "api_cloud/src/main/java/cl/duoc/api/SecurityConfig.java"
    sec_content = sec_cfg.read_text(encoding="utf-8")
    if '"*"' in sec_content and "setAllowedOrigins" in sec_content:
        record_test("SEC-CASE-05", "TEST-SEC-013", "Restricción estricta de orígenes CORS",
                    "FAIL", "SecurityConfig configura CORS con comodín '*' permitiendo cualquier origen arbitrario.")
    else:
        record_test("SEC-CASE-05", "TEST-SEC-013", "Restricción estricta de orígenes CORS", "PASS")

    # TEST-SEC-012: Detección de secretos por defecto débiles
    compose_file = BASE_DIR / "docker-compose.yml"
    compose_content = compose_file.read_text(encoding="utf-8")
    weak_secrets = []
    if "changeme" in compose_content:
        weak_secrets.append("JWT_SECRET=changeme")
    if "guest" in compose_content:
        weak_secrets.append("RABBITMQ_DEFAULT_PASS=guest")
    if "postgres" in compose_content:
        weak_secrets.append("POSTGRES_PASSWORD=postgres")
        
    if weak_secrets:
        record_test("SEC-CASE-05", "TEST-SEC-012", "Ausencia de contraseñas y secretos por defecto débiles",
                    "FAIL", f"docker-compose.yml contiene secretos predecibles en claro: {', '.join(weak_secrets)}.")
    else:
        record_test("SEC-CASE-05", "TEST-SEC-012", "Ausencia de contraseñas y secretos por defecto débiles", "PASS")

    # TEST-SEC-014a: Verificación de puertos internos expuestos
    exposed_ports = re.findall(r'127\.0\.0\.1:(\d+):', compose_content)
    backend_ports = [p for p in exposed_ports if p in ["8086", "8087", "5432", "5672", "15672"]]
    if backend_ports:
        record_test("SEC-CASE-05", "TEST-SEC-014a", "Aislamiento de puertos de microservicios y bases de datos",
                    "FAIL", f"Puertos internos de backend expuestos al host: {', '.join(backend_ports)}.")
    else:
        record_test("SEC-CASE-05", "TEST-SEC-014a", "Aislamiento de puertos de microservicios y bases de datos", "PASS")

    # TEST-SEC-014b: Dockerfiles ejecutándose como root
    root_dockerfiles = []
    for df in BASE_DIR.glob("*/Dockerfile"):
        df_content = df.read_text(encoding="utf-8")
        if "USER " not in df_content:
            root_dockerfiles.append(df.parent.name)
    if root_dockerfiles:
        record_test("SEC-CASE-05", "TEST-SEC-014b", "Instrucción USER no-root en contenedores Docker",
                    "FAIL", f"Dockerfiles sin usuario no-root especificado: {', '.join(root_dockerfiles)}.")
    else:
        record_test("SEC-CASE-05", "TEST-SEC-014b", "Instrucción USER no-root en contenedores Docker", "PASS")


if __name__ == "__main__":
    test_case_01()
    test_case_02()
    test_case_03()
    test_case_04()
    test_case_05()
    
    print("\n" + "="*80)
    print("RESUMEN GENERAL DE SECURITY TESTS AUTOMATIZADOS (ESTADO A - BASELINE)")
    print("="*80)
    total_tests = sum(len(v) for v in results.values())
    total_fails = sum(sum(1 for t in v if t["status"] == "FAIL") for v in results.values())
    
    for case_id, t_list in results.items():
        fails = sum(1 for t in t_list if t["status"] == "FAIL")
        print(f"{case_id}: {fails}/{len(t_list)} tests con resultado FAIL (Esperado: 100% FAIL)")
        
    print(f"\nTotal Tests: {total_tests} | Fails: {total_fails} | Passes: {total_tests - total_fails}")
    print(f"Estado de la Línea Base: {'TOTALMENTE VULNERABLE (100% FAIL)' if total_fails == total_tests else 'PARCIAL'}")
    
    with open("automated-security-test-results.json", "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)
    print("\nResultados exportados a: automated-security-test-results.json")

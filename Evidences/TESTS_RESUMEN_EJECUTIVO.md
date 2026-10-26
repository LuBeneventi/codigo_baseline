# Fase VII — Security Testing: Resumen Ejecutivo Security Tests Automatizados (Estado A - Baseline)

**Fecha de ejecución:** 16 de Octubre de 2026  
**Objetivo:** Demostrar de forma automatizada y reproducible el estado vulnerable inicial (**FAIL**) para los 5 casos de prueba de seguridad mediante pruebas de regresión de seguridad (Security Unit/Integration Tests).  
**Script ejecutor:** `run_automated_security_tests.py`

---

## 1. Matriz General de Casos vs Resultados de Tests Automatizados

| Caso de Seguridad | Tests Automatizados Ejecutados | Tests Fallidos (FAIL) | Tests Exitosos (PASS) | Tasa de Falla en Baseline | Resultado Estado A |
|---|---|:---:|:---:|:---:|:---:|
| **SEC-CASE-01: Autenticación y gestión de sesiones** | TEST-SEC-001 (Lockout login)<br>TEST-SEC-003 (Expiración JWT <= 1h) | 2 | 0 | 100% | ❌ **FAIL** |
| **SEC-CASE-02: Autorización y control de acceso** | TEST-SEC-004a (BOLA en listOrders)<br>TEST-SEC-004b (IDOR en getOrderById)<br>TEST-SEC-005a (Bypass rol vía cabecera) | 3 | 0 | 100% | ❌ **FAIL** |
| **SEC-CASE-03: Seguridad de API** | TEST-SEC-006a (Mass Assignment registro)<br>TEST-SEC-006b (Manipulación de precio)<br>TEST-SEC-007a (Exposición passwordHash)<br>TEST-SEC-008 (Ausencia Rate Limiter) | 4 | 0 | 100% | ❌ **FAIL** |
| **SEC-CASE-04: Validación y sanitización de entradas** | TEST-SEC-010a (SQL Injection)<br>TEST-SEC-010b (Validación cantidades/precios)<br>TEST-SEC-009 (DOM XSS dangerouslySetInnerHTML) | 3 | 0 | 100% | ❌ **FAIL** |
| **SEC-CASE-05: Configuración segura** | TEST-SEC-013 (CORS wildcard `*`)<br>TEST-SEC-012 (Secretos débiles por defecto)<br>TEST-SEC-014a (Puertos backend expuestos)<br>TEST-SEC-014b (Contenedores como root) | 4 | 0 | 100% | ❌ **FAIL** |

**Total de Pruebas Automatizadas:** 16  
**Total de Fallas (FAIL):** 16 (100%)  
**Estado Global de la Línea Base:** ❌ **TOTALMENTE VULNERABLE (100% FAIL)**

---

## 2. Mapa de Evidencias de Tests Generadas

```text
Evidences/
├── SEC-CASE-01/
│   └── tests/
│       ├── CASE01-EV001-test-report.md     # Reporte de tests automatizados de autenticación
│        └── CASE01-EV002-test-results.json  # Resultado JSON de aserciones
├── SEC-CASE-02/
│   └── tests/
│       ├── CASE02-EV001-test-report.md     # Reporte de tests automatizados de autorización
│       └── CASE02-EV002-test-results.json  # Resultado JSON de aserciones
├── SEC-CASE-03/
│   └── tests/
│       ├── CASE03-EV001-test-report.md     # Reporte de tests automatizados de API y datos
│       └── CASE03-EV002-test-results.json  # Resultado JSON de aserciones
├── SEC-CASE-04/
│   └── tests/
│       ├── CASE04-EV001-test-report.md     # Reporte de tests automatizados de inyección y validación
│       └── CASE04-EV002-test-results.json  # Resultado JSON de aserciones
└── SEC-CASE-05/
    └── tests/
        ├── CASE05-EV001-test-report.md     # Reporte de tests automatizados de configuración segura
        └── CASE05-EV002-test-results.json  # Resultado JSON de aserciones
```

---

## 3. Conclusión de la Etapa de Tests Automatizados

1. Se consolidó una suite ejecutable (`run_automated_security_tests.py`) que evalúa las aserciones de seguridad esperadas en el Estado B directamente contra el código del Estado A.
2. La totalidad de las pruebas (16/16) **FALLAN** de forma consistente, dejando demostrada la necesidad de cada corrección de seguridad planteada en el laboratorio.

# Fase VII — Security Testing: Resumen Ejecutivo SAST (Estado A - Baseline)

**Fecha de ejecución:** 14 de Octubre de 2026  
**Objetivo:** Demostrar el estado vulnerable inicial (**FAIL**) para los 5 casos de prueba de seguridad mediante Análisis Estático de Seguridad de Aplicaciones (SAST).  
**Herramientas utilizadas:**
- **Semgrep 1.181.0** (Rulesets: `p/default`, `p/owasp-top-ten`, `p/security-audit`, `p/secrets`)
- **Revisión de Código Estática Manual** (Catálogo trazable con marcadores `BASELINE-VULN`)

---

## 1. Matriz General de Casos vs Resultados SAST

| Caso de Seguridad | Requisitos Asociados | Hallazgos Automatizados (Semgrep) | Hallazgos Manuales (BASELINE-VULN) | Resultado Estado A |
|---|---|:---:|:---:|:---:|
| **SEC-CASE-01: Autenticación y gestión de sesiones** | SEC-REQ-001, SEC-REQ-003 | 0 | 2 | ❌ **FAIL** |
| **SEC-CASE-02: Autorización y control de acceso** | SEC-REQ-004, SEC-REQ-005 | 0 | 5 | ❌ **FAIL** |
| **SEC-CASE-03: Seguridad de API** | SEC-REQ-006, SEC-REQ-007, SEC-REQ-008 | 0 | 6 | ❌ **FAIL** |
| **SEC-CASE-04: Validación y sanitización de entradas** | SEC-REQ-009, SEC-REQ-010, SEC-REQ-011 | 1 (SQLi) | 5 | ❌ **FAIL** |
| **SEC-CASE-05: Configuración segura** | SEC-REQ-012, SEC-REQ-013, SEC-REQ-014 | 9 (Docker/Compose) | 5 | ❌ **FAIL** |

---

## 2. Mapa de Evidencias Generadas

Cada caso cuenta con su reporte detallado y artefactos JSON en el directorio `Evidences/`:

```text
Evidences/
├── SEC-CASE-01/
│   └── SAST/
│       ├── CASE01-EV001-sast-report.md    # Reporte de fuerza bruta y expiración JWT
│       └── CASE01-EV002-findings.json       # Trazas de código fuente
├── SEC-CASE-02/
│   └── SAST/
│       ├── CASE02-EV001-sast-report.md    # Reporte de BOLA/IDOR y escalamiento por cabecera
│       └── CASE02-EV002-findings.json       # Trazas de código fuente
├── SEC-CASE-03/
│   └── SAST/
│       ├── CASE03-EV001-sast-report.md    # Reporte de Mass Assignment, fuga de datos y Rate Limit
│       └── CASE03-EV002-findings.json       # Trazas de código fuente
├── SEC-CASE-04/
│   └── SAST/
│       ├── CASE04-EV001-sast-report.md    # Reporte de SQLi, XSS, inyección HTML y falta de validaciones
│       └── CASE04-EV002-semgrep.json        # Evidencia cruda Semgrep (tainted-sql-string)
└── SEC-CASE-05/
    └── SAST/
        ├── CASE05-EV001-sast-report.md    # Reporte de contenedores root, secretos débiles y CORS
        └── CASE05-EV002-semgrep.json        # Evidencia cruda Semgrep (missing-user, compose privileges)
```

---

## 3. Conclusión de la Etapa SAST

1. Se confirmó la presencia y trazabilidad de todas las vulnerabilidades del **Estado A (Baseline)**.
2. Todas las pruebas SAST concluyen en **FAIL**, logrando el objetivo académico de tener una línea base vulnerable medible.
3. El proyecto queda listo para continuar con las siguientes actividades de la **Fase VII (Security Testing)**:
   - **SCA (Software Composition Analysis)**: Dependencias vulnerables (OWASP Dependency-Check / npm audit / trivy).
   - **Pruebas Automatizadas (Tests unitarios e integración de seguridad)**.
   - **DAST (Dynamic Application Security Testing)**: Pruebas dinámicas con ZAP o peticiones cURL automatizadas contra los servicios desplegados.

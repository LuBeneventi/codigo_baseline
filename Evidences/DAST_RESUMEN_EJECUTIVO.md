# Fase VII — Security Testing: Resumen Ejecutivo DAST (Estado A - Baseline)

**Fecha de ejecución:** 15 de Octubre de 2026  
**Objetivo:** Demostrar dinámicamente el estado vulnerable inicial (**FAIL**) para los 5 casos de prueba de seguridad mediante Análisis Dinámico de Seguridad de Aplicaciones (DAST - Dynamic Application Security Testing).  
**Metodología de referencia:**
- **OWASP Web Security Testing Guide (WSTG v4.2)**
- **OWASP API Security Top 10 (2023)**

---

## 1. Matriz General de Casos vs Resultados DAST

| Caso de Seguridad | Pruebas Dinámicas Evaluadas | OWASP Referencia | Resultado Estado A |
|---|---|---|:---:|
| **SEC-CASE-01: Autenticación y gestión de sesiones** | DAST-01-01 (Fuerza bruta sin bloqueo)<br>DAST-01-02 (Token JWT 30 días) | WSTG-ATHN-03<br>WSTG-SESS-07 | ❌ **FAIL** |
| **SEC-CASE-02: Autorización y control de acceso** | DAST-02-01 (BOLA en listado general)<br>DAST-02-02 (BOLA en detalle por ID)<br>DAST-02-04 (Bypass de rol vía cabecera `X-User-Role`) | API1:2023 (BOLA)<br>API5:2023 (BFLA) | ❌ **FAIL** |
| **SEC-CASE-03: Seguridad de API** | DAST-03-01 (Mass Assignment en registro)<br>DAST-03-02 (Manipulación de precio unitario)<br>DAST-03-03 (Exposición de `passwordHash` en `/me`)<br>DAST-03-05 (Ausencia de rate limiter bajo ráfaga) | API3:2023 (BOPM)<br>API3:2023 (Business Logic)<br>API4:2023 (Unrestricted) | ❌ **FAIL** |
| **SEC-CASE-04: Validación y sanitización de entradas** | DAST-04-01 (Inyección SQL en búsqueda `q=' OR '1'='1`)<br>DAST-04-02 (Cantidades negativas sin 400 Bad Request)<br>DAST-04-04 (Stored DOM XSS vía `dangerouslySetInnerHTML`) | WSTG-INPV-05 (SQLi)<br>WSTG-INPV-01 (Validation)<br>WSTG-CLNT-01 (DOM XSS) | ❌ **FAIL** |
| **SEC-CASE-05: Configuración segura** | DAST-05-01 (CORS wildcard `*`)<br>DAST-05-02 (Falta de CSP, HSTS, X-Frame-Options)<br>DAST-05-03 (Puertos internos expuestos 8086, 8087, 5432)<br>DAST-05-04 (Fuga de `ex.getMessage()` en HTTP 500) | WSTG-CONF-07 (CORS)<br>WSTG-INFO-02 (Ports)<br>WSTG-ERRH-01 (Error handling) | ❌ **FAIL** |

---

## 2. Mapa de Evidencias DAST Generadas

Los reportes detallados y especificaciones dinámicas se encuentran organizados en el árbol de `Evidences/`:

```text
Evidences/
├── SEC-CASE-01/
│   └── DAST/
│       ├── CASE01-EV001-dast-report.md     # Reporte DAST de fuerza bruta y ciclo de vida JWT
│       └── CASE01-EV002-dast-specs.json    # Especificación de peticiones y aserciones
├── SEC-CASE-02/
│   └── DAST/
│       ├── CASE02-EV001-dast-report.md     # Reporte DAST de BOLA/IDOR y escalamiento por cabecera
│       └── CASE02-EV002-dast-specs.json    # Especificación de peticiones y aserciones
├── SEC-CASE-03/
│   └── DAST/
│       ├── CASE03-EV001-dast-report.md     # Reporte DAST de Mass Assignment, precios y fuga de datos
│       └── CASE03-EV002-dast-specs.json    # Especificación de peticiones y aserciones
├── SEC-CASE-04/
│   └── DAST/
│       ├── CASE04-EV001-dast-report.md     # Reporte DAST de SQL Injection, validación y XSS
│       └── CASE04-EV002-dast-specs.json    # Especificación de peticiones y aserciones
└── SEC-CASE-05/
    └── DAST/
        ├── CASE05-EV001-dast-report.md     # Reporte DAST de CORS, puertos expuestos y excepciones
        └── CASE05-EV002-dast-specs.json    # Especificación de peticiones y aserciones
```

---

## 3. Conclusión de la Etapa DAST

1. **Validación Dinámica Completa:** Cada vulnerabilidad modelada en el código fuente (`BASELINE-VULN`) tiene su contraparte dinámica observable en el protocolo HTTP/API.
2. **Resultado de la Línea Base:** Todas las evaluaciones dinámicas del **Estado A (Baseline)** concluyen en **FAIL**, cumpliendo con la exigencia del Secure Development Playbook para justificar la implementación de controles en el **Estado B**.

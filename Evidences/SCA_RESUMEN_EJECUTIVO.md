# Fase VII — Security Testing: Resumen Ejecutivo SCA (Estado A - Baseline)

**Fecha de ejecución:** 15 de Octubre de 2026  
**Objetivo:** Demostrar el estado vulnerable inicial (**FAIL**) para los 5 casos de prueba de seguridad mediante Análisis de Composición de Software (SCA - Software Composition Analysis).  
**Base de datos de vulnerabilidades:** Open Source Vulnerability Database (`OSV.dev` / Google Open Source Security) cruzada con CVE / NVD y GitHub Security Advisories (GHSA).  
**Ecosistemas analizados:**
- **Maven**: Microservicios Spring Boot 3.3.4 (`ms-pedidos360-catalog`, `ms-pedidos360-orders`, `ms-pedidos360-notify`) y BFF `api_cloud`.
- **npm**: Aplicación frontend React + Vite (`frontend`).

---

## 1. Matriz General de Casos vs Resultados SCA

| Caso de Seguridad | Componentes Terceros Clave | CVEs Críticos / Altos | Total Vulnerabilidades | Resultado Estado A |
|---|---|:---:|:---:|:---:|
| **SEC-CASE-01: Autenticación y gestión de sesiones** | Spring Security 6.3.3, Tomcat Embed 10.1.30 | 7 | 37 | ❌ **FAIL** |
| **SEC-CASE-02: Autorización y control de acceso** | Spring Security 6.3.3, Tomcat Embed 10.1.30 | 7 | 37 | ❌ **FAIL** |
| **SEC-CASE-03: Seguridad de API** | Spring WebMVC 6.1.13, Jackson Databind 2.17.2, RabbitMQ 5.21.0 | 6 | 22 | ❌ **FAIL** |
| **SEC-CASE-04: Validación y sanitización de entradas** | PostgreSQL Driver 42.7.4, Tomcat Embed 10.1.30, Vite 5.4.10 | 11 | 47 | ❌ **FAIL** |
| **SEC-CASE-05: Configuración segura** | Spring Boot 3.3.4, RabbitMQ Client 5.21.0, Tomcat Embed 10.1.30, Vite 5.4.10 | 13 | 57 | ❌ **FAIL** |

---

## 2. Mapa de Evidencias SCA Generadas

Los reportes y artefactos JSON de vulnerabilidades se encuentran organizados en el árbol de `Evidences/`:

```text
Evidences/
├── SEC-CASE-01/
│   └── SCA/
│       ├── CASE01-EV001-sca-report.md     # Reporte SCA de autenticación y sesiones
│        └── CASE01-EV002-osv-vulns.json      # Vulnerabilidades crudas OSV (Tomcat, Spring Security)
├── SEC-CASE-02/
│   └── SCA/
│       ├── CASE02-EV001-sca-report.md     # Reporte SCA de elusión de filtros y autorización
│       └── CASE02-EV002-osv-vulns.json      # Vulnerabilidades crudas OSV
├── SEC-CASE-03/
│   └── SCA/
│       ├── CASE03-EV001-sca-report.md     # Reporte SCA de serialización, Jackson y RabbitMQ RPC
│       └── CASE03-EV002-osv-vulns.json      # Vulnerabilidades crudas OSV
├── SEC-CASE-04/
│   └── SCA/
│       ├── CASE04-EV001-sca-report.md     # Reporte SCA de inyecciones, driver JDBC y Vite
│       └── CASE04-EV002-osv-vulns.json      # Vulnerabilidades crudas OSV
└── SEC-CASE-05/
    └── SCA/
        ├── CASE05-EV001-sca-report.md     # Reporte SCA de fugas de credenciales, TLS y dev server
        └── CASE05-EV002-osv-vulns.json      # Vulnerabilidades crudas OSV
```

---

## 3. Conclusión de la Etapa SCA

1. **Estado A Validado en Dependencias:** Se constató que las versiones utilizadas en la línea base poseen vulnerabilidades públicas activas (CVEs) que complementan y agravan las debilidades intencionales de código (`BASELINE-VULN`).
2. **Interacciones Críticas Identificadas:**
   - La fuga de contraseñas de RabbitMQ en `e.getMessage()` (`CVE-2026-106123`) magnifica la respuesta HTTP 500 sin sanitizar de `SEC-REQ-014`.
   - Las fallas de fijación de sesión en Tomcat (`CVE-2025-55668`) amplifican el riesgo del token JWT de 30 días de `SEC-REQ-003`.
   - Las fallas de elusión de restricciones de seguridad en Tomcat (`CVE-2026-43515`) facilitan el bypass del rol administrativo `SEC-REQ-005`.
3. Todas las pruebas de composición de software concluyen en **FAIL**, completando exitosamente los requerimientos de la etapa SCA.

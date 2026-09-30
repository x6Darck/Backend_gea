# Manual Técnico GEA (PDF) — Diseño
**Fecha:** 2026-06-04
**Estado:** Aprobado

## Goal

Producir un manual técnico en PDF, exhaustivo por capa, de los tres sistemas del entorno GEA (backend Spring Boot, frontend React, app Flutter), de modo que un desarrollador nuevo pueda entender y modificar el código correctamente. Incluye diagramas, capturas de pantalla y código anotado.

## Audiencia

Desarrollador con experiencia general que NO conoce este proyecto. Asume conocimiento técnico (Java, JS, Dart) pero no del dominio ni de la arquitectura específica.

## Toolchain de generación

```
Markdown (fuente)  →  HTML + CSS  →  Chrome headless --print-to-pdf  →  manual-tecnico-gea.pdf
```

- **Por qué:** El sistema tiene Node v22, Chrome y Edge. No tiene pandoc ni LaTeX. Chrome headless `--print-to-pdf` es el camino disponible y produce PDF de calidad con control total de estilos vía CSS.
- **Diagramas:** Mermaid renderizado a SVG/PNG e incrustado en el HTML antes de imprimir.
- **Capturas:** El usuario las provee. El manual deja marcadores `[CAPTURA-XX: descripción]` y un anexo lista exactamente qué capturar.
- **Código anotado:** Bloques con resaltado (highlight.js o Prism en el HTML) y notas explicativas.
- **Formato:** Portada, índice (TOC) con numeración, encabezados/pies de página, figuras numeradas.

## Ubicación

Carpeta `docs/manual-tecnico/` en el repo del **backend** (sistema central):

```
docs/manual-tecnico/
├── src/                       # Markdown por parte
│   ├── 00-introduccion.md
│   ├── 01-arranque.md
│   ├── 02-backend.md
│   ├── 03-frontend.md
│   ├── 04-app-flutter.md
│   ├── 05-integracion.md
│   ├── 06-despliegue.md
│   ├── 07-troubleshooting.md
│   └── 08-anexos.md
├── assets/
│   ├── diagrams/              # .mmd (Mermaid) + .svg renderizados
│   └── screenshots/           # capturas que provee el usuario
├── styles/
│   └── manual.css             # estilos del PDF
├── build.js                   # ensambla MD → HTML → PDF
└── manual-tecnico-gea.pdf     # salida final
```

## Estructura del manual (índice)

**Parte 0 — Introducción**
- Propósito · visión general de GEA · diagrama de arquitectura global (3 sistemas + BD + servicios externos) · stack completo · glosario

**Parte 1 — Guía de arranque**
- Requisitos (JDK 21, Node, Flutter SDK, MySQL) · cómo levantar cada sistema en local · tabla completa de variables de entorno

**Parte 2 — Backend (Spring Boot)** — exhaustivo por capa
- Arquitectura en capas (diagrama) · estructura de paquetes
- Configuración · Seguridad (JWT, filtros, CORS, rate limiter, cache)
- Modelo de datos (cada entidad + diagrama ER)
- Repositorios · Servicios (uno por uno) · Controllers (cada endpoint) · DTOs
- Procesos clave con diagramas de secuencia: login, ciclo de evento, ciclo de anuncio, reportes, notificaciones async, auditoría
- Manejo de errores · Migraciones Flyway

**Parte 3 — Frontend Web (React)** — exhaustivo por capa
- Arquitectura · estructura · routing y rutas protegidas · AuthContext
- Capa de servicios · hooks · cada página (con captura) · componentes UI · flujos clave

**Parte 4 — App Móvil (Flutter)** — exhaustivo por capa
- Clean Architecture (diagrama) · estructura core+features · capa de red · Riverpod · GoRouter
- Cada feature con sus capas data/domain/presentation
- Modo offline · i18n · notificaciones push · cada pantalla (con captura)

**Parte 5 — Integración entre sistemas**
- Contratos de API · sincronización DTO↔modelo · autenticación end-to-end

**Parte 6 — Despliegue a producción**
- Backend · frontend · app (APK/PWA) · variables de producción · checklist

**Parte 7 — Troubleshooting**
- Errores comunes y soluciones (incluye los reales ya resueltos: cuelgue en 401, connectivity Android, CORS, secrets)

**Parte 8 — Anexos**
- Lista completa de capturas a insertar · referencia de roles y permisos

## Criterios de aceptación

1. El PDF se genera con un solo comando (`node build.js`)
2. Cubre los tres sistemas con nivel de detalle por capa
3. Incluye diagramas Mermaid renderizados (arquitectura, ER, secuencia)
4. Cada lugar que requiere captura tiene un marcador claro + entrada en el anexo
5. Incluye setup, despliegue y troubleshooting
6. El Markdown fuente es mantenible y editable sin regenerar todo a mano
7. Está en español

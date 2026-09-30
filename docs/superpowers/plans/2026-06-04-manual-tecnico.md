# Manual Técnico GEA (PDF) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Generar un manual técnico en PDF, exhaustivo por capa, de los tres sistemas GEA (backend, frontend, app), con diagramas, marcadores de captura y código anotado, producible con un solo comando.

**Architecture:** Markdown fuente por parte → `marked` (MD→HTML) + `highlight.js` (código) + Mermaid CLI (diagramas→SVG) → CSS de impresión → Chrome headless `--print-to-pdf`.

**Tech Stack:** Node v22 · marked · highlight.js · @mermaid-js/mermaid-cli · Chrome headless

---

## Ubicación
Todo vive en `C:\Users\Administrador\Documents\test\GEA_BACKEND\docs\manual-tecnico\`

## Convenciones de contenido
- **Marcadores de captura:** `> 🖼️ **[CAPTURA-NN]** descripción de qué capturar` — cada uno se registra también en el anexo (Parte 8).
- **Diagramas:** archivo `assets/diagrams/<nombre>.mmd` (Mermaid) + referencia en el MD como `![Figura N: título](../assets/diagrams/<nombre>.svg)`.
- **Código anotado:** bloque ```` ```java ```` etc., seguido de lista de notas explicativas por línea/bloque.
- **Idioma:** español.

## Inventario de referencia (lo que cada parte debe cubrir)

**Backend** (`com.calendario.callapp.callapp_backend`):
- Controllers (12): Archivo, Auditoria, Auth, Dashboard, DispositivoUsuario, LugarFisico, Oficina, Reporte, SolicitudAnuncio, SolicitudEvento, TipoEvento, Usuario
- Services impl (17): AgendaPdf, Archivo, Auditoria, Auth, Dashboard, DispositivoUsuario, LugarFisico, MicrosoftAuth, Notificacion, Oficina, PlantillaCorreo, PushNotification, Reporte, SolicitudAnuncio, SolicitudEvento, TipoEvento, Usuario
- Repositories (14), Entities (21), Security (7), Config (8), DTO request (17), DTO response (17), Mappers (5), GlobalExceptionHandler

**Frontend** (`GEA_FRONT/src`):
- Pages (7): Login, CalendarView, Events, Announcements, PublicAnnouncements, Reports, Users
- Services (10): api + anuncios, archivos, auth, eventos, lugarFisico, oficinas, reportes, tipoEvento, usuarios
- Components UI (~17), layout (DashboardLayout, Sidebar), hooks (2), AuthContext, utils (4)

**App** (`gea_app/lib`):
- core: config, error, models, network (+interceptors), presentation (screens/widgets), providers, services, utils
- features: auth, calendar, announcements (cada uno data/domain/presentation)

---

## Task 1: Infraestructura de build (carpetas, package.json, CSS, build.js)

**Archivos:**
- Crear: `docs/manual-tecnico/package.json`
- Crear: `docs/manual-tecnico/styles/manual.css`
- Crear: `docs/manual-tecnico/build.js`
- Crear: estructura de carpetas `src/`, `assets/diagrams/`, `assets/screenshots/`

- [ ] **Step 1: Crear estructura de carpetas**

```bash
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND\docs"
mkdir manual-tecnico
cd manual-tecnico
mkdir src
mkdir assets
mkdir assets\diagrams
mkdir assets\screenshots
mkdir styles
```

- [ ] **Step 2: Crear `package.json`**

```json
{
  "name": "gea-manual-tecnico",
  "version": "1.0.0",
  "private": true,
  "description": "Generador del manual técnico GEA en PDF",
  "scripts": {
    "diagrams": "node build.js --diagrams-only",
    "build": "node build.js"
  },
  "dependencies": {
    "marked": "^12.0.0",
    "marked-highlight": "^2.1.0",
    "highlight.js": "^11.9.0"
  },
  "devDependencies": {
    "@mermaid-js/mermaid-cli": "^10.9.0"
  }
}
```

- [ ] **Step 3: Crear `styles/manual.css`** (estilos de impresión)

```css
@page {
  size: A4;
  margin: 2cm 1.8cm;
  @bottom-center { content: counter(page); }
}
body {
  font-family: "Segoe UI", Arial, sans-serif;
  font-size: 10.5pt;
  line-height: 1.55;
  color: #1e293b;
}
h1 { font-size: 22pt; color: #CE1126; border-bottom: 3px solid #CE1126; padding-bottom: 8px; page-break-before: always; }
h2 { font-size: 16pt; color: #0f172a; margin-top: 1.6em; border-bottom: 1px solid #e2e8f0; padding-bottom: 4px; }
h3 { font-size: 13pt; color: #334155; margin-top: 1.2em; }
h4 { font-size: 11pt; color: #475569; }
code { font-family: "Consolas", monospace; font-size: 9pt; background: #f1f5f9; padding: 1px 4px; border-radius: 3px; }
pre { background: #0f172a; color: #e2e8f0; padding: 14px 16px; border-radius: 8px; font-size: 8.5pt; overflow-x: auto; line-height: 1.45; }
pre code { background: none; color: inherit; padding: 0; }
table { border-collapse: collapse; width: 100%; font-size: 9pt; margin: 12px 0; }
th { background: #CE1126; color: #fff; padding: 8px 10px; text-align: left; }
td { border: 1px solid #e2e8f0; padding: 6px 10px; }
tr:nth-child(even) td { background: #f8fafc; }
blockquote { border-left: 4px solid #f59e0b; background: #fffbeb; margin: 12px 0; padding: 10px 16px; color: #92400e; }
img { max-width: 100%; }
.cover { page-break-after: always; text-align: center; padding-top: 30vh; }
.cover h1 { font-size: 40pt; border: none; page-break-before: avoid; }
.cover .subtitle { font-size: 16pt; color: #64748b; }
.toc { page-break-after: always; }
.figure-caption { font-size: 8.5pt; color: #64748b; text-align: center; font-style: italic; margin-top: 4px; }
/* highlight.js theme (github-dark embebido por build.js) */
```

- [ ] **Step 4: Crear `build.js`**

```js
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const { Marked } = require('marked');
const { markedHighlight } = require('marked-highlight');
const hljs = require('highlight.js');

// marked v12 quitó la opción `highlight` del core: se usa marked-highlight
const marked = new Marked(
  markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code, lang) {
      const language = hljs.getLanguage(lang) ? lang : 'plaintext';
      return hljs.highlight(code, { language }).value;
    },
  })
);

const ROOT = __dirname;
const SRC = path.join(ROOT, 'src');
const DIAGRAMS = path.join(ROOT, 'assets', 'diagrams');
const OUT_HTML = path.join(ROOT, 'manual.html');
const OUT_PDF = path.join(ROOT, 'manual-tecnico-gea.pdf');

// Orden de las partes
const PARTS = [
  '00-introduccion.md',
  '01-arranque.md',
  '02-backend.md',
  '03-frontend.md',
  '04-app-flutter.md',
  '05-integracion.md',
  '06-despliegue.md',
  '07-troubleshooting.md',
  '08-anexos.md',
];

// 1. Renderizar diagramas Mermaid (.mmd -> .svg) con mmdc
function renderDiagrams() {
  if (!fs.existsSync(DIAGRAMS)) return;
  const mmds = fs.readdirSync(DIAGRAMS).filter(f => f.endsWith('.mmd'));
  for (const mmd of mmds) {
    const input = path.join(DIAGRAMS, mmd);
    const output = path.join(DIAGRAMS, mmd.replace('.mmd', '.svg'));
    console.log(`Renderizando diagrama: ${mmd}`);
    execSync(`npx mmdc -i "${input}" -o "${output}" -b transparent`, { stdio: 'inherit', cwd: ROOT });
  }
}

// 2. Ensamblar HTML
function buildHtml() {
  const css = fs.readFileSync(path.join(ROOT, 'styles', 'manual.css'), 'utf8');
  const hljsCss = fs.readFileSync(
    path.join(ROOT, 'node_modules', 'highlight.js', 'styles', 'github-dark.css'), 'utf8'
  );

  let bodyHtml = '';
  for (const part of PARTS) {
    const file = path.join(SRC, part);
    if (!fs.existsSync(file)) {
      console.warn(`AVISO: falta ${part}`);
      continue;
    }
    const md = fs.readFileSync(file, 'utf8');
    bodyHtml += marked.parse(md);
  }

  const html = `<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<style>${hljsCss}\n${css}</style>
</head>
<body>
${bodyHtml}
</body>
</html>`;

  fs.writeFileSync(OUT_HTML, html, 'utf8');
  console.log(`HTML generado: ${OUT_HTML}`);
}

// 4. Imprimir a PDF con Chrome headless
function printPdf() {
  const chrome = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
  const cmd = `"${chrome}" --headless --disable-gpu --no-pdf-header-footer --print-to-pdf="${OUT_PDF}" "file:///${OUT_HTML.replace(/\\/g, '/')}"`;
  console.log('Generando PDF...');
  execSync(cmd, { stdio: 'inherit' });
  console.log(`PDF generado: ${OUT_PDF}`);
}

// Main
const diagramsOnly = process.argv.includes('--diagrams-only');
renderDiagrams();
if (!diagramsOnly) {
  buildHtml();
  printPdf();
}
```

- [ ] **Step 5: Instalar dependencias y probar el pipeline con un MD mínimo**

Crear un `src/00-introduccion.md` temporal de prueba:
```markdown
# Manual de Prueba
Contenido de prueba.
```

```bash
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND\docs\manual-tecnico"
npm install
node build.js
```

Verificar que se generó `manual-tecnico-gea.pdf`. Si Chrome falla, probar con la ruta de Edge: `C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`.

- [ ] **Step 6: Agregar `.gitignore` para node_modules**

Crear `docs/manual-tecnico/.gitignore`:
```
node_modules/
manual.html
```

- [ ] **Step 7: Commit**

```bash
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND"
git add docs/manual-tecnico/package.json docs/manual-tecnico/build.js docs/manual-tecnico/styles/ docs/manual-tecnico/.gitignore
git commit -m "docs(manual): add PDF build pipeline (marked + mermaid + chrome headless)"
```

---

## Task 2: Parte 0 — Introducción + arquitectura global

**Archivos:**
- Crear: `src/00-introduccion.md`
- Crear: `assets/diagrams/arquitectura-global.mmd`

- [ ] **Step 1: Crear el diagrama de arquitectura global**

`assets/diagrams/arquitectura-global.mmd`:
```
graph TB
  subgraph Clientes
    Web[Panel Web React]
    Movil[App Flutter Android/iOS]
    PWA[PWA Navegador]
  end
  subgraph Backend[Backend Spring Boot :8083]
    API[API REST /api]
    Sec[Capa Seguridad JWT]
    Svc[Servicios]
    Repo[Repositorios JPA]
  end
  DB[(MySQL gea_db)]
  SMTP[SMTP Institucional]
  FCM[Firebase Cloud Messaging]
  Azure[Microsoft Azure AD]
  Web --> API
  Movil --> API
  PWA --> API
  API --> Sec --> Svc --> Repo --> DB
  Svc --> SMTP
  Svc --> FCM
  Sec --> Azure
```

- [ ] **Step 2: Crear `src/00-introduccion.md`**

Estructura (escribir el contenido en prosa técnica bajo cada heading):
```markdown
<div class="cover">
<h1>Manual Técnico</h1>
<p class="subtitle">Sistema GEA — Gestión de Eventos y Anuncios<br>Universidad Libre, Seccional Cúcuta</p>
<p class="subtitle">Versión 1.0 · 2026</p>
</div>

# 0. Introducción

## 0.1 Propósito de este manual
[Para quién es, qué cubre, cómo leerlo]

## 0.2 ¿Qué es GEA?
[Descripción funcional: plataforma de gestión de eventos, anuncios, calendario, con flujo de aprobación multi-rol. Quién lo usa.]

## 0.3 Arquitectura global
[Explicar los 3 sistemas + BD + servicios externos]
![Figura 1: Arquitectura global del sistema GEA](../assets/diagrams/arquitectura-global.svg)
<p class="figure-caption">Figura 1: Arquitectura global del sistema GEA</p>

## 0.4 Stack tecnológico
[Tabla por sistema: tecnología, versión, propósito]

## 0.5 Glosario
[Tabla de términos: SIAPAC, Solicitud, Publicación, Oficina, Pieza Gráfica, Rol, JWT, etc.]
```

Completar cada sección con contenido real basado en el conocimiento del proyecto (los 3 repos, stack del spec, roles SUPER_ADMIN/ADMIN/COMUNICACIONES/OFICINA/USUARIO_APP/USUARIO_AUTENTICADO_APP).

- [ ] **Step 3: Generar diagramas y verificar**

```bash
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND\docs\manual-tecnico"
npm run diagrams
```

Verificar que `assets/diagrams/arquitectura-global.svg` existe.

- [ ] **Step 4: Commit**

```bash
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND"
git add docs/manual-tecnico/src/00-introduccion.md docs/manual-tecnico/assets/diagrams/
git commit -m "docs(manual): part 0 - introduction and global architecture"
```

---

## Task 3: Parte 1 — Guía de arranque

**Archivos:**
- Crear: `src/01-arranque.md`

- [ ] **Step 1: Crear `src/01-arranque.md`**

Headings a cubrir con contenido real:
```markdown
# 1. Guía de arranque

## 1.1 Requisitos previos
[Tabla: JDK 21, Maven, Node 18+, Flutter SDK 3.x, MySQL 8, Git. Versiones exactas del proyecto.]

## 1.2 Estructura de repositorios
[Tabla con los 3 repos GitHub (x6Darck/GEA_BACKEND, GEA_FRONT, GEA_MOVIL) y rutas]

## 1.3 Backend — levantar en local
[Pasos: clonar, crear BD gea_db en MySQL, configurar application.properties / variables, mvn spring-boot:run. Puerto 8083, context-path /api]

## 1.4 Frontend — levantar en local
[Pasos: clonar, npm install, .env.local con VITE_API_URL, npm run dev. Puerto 5173]

## 1.5 App Flutter — levantar en local
[Pasos: flutter pub get, flutter gen-l10n, flutter run con --dart-define=API_BASE_URL]

## 1.6 Variables de entorno (referencia completa)
[Tabla exhaustiva: DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, JWT, CORS_ALLOWED_ORIGINS, SMTP_*, UPLOAD_DIR, FRONTEND_URL, DB_POOL_*, NOTIF_*, MANAGEMENT_PORT, FLYWAY_ENABLED | VITE_API_URL | API_BASE_URL — con default y descripción de cada una]
```

Basar el contenido en `application.properties` real del backend, el `.env` del frontend y `app_config.dart` de la app.

- [ ] **Step 2: Commit**

```bash
git add docs/manual-tecnico/src/01-arranque.md
git commit -m "docs(manual): part 1 - local setup guide and environment variables"
```

---

## Task 4: Parte 2 — Backend (dividido en sub-tareas por capa)

**Archivo:** `src/02-backend.md` (un solo archivo, construido incrementalmente)
**Diagramas:** `assets/diagrams/backend-capas.mmd`, `backend-er.mmd`, `seq-login.mmd`, `seq-ciclo-evento.mmd`, `seq-notificacion.mmd`

### Task 4a: Arquitectura, paquetes, configuración y seguridad

- [ ] **Step 1: Crear diagrama de capas** `assets/diagrams/backend-capas.mmd`:
```
graph LR
  C[Controller] --> S[Service]
  S --> R[Repository]
  R --> E[Entity/JPA]
  E --> DB[(MySQL)]
  S --> M[Mapper]
  M --> DTO[DTO Response]
  C -.recibe.-> REQ[DTO Request]
```

- [ ] **Step 2: Crear diagrama de secuencia de login** `assets/diagrams/seq-login.mmd`:
```
sequenceDiagram
  participant Cli as Cliente
  participant AC as AuthController
  participant AS as AuthServiceImpl
  participant UR as UsuarioRepository
  participant JS as JwtService
  Cli->>AC: POST /auth/login {correo, password}
  AC->>AS: login(request)
  AS->>UR: getByCorreoOptimized(correo)
  UR-->>AS: Usuario
  AS->>AS: passwordEncoder.matches()
  AS->>JS: generarToken(usuario)
  JS-->>AS: JWT
  AS-->>AC: AuthResponse{token, datos}
  AC-->>Cli: 200 OK
```

- [ ] **Step 3: Escribir sección de backend (parte 1) en `src/02-backend.md`**

```markdown
# 2. Backend (Spring Boot)

## 2.1 Arquitectura en capas
[Explicar Controller→Service→Repository→Entity, el flujo DTO request/response, mappers]
![Figura: Capas del backend](../assets/diagrams/backend-capas.svg)

## 2.2 Estructura de paquetes
[Árbol de paquetes com.calendario.callapp.callapp_backend con descripción de cada uno]

## 2.3 Capa de configuración
[Documentar cada clase de config/: AsyncConfig (pool de hilos), CacheConfig (Caffeine), DataInitializer, JpaAuditingConfig, OficinaDataSeeder, PasswordConfig, SecurityProperties, AppSecurityProperties]

## 2.4 Capa de seguridad
[Documentar cada clase de security/:
- SecurityConfig (filtros, CORS, autorización por rol — tabla de rutas→roles)
- JwtService (generación/validación, fail-fast del secret)
- JwtAuthenticationFilter (validación por request + cache)
- CustomUserDetailsService (carga + @Cacheable)
- RateLimiterFilter (límite por IP)
- JwtAuthenticationEntryPoint, JwtAccessDeniedHandler]
[Incluir diagrama seq-login + código anotado de JwtService.generarToken]
```

- [ ] **Step 4: Generar diagramas y commit**

```bash
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND\docs\manual-tecnico"
npm run diagrams
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND"
git add docs/manual-tecnico/src/02-backend.md docs/manual-tecnico/assets/diagrams/
git commit -m "docs(manual): part 2a - backend architecture, config and security"
```

### Task 4b: Modelo de datos (entidades + ER)

- [ ] **Step 1: Crear diagrama ER** `assets/diagrams/backend-er.mmd` (entidades clave y relaciones: Usuario, RolEntity, Oficina, SolicitudEvento, PublicacionEvento, SolicitudAnuncio, PublicacionAnuncio, LugarFisico, TipoEventoCatalogo, ArchivoAdjunto, SolicitudEventoParticipante, DispositivoUsuario, ReporteGenerado, NotificacionEnviada). Usar `erDiagram` de Mermaid.

- [ ] **Step 2: Añadir a `src/02-backend.md`**
```markdown
## 2.5 Modelo de datos
[Diagrama ER + tabla por cada una de las 21 entidades: nombre, tabla, campos clave, relaciones, enums (EstadoSolicitud, Rol, FrecuenciaRecurrencia, TipoParticipante, AuthProvider). Explicar BaseEntity (auditoría) y @Audited (Envers)]
![Figura: Modelo entidad-relación](../assets/diagrams/backend-er.svg)
```

- [ ] **Step 3: Generar y commit**
```bash
npm run diagrams
git add docs/manual-tecnico/src/02-backend.md docs/manual-tecnico/assets/diagrams/backend-er.svg docs/manual-tecnico/assets/diagrams/backend-er.mmd
git commit -m "docs(manual): part 2b - data model and ER diagram"
```

### Task 4c: Repositorios y servicios

- [ ] **Step 1: Añadir a `src/02-backend.md`**
```markdown
## 2.6 Capa de repositorios
[Tabla por cada uno de los 14 repositorios: entidad, queries personalizadas relevantes (ej: getByCorreoOptimized, findProximos con paginación, findConflictsBulk). Explicar el patrón JOIN FETCH para evitar N+1 y la paginación de seguridad agregada]

## 2.7 Capa de servicios
[Sub-sección por cada servicio importante:
- AuthServiceImpl, MicrosoftAuthServiceImpl (autenticación)
- SolicitudEventoServiceImpl (el más complejo: crear, aprobar, rechazar, publicar, recurrencia, conflictos de lugar)
- SolicitudAnuncioServiceImpl
- NotificacionServiceImpl (correos async @Async + pool)
- ReporteServiceImpl, AgendaPdfService (XLSX/PDF)
- ArchivoServiceImpl (storage + interfaz ArchivoStorageService)
- UsuarioServiceImpl (CRUD + invalidación de cache)
- DashboardServiceImpl, OficinaServiceImpl, LugarFisicoServiceImpl, TipoEventoServiceImpl, DispositivoUsuarioServiceImpl, PushNotificationService, PlantillaCorreoServiceImpl, AuditoriaServiceImpl
Cada uno: responsabilidad, métodos públicos, dependencias]
```

- [ ] **Step 2: Commit**
```bash
git add docs/manual-tecnico/src/02-backend.md
git commit -m "docs(manual): part 2c - repositories and services"
```

### Task 4d: Controllers, DTOs y mappers

- [ ] **Step 1: Añadir a `src/02-backend.md`**
```markdown
## 2.8 Capa de controllers (endpoints)
[Tabla por cada uno de los 12 controllers: método HTTP, ruta, rol requerido, DTO request, DTO response, descripción. Es la referencia de API del backend]

## 2.9 DTOs
[Tabla de los 17 request y 17 response DTOs, agrupados por dominio, con sus campos principales]

## 2.10 Mappers
[Los 5 mappers: qué entidad↔DTO convierten]
```

- [ ] **Step 2: Commit**
```bash
git add docs/manual-tecnico/src/02-backend.md
git commit -m "docs(manual): part 2d - controllers, DTOs and mappers"
```

### Task 4e: Procesos clave, errores y migraciones

- [ ] **Step 1: Crear diagramas** `seq-ciclo-evento.mmd` (PENDIENTE→APROBADA→PUBLICADA con actores por rol) y `seq-notificacion.mmd` (flujo async de correo).

- [ ] **Step 2: Añadir a `src/02-backend.md`**
```markdown
## 2.11 Procesos clave
### 2.11.1 Ciclo de vida de un evento
[Diagrama de secuencia + explicación de estados y quién hace cada transición]
![Figura: Ciclo de vida del evento](../assets/diagrams/seq-ciclo-evento.svg)
### 2.11.2 Ciclo de vida de un anuncio
### 2.11.3 Notificaciones asíncronas
![Figura: Flujo de notificación por correo](../assets/diagrams/seq-notificacion.svg)
### 2.11.4 Generación de reportes
### 2.11.5 Auditoría (Hibernate Envers)

## 2.12 Manejo de errores
[GlobalExceptionHandler: qué excepciones captura, formato ApiResponse de error]

## 2.13 Migraciones de base de datos (Flyway)
[Cómo funciona el baseline, cómo crear V2__, V3__]
```

- [ ] **Step 3: Generar y commit**
```bash
npm run diagrams
git add docs/manual-tecnico/src/02-backend.md docs/manual-tecnico/assets/diagrams/
git commit -m "docs(manual): part 2e - key processes, error handling and migrations"
```

---

## Task 5: Parte 3 — Frontend Web (React)

**Archivos:** `src/03-frontend.md`, `assets/diagrams/frontend-flujo.mmd`

- [ ] **Step 1: Crear `src/03-frontend.md`**
```markdown
# 3. Frontend Web (React)

## 3.1 Arquitectura y stack
[React 19, Vite, axios, react-router-dom v7, recharts. Patrón: pages + services + context + hooks]

## 3.2 Estructura de carpetas
[Árbol src/ con descripción]

## 3.3 Routing y rutas protegidas
[App.jsx: estructura de rutas, DashboardLayout, ProtectedRoute (allowedRoles), redirects]

## 3.4 Autenticación (AuthContext)
[Estado de usuario, login/logout, persistencia en localStorage, listener auth-error → redirect, manejo de token]

## 3.5 Capa de servicios
[api.js: instancia axios, interceptores request (token) y response (ApiResponse unwrap, cooldown de notificaciones, 401). Cada servicio de dominio: anuncios, archivos, auth, eventos, lugarFisico, oficinas, reportes, tipoEvento, usuarios — endpoints que consume]

## 3.6 Hooks personalizados
[useEventManagement, useAnnouncementManagement: qué lógica encapsulan]

## 3.7 Páginas
[Por cada página, con marcador de captura:
### 3.7.1 Login  > 🖼️ [CAPTURA-01] Pantalla de login web
### 3.7.2 CalendarView  > 🖼️ [CAPTURA-02] ...
### 3.7.3 Events  ### 3.7.4 Announcements  ### 3.7.5 PublicAnnouncements
### 3.7.6 Reports  ### 3.7.7 Users
Cada una: propósito, datos que muestra, acciones, componentes que usa]

## 3.8 Componentes UI reutilizables
[Modal, Drawer, EventModal (con diálogo SIAPAC), PublishEventModal, GenerateReportModal, ParticipantModal, EventDetailModal, AnnouncementDetailModal/Lightbox, AmPmTimePicker, EmptyState, Spinner, ErrorBoundary, UserDetailDrawer, ExportAgendaModal, ReportDetailModal]

## 3.9 Componentes de layout
[DashboardLayout, Sidebar (navegación por rol)]
```

- [ ] **Step 2: Commit**
```bash
git add docs/manual-tecnico/src/03-frontend.md docs/manual-tecnico/assets/diagrams/
git commit -m "docs(manual): part 3 - React frontend exhaustive by layer"
```

---

## Task 6: Parte 4 — App Móvil (Flutter)

**Archivos:** `src/04-app-flutter.md`, `assets/diagrams/flutter-clean-arch.mmd`

- [ ] **Step 1: Crear diagrama Clean Architecture** `flutter-clean-arch.mmd` (Presentation → Domain ← Data, con Riverpod providers).

- [ ] **Step 2: Crear `src/04-app-flutter.md`**
```markdown
# 4. App Móvil (Flutter)

## 4.1 Clean Architecture
[Capas presentation/domain/data, dirección de dependencias]
![Figura: Clean Architecture de la app](../assets/diagrams/flutter-clean-arch.svg)

## 4.2 Estructura de carpetas
[core/ + features/, descripción]

## 4.3 Capa core
### 4.3.1 Red (dio_client, AuthInterceptor con timeout, network_providers, session_expired_notifier)
### 4.3.2 Configuración (app_config con dart-define, microsoft_auth_config)
### 4.3.3 Servicios (connectivity_service, offline_cache_service, notification_service)
### 4.3.4 Providers (connectivity, notification)
### 4.3.5 Widgets compartidos (gea_button, gea_text_field, gea_empty_state, offline_banner, notification_bell, info_row)
### 4.3.6 Manejo de errores (failures), utils (image_utils)

## 4.4 Gestión de estado (Riverpod)
[Patrón de providers, FutureProvider, StateNotifier, cómo se inyectan repos]

## 4.5 Navegación (GoRouter)
[app_router: rutas, MainScreen con NavigationBar, redirect de sesión]

## 4.6 Feature: Auth
[data/domain/presentation: auth_model, auth_repository_impl, auth_user, auth_providers, login_screen, profile_screen. Login local + Microsoft + invitado]

## 4.7 Feature: Calendar
[event_model, event_repository_impl (con cache offline), pinned events (datasource/usecases), calendar_screen, event_card, countdown_chip, stream_badge, share_card, pin_button, etc.]

## 4.8 Feature: Announcements
[announcement_model, lugar_fisico_model, repos, providers, announcements_screen, request_announcement_screen, announcement_card]

## 4.9 Modo offline
[connectivity_service + offline_cache_service + offline_banner: cómo se cachea y se sirve]

## 4.10 Internacionalización
[flutter gen-l10n, ARB files, AppLocalizations, cómo agregar un idioma]

## 4.11 Notificaciones push
[notification_service, Firebase, registro de dispositivo]

## 4.12 Pantallas (capturas)
[Marcadores: > 🖼️ [CAPTURA-08..14] login, calendario, detalle evento, anuncios, perfil, notificaciones, modo offline]
```

- [ ] **Step 3: Generar y commit**
```bash
npm run diagrams
git add docs/manual-tecnico/src/04-app-flutter.md docs/manual-tecnico/assets/diagrams/
git commit -m "docs(manual): part 4 - Flutter app exhaustive by layer"
```

---

## Task 7: Parte 5 — Integración entre sistemas

**Archivo:** `src/05-integracion.md`

- [ ] **Step 1: Crear `src/05-integracion.md`**
```markdown
# 5. Integración entre sistemas

## 5.1 Contratos de API
[Cómo frontend y app consumen el backend: base URL, formato ApiResponse {success, data, message}, autenticación Bearer]

## 5.2 Sincronización DTO ↔ modelo
[Tabla: DTO backend → modelo frontend → modelo Flutter, para Evento, Anuncio, Usuario. Advertencia de mantenerlos sincronizados]

## 5.3 Autenticación end-to-end
[Flujo completo: login en cliente → JWT → almacenamiento (localStorage web / secure storage app) → envío en cada request → validación en backend → expiración y refresh manual]

## 5.4 Manejo de archivos e imágenes
[Upload multipart, resolución de URLs relativas→absolutas en cada cliente]
```

- [ ] **Step 2: Commit**
```bash
git add docs/manual-tecnico/src/05-integracion.md
git commit -m "docs(manual): part 5 - cross-system integration"
```

---

## Task 8: Parte 6 — Despliegue a producción

**Archivo:** `src/06-despliegue.md`

- [ ] **Step 1: Crear `src/06-despliegue.md`**
```markdown
# 6. Despliegue a producción

## 6.1 Visión general del despliegue objetivo
[Diagrama: dominio + SSL, backend en cloud, frontend en Netlify/Vercel, BD administrada, object storage]

## 6.2 Backend
[Build mvn package, variables de producción obligatorias (JWT_SECRET, DB_*, SPRING_PROFILES_ACTIVE=prod), ddl-auto=validate + Flyway, Docker]

## 6.3 Frontend
[npm run build, .env.production con VITE_API_URL, servir con nginx (config), Netlify/Vercel]

## 6.4 App móvil
[flutter build apk/ipa con --dart-define, PWA con flutter build web, distribución]

## 6.5 Checklist de despliegue
[Lista verificable: secrets configurados, CORS con dominios reales, HTTPS, BD respaldada, health checks, etc.]
```

- [ ] **Step 2: Commit**
```bash
git add docs/manual-tecnico/src/06-despliegue.md
git commit -m "docs(manual): part 6 - production deployment"
```

---

## Task 9: Parte 7 — Troubleshooting

**Archivo:** `src/07-troubleshooting.md`

- [ ] **Step 1: Crear `src/07-troubleshooting.md`**

Documentar los errores reales ya resueltos en este proyecto + comunes:
```markdown
# 7. Solución de problemas comunes

## 7.1 Backend
[- Arranque falla por JWT_SECRET (fail-fast)
- ddl-auto=validate falla por esquema desincronizado
- Pool de conexiones agotado
- Correos no se envían (SMTP)]

## 7.2 Frontend
[- CORS bloqueado (ERR_NGROK/origen no permitido)
- 401 no redirige
- URL de backend incorrecta]

## 7.3 App Flutter
[- App se queda cargando, no conecta al backend (cuelgue en AuthInterceptor sin timeout — caso real)
- connectivity_plus reporta offline (falta ACCESS_NETWORK_STATE — caso real)
- Cleartext HTTP bloqueado en Android
- Build falla por gen-l10n]

## 7.4 Tabla de referencia rápida
[Síntoma → causa probable → solución]
```

- [ ] **Step 2: Commit**
```bash
git add docs/manual-tecnico/src/07-troubleshooting.md
git commit -m "docs(manual): part 7 - troubleshooting guide"
```

---

## Task 10: Parte 8 — Anexos (lista de capturas)

**Archivo:** `src/08-anexos.md`

- [ ] **Step 1: Crear `src/08-anexos.md`**
```markdown
# 8. Anexos

## 8.1 Lista de capturas a insertar
[Tabla con TODOS los marcadores [CAPTURA-NN] del documento: ID, sección, descripción exacta de qué capturar, dónde tomarla (URL/pantalla). El usuario usa esto como checklist]

## 8.2 Referencia de roles y permisos
[Tabla completa: rol → qué puede hacer en cada sistema → rutas backend permitidas]

## 8.3 Referencia de comandos
[Tabla de comandos útiles por sistema: build, test, run, deploy]

## 8.4 Índice de figuras
[Lista de todas las figuras/diagramas]
```

- [ ] **Step 2: Recopilar todos los marcadores de captura del documento**

```bash
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND\docs\manual-tecnico"
grep -rn "CAPTURA-" src/
```

Asegurarse que cada marcador esté listado en el anexo 8.1.

- [ ] **Step 3: Commit**
```bash
git add docs/manual-tecnico/src/08-anexos.md
git commit -m "docs(manual): part 8 - annexes and screenshot checklist"
```

---

## Task 11: Generación final, índice y portada

- [ ] **Step 1: Agregar índice (TOC) manual al inicio de `00-introduccion.md`**

Después de la portada, antes de `# 0. Introducción`, insertar una tabla de contenidos con las partes y secciones principales (con `<div class="toc">`).

- [ ] **Step 2: Generar el PDF completo**

```bash
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND\docs\manual-tecnico"
npm run build
```

Verificar que `manual-tecnico-gea.pdf` se genera con todas las partes, diagramas renderizados y saltos de página correctos.

- [ ] **Step 3: Revisar el PDF**

Abrir el PDF y verificar:
- Portada e índice correctos
- Todos los diagramas visibles
- Código con resaltado
- Saltos de página en cada `# ` (h1)
- Tablas bien formateadas
- Marcadores de captura visibles y claros

Ajustar `manual.css` o el contenido si algo se ve mal, regenerar.

- [ ] **Step 4: Commit final**
```bash
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND"
git add docs/manual-tecnico/src/00-introduccion.md docs/manual-tecnico/manual-tecnico-gea.pdf
git commit -m "docs(manual): add TOC, cover and generate final PDF"
git push origin main
```

---

## Verificación final

1. `node build.js` genera `manual-tecnico-gea.pdf` sin errores
2. El PDF cubre los 3 sistemas exhaustivamente por capa
3. Incluye diagramas (arquitectura global, capas backend, ER, secuencias, clean architecture)
4. Cada captura tiene marcador + entrada en anexo 8.1
5. Incluye setup (parte 1), despliegue (parte 6) y troubleshooting (parte 7)
6. Está en español y es navegable con índice

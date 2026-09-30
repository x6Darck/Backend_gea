# Arquitectura del Backend GEA

> Documento de referencia técnica para comprender cómo están organizadas las capas del sistema y cómo fluye la información entre ellas.

---

## 1. Visión general — capas de la aplicación

```
┌────────────────────────────────────────────────────────────────┐
│  Cliente (app Flutter / frontend React)                        │
└────────────────────────┬───────────────────────────────────────┘
                         │ HTTP / JSON
┌────────────────────────▼───────────────────────────────────────┐
│  Filtros de seguridad (Spring Security)                        │
│  RateLimiterFilter  →  JwtAuthenticationFilter                 │
│  (orden en SecurityConfig; ambos van antes del filtro estándar │
│   UsernamePasswordAuthenticationFilter de Spring)              │
└────────────────────────┬───────────────────────────────────────┘
                         │ SecurityContextHolder poblado
┌────────────────────────▼───────────────────────────────────────┐
│  Controller  (@RestController)                                 │
│  Valida entrada con Bean Validation (@Valid)                   │
│  Comprueba roles con @PreAuthorize                             │
│  Devuelve ApiResponse<T>                                       │
└────────────────────────┬───────────────────────────────────────┘
                         │ llama a servicio
┌────────────────────────▼───────────────────────────────────────┐
│  Service impl  (@Service)                                      │
│  Reglas de negocio, validaciones, @Transactional               │
│  Coordina repositorios, lanza notificaciones, registra         │
│  auditoría                                                     │
└──────────┬─────────────────────────┬───────────────────────────┘
           │ consulta / persiste     │ convierte a DTO
┌──────────▼──────────┐   ┌──────────▼──────────────────────────┐
│  Repository          │   │  Mapper  (MapStruct)                │
│  (JPA / Spring Data) │   │  Entity  ↔  DTO request / response │
└──────────┬──────────┘   └─────────────────────────────────────┘
           │ JPQL / SQL
┌──────────▼──────────┐
│  Base de datos       │
│  (MySQL / MariaDB)   │
└─────────────────────┘
```

El flujo canónico de respuesta:
```
Entity  →  Mapper  →  DTO response  →  ApiResponse<DTO>  →  JSON al cliente
```

`ApiResponse<T>` es el sobre estándar de todas las respuestas: campos `success`, `message`, `data` y `timestamp`.

---

## 2. Flujo de autenticación

### 2.1 Login local (usuario + contraseña)

```
POST /auth/login
  ↓
AuthController.login()
  ↓
AuthServiceImpl.login(AuthRequest)
  ├─ UsuarioRepository.getByCorreoOptimized()  (busca usuario)
  ├─ PasswordEncoder.matches()                 (verifica contraseña bcrypt)
  ├─ JwtService.generarToken(usuario)          (emite JWT con JTI único)
  ├─ HistorialLoginService.registrarExito() /
  │  .registrarFallo()                         (ver §7.3 — nunca bloquea el login)
  └─ retorna AuthResponse { id, token, nombre, correo, rol, idOficina,
                             oficinaNombre, fotoUrl, tipo }
  ↓
AuthController (devuelve token en body + cookie HttpOnly gea_auth)
```

La cookie `gea_auth` es usada por el frontend React. El token en el body es usado por la app Flutter (Bearer header). El `id` en `AuthResponse` es necesario en el frontend para comparaciones de "dueño de la solicitud" (ej. permitir editar una solicitud propia en `EN_REVISION`).

### 2.2 Login Microsoft (OAuth2 PKCE — app móvil)

```
POST /auth/microsoft/mobile
  ↓
AuthController.microsoftMobile()
  ↓
MicrosoftAuthServiceImpl.autenticar(MicrosoftAuthRequest)
  ├─ Valida el idToken (emisor, audiencia, claims requeridos)
  ├─ Busca / crea Usuario local por correo Microsoft (auto-provisión)
  ├─ JwtService.generarToken(usuario)
  ├─ HistorialLoginService.registrarExito() / .registrarFallo()  (ver §7.3)
  └─ retorna AuthResponse { id, token, nombre, correo, rol, idOficina,
                             oficinaNombre, fotoUrl, tipo }
```

Existe también `POST /auth/microsoft/mobile/code` (`autenticarConCodigo()`), para apps registradas en Azure como cliente confidencial: recibe el código de autorización en vez del idToken directo y lo intercambia en el backend (con el client_secret, que nunca sale del servidor).

### 2.3 Logout

```
POST /auth/logout
  ↓
AuthController.logout()
  ├─ Extrae token de Authorization header (app móvil) O cookie (frontend)
  ├─ JwtService.extractJti(token)              (obtiene identificador único del JWT)
  ├─ TokenBlacklistService.blacklist(jti, exp) (agrega a caché en memoria hasta expiración)
  └─ Borra cookie en cliente (maxAge=0)
```

Desde ese momento, `JwtAuthenticationFilter` rechaza el token aunque no haya expirado, porque el JTI está en la lista negra.

### 2.4 Validación por petición (JwtAuthenticationFilter)

```
Cada petición autenticada:
  RateLimiterFilter         (limita peticiones por IP, antes del JWT)
  ↓
  JwtAuthenticationFilter
    ├─ Extrae Bearer header O cookie gea_auth
    ├─ JwtService.extractJti() → TokenBlacklistService.isBlacklisted()
    ├─ JwtService.validateToken()                (firma + expiración)
    ├─ CustomUserDetailsService.loadUserByUsername()
    └─ Puebla SecurityContextHolder
```

### 2.5 Roles del sistema

| Rol | Qué puede hacer |
|---|---|
| `SUPER_ADMIN` / `ADMIN` | Todo: gestión de usuarios, oficinas, revisión/aprobación global |
| `COMUNICACIONES` | Revisar, aprobar/rechazar/publicar solicitudes de evento y anuncio |
| `OFICINA` | Crear y editar sus propias solicitudes de evento |
| `USUARIO_AUTENTICADO_APP` | Crear solicitudes de evento desde la app móvil |
| `CONSULTORIA` | Lectura global de solicitudes (sin modificar) |
| `USUARIO_APP` | Ver publicaciones y hacer solicitudes de anuncio |

Los roles se controlan con `@PreAuthorize` en cada endpoint y con reglas globales en `SecurityConfig.securityFilterChain()`.

---

## 3. Flujo de ciclo de vida de una Solicitud de Evento

Este es el flujo central del negocio.

### 3.1 Diagrama de estados

```
               crear
                 │
                 ▼
           PENDIENTE
                 │
       ┌─────────┴──────────┐
       │ rechazar           │ devolver
       ▼                    ▼
   RECHAZADA            EN_REVISION
                            │
                            │ (usuario edita y reenvía → vuelve a PENDIENTE)
                            │
                        PENDIENTE
                            │
                            │ aprobar
                            ▼
                         APROBADA
                            │
                            │ publicar
                            ▼
                         PUBLICADA
```

### 3.2 Operaciones por actor

**Actor: Oficina / app móvil**
```
POST /oficina/solicitudes-evento              → SolicitudEventoServiceImpl.crear()
PUT  /oficina/solicitudes-evento/{id}         → .actualizarPropia()
PUT  /oficina/solicitudes-evento/serie/{grp}  → .actualizarSerie()
DELETE /oficina/solicitudes-evento/{id}       → .eliminarPropia()
```

**Actor: Comunicaciones / Admin**
```
GET  /comunicaciones/solicitudes-evento          → .listarParaRevision()
POST /comunicaciones/solicitudes-evento/{id}/aprobar   → .aprobar()
POST /comunicaciones/solicitudes-evento/{id}/rechazar  → .rechazar()
POST /comunicaciones/solicitudes-evento/{id}/devolver  → .devolver()
POST /comunicaciones/solicitudes-evento/{id}/publicar  → .publicar()
```

**Acciones masivas (series recurrentes):** Los eventos recurrentes comparten un `idGrupo` (UUID string). Las operaciones de serie aplican la acción a todos los miembros del grupo:
```
POST /comunicaciones/solicitudes-evento/serie/{grp}/aprobar
POST /comunicaciones/solicitudes-evento/serie/{grp}/publicar
DELETE /comunicaciones/solicitudes-evento/serie/{grp}
```

**Publicaciones visibles al público (sin autenticación):**
```
GET /app/eventos/publicados
GET /app/eventos/publicados/{id}
GET /app/eventos/proximos
GET /app/eventos/agenda/export/pdf   → AgendaPdfService.exportarAgendaPdf()
```

### 3.3 Notificaciones disparadas

| Evento | Notificación enviada por NotificacionServiceImpl |
|---|---|
| crear solicitud | Correo a destinatarios universitarios |
| aprobar solicitud | Correo al solicitante + push al creador |
| rechazar solicitud | Correo al solicitante con motivo |
| publicar solicitud | Push masivo a dispositivos registrados |

---

## 4. Flujo de Solicitud de Anuncio

Análogo al de eventos, con las siguientes diferencias:

- Los anuncios no tienen series recurrentes.
- Pueden adjuntar archivos (`ArchivoAdjunto`) mediante `ArchivoController`.
- El flujo de revisión usa `/comunicaciones/solicitudes-anuncio/` en lugar de `/comunicaciones/solicitudes-evento/`.
- Los anuncios publicados son visibles en `/app/anuncios/publicados` (sin autenticación).

```
POST /app/solicitudes-anuncio                → SolicitudAnuncioServiceImpl.crear()
POST /comunicaciones/solicitudes-anuncio/{id}/aprobar  → .aprobar()
POST /comunicaciones/solicitudes-anuncio/{id}/publicar → .publicar()
```

---

## 5. Flujo de notificaciones

```
SolicitudEventoServiceImpl / SolicitudAnuncioServiceImpl
  │
  │ llama tras cambio de estado
  ▼
NotificacionServiceImpl  (@Async "notificacionesExecutor")
  ├─ PlantillaCorreoServiceImpl.cargarPlantilla()   (HTML desde disco)
  │   └─ rellena variables (titulo, solicitante, url, etc.)
  ├─ JavaMailSender.send()                          (SMTP configurado en application.yml)
  ├─ NotificacionEnviadaRepository.save()           (persiste registro del envío)
  │
  └─ PushNotificationService (FCM)
      ├─ DispositivoUsuarioRepository.findAllTokens()  (tokens FCM registrados)
      └─ FCM HTTP API: envía mensaje push a cada token
```

Las notificaciones se ejecutan de forma **asíncrona** (hilo del pool `notificacionesExecutor` definido en `AsyncConfig`) para no bloquear la petición HTTP principal.

---

## 6. Flujo de reportes y dashboard

```
GET /reportes/**  →  ReporteController  →  ReporteServiceImpl
  ├─ Consulta agregada sobre SolicitudEvento, SolicitudAnuncio, etc.
  ├─ Genera PDF / Excel con iText / Apache POI (según tipo de reporte)
  ├─ ReporteGeneradoRepository.save()   (persiste metadatos del reporte)
  └─ Devuelve bytes del fichero o DTO

GET /dashboard   →  DashboardController  →  DashboardServiceImpl
  └─ Devuelve DashboardResumenResponse con conteos por estado, por oficina, etc.
```

Los reportes se filtran por rango de fechas, estado y oficina. Los archivos generados se pueden recuperar posteriormente mediante `ReporteGeneradoRepository`.

---

## 7. Auditoría y archivos adjuntos

### 7.1 Auditoría (Hibernate Envers)

```
Cualquier operación @Transactional que modifica
SolicitudEvento / SolicitudAnuncio / Usuario / etc.
  │
  └─ Hibernate Envers intercepta automáticamente
      ├─ AuditRevisionListener.newRevision()   (captura el usuario actual)
      ├─ AuditRevisionEntity                   (guarda id, timestamp, usuario)
      └─ tablas _AUD en BD (copia histórica de cada cambio)

GET /auditoria/**  →  AuditoriaController  →  AuditoriaServiceImpl
  └─ Consulta historial de cambios mediante AuditReader de Envers
```

### 7.2 Archivos adjuntos

```
POST /comunicaciones/archivos/upload  →  ArchivoController  →  ArchivoServiceImpl
  ├─ ArchivoStorageService.store()    (guarda en directorio uploads/ del servidor)
  ├─ ArchivoAdjuntoRepository.save()  (persiste metadatos: nombre, ruta, tamaño)
  └─ Retorna ArchivoResponse { id, url, nombre }

GET /archivos/public/{filename}       →  ArchivoController (público, sin auth)
  └─ Sirve el archivo directamente desde uploads/
```

### 7.3 Historial de inicios de sesión (panel de seguridad)

Mecanismo **separado** de la auditoría Envers de §7.1 — no audita cambios de
entidades, sino intentos de login (éxito y fallo, local y Microsoft), para
un panel de seguridad restringido a `SUPER_ADMIN`:

```
AuthServiceImpl.login() / MicrosoftAuthServiceImpl.autenticar()
  │  (cada rama de éxito o fallo, antes del throw/return)
  ▼
HistorialLoginService.registrarExito() / .registrarFallo()
  ├─ ClientIpResolver.resolve()      (IP real del cliente, resistente a
  │                                   spoofing de X-Forwarded-For)
  ├─ construye HistorialLogin (correo, id_usuario si existe, éxito/fallo,
  │  método LOCAL/MICROSOFT, MotivoFalloLogin, IP, user-agent, fecha)
  └─ HistorialLoginPersister.guardarEnNuevaTransaccion()   (REQUIRES_NEW)

GET /admin/seguridad/historial-login  →  SeguridadController
  (solo SUPER_ADMIN — más restringido que /auditoria/**)
  └─ HistorialLoginService.buscar()   (filtros: correo, ip, éxito, rango de
                                        fechas; paginado)

HistorialLoginService.purgarAntiguos()   (@Scheduled diario)
  └─ borra registros con más de 90 días de antigüedad
```

**Por qué el registro va en una transacción `REQUIRES_NEW` separada
(`HistorialLoginPersister`):** `login()` es `@Transactional(readOnly = true)`
y lanza una excepción en los casos fallidos — registrar en la misma
transacción no persistiría nada. El registro nunca puede tumbar un login: si
falla (por la razón que sea), solo se deja un `log.warn` y el login continúa
normalmente.

---

## 8. Mapa de endpoints

> Tabla de referencia rápida: ruta → método HTTP → roles mínimos → servicio destino.

### Autenticación (`/auth/**` — público)

| Método | Ruta | Roles | Servicio |
|---|---|---|---|
| POST | `/auth/login` | — (público) | `AuthServiceImpl.login()` |
| POST | `/auth/microsoft/mobile` | — (público) | `MicrosoftAuthServiceImpl.autenticar()` |
| POST | `/auth/logout` | autenticado | `TokenBlacklistService.blacklist()` |

### Oficina — gestión de solicitudes propias

| Método | Ruta | Roles | Servicio |
|---|---|---|---|
| POST | `/oficina/solicitudes-evento` | OFICINA, ADMIN, COMUNICACIONES, USUARIO_AUTENTICADO_APP | `SolicitudEventoServiceImpl.crear()` |
| GET | `/oficina/solicitudes-evento` | ídem | `.listarPropias()` |
| GET | `/oficina/solicitudes-evento/{id}` | ídem | `.obtenerPropia()` |
| PUT | `/oficina/solicitudes-evento/{id}` | ídem | `.actualizarPropia()` |
| PUT | `/oficina/solicitudes-evento/serie/{grp}` | ídem | `.actualizarSerie()` |
| DELETE | `/oficina/solicitudes-evento/{id}` | ídem | `.eliminarPropia()` |
| DELETE | `/oficina/solicitudes-evento/serie/{grp}` | ídem | `.eliminarSerie()` |

### Comunicaciones — revisión y publicación de eventos

| Método | Ruta | Roles | Servicio |
|---|---|---|---|
| GET | `/comunicaciones/solicitudes-evento` | COMUNICACIONES, ADMIN, CONSULTORIA | `.listarParaRevision()` |
| GET | `/comunicaciones/solicitudes-evento/{id}` | ídem | `.obtenerParaRevision()` |
| POST | `/comunicaciones/solicitudes-evento/{id}/aprobar` | COMUNICACIONES, ADMIN | `.aprobar()` |
| POST | `/comunicaciones/solicitudes-evento/{id}/rechazar` | COMUNICACIONES, ADMIN | `.rechazar()` |
| POST | `/comunicaciones/solicitudes-evento/{id}/devolver` | COMUNICACIONES, ADMIN | `.devolver()` |
| POST | `/comunicaciones/solicitudes-evento/{id}/publicar` | COMUNICACIONES, ADMIN | `.publicar()` |
| POST | `/comunicaciones/solicitudes-evento/serie/{grp}/aprobar` | COMUNICACIONES, ADMIN | `.aprobarSerie()` |
| POST | `/comunicaciones/solicitudes-evento/serie/{grp}/publicar` | COMUNICACIONES, ADMIN | `.publicarSerie()` |
| DELETE | `/comunicaciones/solicitudes-evento/serie/{grp}` | COMUNICACIONES, ADMIN | `.eliminarSerie()` |
| PATCH | `/comunicaciones/eventos-publicados/{id}/visibilidad` | COMUNICACIONES, ADMIN | `.toggleVisibilidad()` |
| PUT | `/comunicaciones/eventos-publicados/{id}` | COMUNICACIONES, ADMIN | `.updatePublicacion()` |
| DELETE | `/comunicaciones/eventos-publicados/{id}` | COMUNICACIONES, ADMIN | `.eliminarPublicacion()` |

### Anuncios — oficina (app)

| Método | Ruta | Roles | Servicio |
|---|---|---|---|
| POST | `/app/solicitudes-anuncio` | USUARIO_APP, USUARIO_AUTENTICADO_APP, ADMIN... | `SolicitudAnuncioServiceImpl.crear()` |
| GET | `/app/solicitudes-anuncio` | ídem | `.listarPropias()` |
| GET | `/app/solicitudes-anuncio/{id}` | ídem | `.obtenerPropia()` |
| PUT | `/app/solicitudes-anuncio/{id}` | ídem | `.actualizar()` |
| DELETE | `/app/solicitudes-anuncio/{id}` | ídem | `.eliminar()` |

### Anuncios — revisión (comunicaciones)

| Método | Ruta | Roles | Servicio |
|---|---|---|---|
| GET | `/comunicaciones/solicitudes-anuncio` | COMUNICACIONES, ADMIN, CONSULTORIA | `.listarParaRevision()` |
| POST | `/comunicaciones/solicitudes-anuncio/{id}/aprobar` | COMUNICACIONES, ADMIN | `.aprobar()` |
| POST | `/comunicaciones/solicitudes-anuncio/{id}/rechazar` | COMUNICACIONES, ADMIN | `.rechazar()` |
| POST | `/comunicaciones/solicitudes-anuncio/{id}/publicar` | COMUNICACIONES, ADMIN | `.publicar()` |

### Publicaciones públicas (sin autenticación)

| Método | Ruta | Servicio |
|---|---|---|
| GET | `/app/eventos/publicados` | `SolicitudEventoServiceImpl.listarPublicadas()` |
| GET | `/app/eventos/publicados/{id}` | `.obtenerPublicacion()` |
| GET | `/app/eventos/proximos` | `.listarProximos()` |
| GET | `/app/eventos/agenda/export/pdf` | `AgendaPdfService.exportarAgendaPdf()` |
| GET | `/app/anuncios/publicados` | `SolicitudAnuncioServiceImpl.listarPublicados()` |
| GET | `/app/anuncios/publicados/{id}` | `.obtenerPublicado()` |

### Administración (requiere ADMIN / SUPER_ADMIN)

| Método | Ruta | Servicio |
|---|---|---|
| GET | `/admin/usuarios` | `UsuarioServiceImpl.listar()` |
| POST | `/admin/usuarios` | `.crear()` |
| PUT | `/admin/usuarios/{id}` | `.actualizar()` |
| DELETE | `/admin/usuarios/{id}` | `.eliminar()` |
| GET | `/admin/oficinas` | `OficinaServiceImpl.listar()` |
| POST | `/admin/oficinas` | `.crear()` |
| PUT | `/admin/oficinas/{id}` | `.actualizar()` |
| DELETE | `/admin/oficinas/{id}` | `.eliminar()` |
| GET | `/admin/seguridad/historial-login` | `SeguridadController` — solo `SUPER_ADMIN` (ver §7.3) |

### Reportes, dashboard, auditoría, archivos, catálogos

| Método | Ruta | Roles | Servicio |
|---|---|---|---|
| GET/POST | `/reportes/**` | autenticado | `ReporteServiceImpl` |
| GET | `/dashboard` | ADMIN, COMUNICACIONES, SUPER_ADMIN | `DashboardServiceImpl` |
| GET | `/auditoria/**` | ADMIN, SUPER_ADMIN | `AuditoriaServiceImpl` |
| POST | `/comunicaciones/archivos/upload` | COMUNICACIONES, ADMIN, OFICINA | `ArchivoServiceImpl` |
| GET | `/archivos/public/{filename}` | — (público) | `ArchivoServiceImpl` |
| GET | `/admin/tipos-evento` | ADMIN, COMUNICACIONES | `TipoEventoServiceImpl` |
| GET | `/lugares-fisicos` | autenticado | `LugarFisicoServiceImpl` |
| GET/POST | `/usuario/**` | autenticado | `UsuarioServiceImpl` |
| POST | `/usuario/dispositivo/token` | autenticado | `DispositivoUsuarioServiceImpl` |

---

## 9. Configuración del entorno

Los parámetros operativos se configuran mediante variables de entorno o `application.yml`:

| Variable | Propósito |
|---|---|
| `JWT_SECRET` | Clave HMAC para firmar tokens (mínimo 32 chars) |
| `jwt.expiration` | TTL del token en milisegundos |
| `app.security.cors.allowed-origins` | Orígenes CORS permitidos (separados por coma) |
| `app.security.cookie.secure` | `true` en producción (HTTPS) |
| `app.notifications.email-enabled` | Activa/desactiva envío de correos |
| `app.notifications.university-recipients` | Destinatarios de correos de nueva solicitud |
| `app.notifications.published-recipients` | Destinatarios de correos de publicación |
| `spring.mail.*` | Configuración SMTP |
| `FIREBASE_CREDENTIALS_JSON` | JSON de credenciales Firebase para push (FCM) |

---

*Generado: 2026-06-30 — Ver código fuente en `src/main/java/com/calendario/callapp/callapp_backend/`*

# Historial de Login (Panel de Seguridad SuperAdmin) — Diseño

**Fecha:** 2026-07-10
**Estado:** Aprobado (modelo de datos), pendiente revisión del spec completo

## Objetivo

Dar a los SuperAdmin un panel donde consultar el historial de inicios de sesión
al sistema —quién entró, desde qué IP, cuándo, con qué método, y qué intentos
fallaron— como herramienta básica de visibilidad de seguridad.

## Alcance (decisiones tomadas en brainstorming)

- **Registrar** inicios de sesión **exitosos y fallidos** (el fallido es la señal
  de seguridad más útil: detecta fuerza bruta / accesos no autorizados).
- **Ambos métodos** de login: local (correo/contraseña) y Microsoft OAuth
  institucional, en un **único historial unificado** marcando el método.
- **Retención 90 días**: purga automática de registros más antiguos.
- **Panel con filtros** (correo, IP, éxito/fallido, rango de fechas), reutilizando
  el patrón de barra de filtros que ya existe en la página de Reportes.
- **Solo SuperAdmin** puede ver el panel (backend y frontend).

### Fuera de alcance (YAGNI, por ahora)

Geolocalización de IP, bloqueo/baneo de IPs, gestión de sesiones activas
revocables, alertas automáticas. Si más adelante hacen falta, cada una es su
propio spec.

## Arquitectura

Tabla nueva `historial_login` (solo-inserción, nunca se actualiza) que se llena
en cada intento de login desde la capa de servicio. La lectura la expone un
endpoint admin-only con paginación y filtros; el frontend es una página React
nueva reutilizando el patrón de Reportes.

Se descartó reutilizar Hibernate Envers (`/admin/auditoria`): audita *cambios de
entidades persistidas*, no eventos de autenticación (un login fallido no
modifica ninguna entidad), así que no aplica.

### Modelo de datos — `historial_login`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT PK auto | |
| `correo_intentado` | VARCHAR(160) | el correo tal como se escribió en el login |
| `id_usuario` | BIGINT FK NULL | null si el correo no corresponde a un usuario real |
| `exito` | BOOLEAN NOT NULL | |
| `metodo` | VARCHAR(30) NOT NULL | reutiliza el enum `AuthProvider` (LOCAL / MICROSOFT) |
| `motivo_fallo` | VARCHAR(40) NULL | enum `MotivoFalloLogin`; null en éxitos |
| `ip` | VARCHAR(45) NULL | IPv4/IPv6; null si no se puede resolver |
| `user_agent` | VARCHAR(400) NULL | navegador/cliente |
| `fecha` | TIMESTAMP NOT NULL | instante del intento |

**Índices:** `fecha` (para la purga por retención y el orden del panel),
`id_usuario`, `ip` (para los filtros).

La entidad **no** extiende `BaseEntity` ni se anota `@Audited`: ya *es* un log de
auditoría, no necesita ser auditado por Envers; y al no actualizarse nunca no
requiere los campos de `fecha_actualizacion`/`usuario_actualizacion`.

**`MotivoFalloLogin`** (enum): `CORREO_NO_REGISTRADO`, `CUENTA_INACTIVA`,
`CREDENCIALES_INVALIDAS`, `TOKEN_INVALIDO`, `OTRO`. Nota: al usuario final el
login sigue devolviendo el mismo mensaje genérico (defensa anti-enumeración ya
existente); el motivo real solo se guarda en este log admin-only, donde sí es
información útil y legítima.

### Resolución de IP (punto crítico de seguridad)

La IP real ya se resuelve de forma endurecida contra spoofing en
`RateLimiterFilter` (toma el **último** salto de `X-Forwarded-For`, solo cuando
`app.security.rate-limit.trust-proxy=true`; si no, `getRemoteAddr()`). Esa lógica
se **extrae** a un componente compartido `ClientIpResolver` reutilizado por
ambos (DRY: una sola implementación auditada, no dos que puedan divergir). El
`RateLimiterFilterTest` existente es la red de seguridad del refactor.

### Persistencia de fallos — transacción independiente (punto crítico)

`AuthServiceImpl.login()` es `@Transactional(readOnly = true)` y **lanza una
excepción** en los fallos. Registrar el fallo dentro de esa transacción no
serviría: es de solo-lectura y además haría rollback al propagarse la excepción.
Por eso `HistorialLoginService.registrar…()` usa
`@Transactional(propagation = REQUIRES_NEW)`: abre su propia transacción
read-write que **commitea de forma independiente** del método que lanzó la
excepción. Es el patrón estándar para registros de auditoría que deben
sobrevivir a un rollback del negocio.

La IP y el User-Agent se resuelven dentro de `HistorialLoginService` desde
`RequestContextHolder` (el request actual del hilo), de forma **null-safe**: si
no hay request (por ejemplo, un test que llama al servicio directamente) se
guarda `null`, sin romper el login.

### API

`GET /admin/seguridad/historial-login` — `@PreAuthorize("hasRole('SUPER_ADMIN')")`
- Query params (todos opcionales): `correo`, `ip`, `exito` (bool), `desde`,
  `hasta` (fecha), `page`, `size`.
- Devuelve `ApiResponse<Page<HistorialLoginResponse>>` (paginado en servidor,
  ordenado por `fecha` descendente — el log crece sin cota dentro de los 90 días).

### Retención

`@Scheduled` diario que borra registros con `fecha` anterior a `now - 90 días`.
El scheduling ya está habilitado en la app (`TokenBlacklistService` ya usa
`@Scheduled`), así que no hace falta tocar configuración global.

### Frontend

- `seguridad.service.js` — cliente del endpoint.
- `SecurityPanel.jsx` — página nueva; barra de filtros calcada de `Reports.jsx`
  (búsqueda + rango de fechas + selector) y tabla con badges de éxito/fallo,
  método e IP; paginación prev/next contra el `Page` del backend.
- Ruta `/seguridad` protegida `allowedRoles={['SUPER_ADMIN']}`.
- Ítem de navegación en `Sidebar.jsx` visible solo para SUPER_ADMIN.

## Testing

- `ClientIpResolverTest`: XFF de confianza toma el último salto; sin trust-proxy
  usa remoteAddr; XFF con basura no rompe.
- `RateLimiterFilterTest` existente sigue verde tras el refactor (regresión).
- `HistorialLoginServiceTest`: registrar éxito y fallo persiste la fila correcta;
  sobrevive a un rollback del llamador (REQUIRES_NEW).
- `AuthHistorialLoginTest`: `authService.login` exitoso deja fila `exito=true`;
  cada rama de fallo deja fila `exito=false` con su `motivo_fallo`.
- `HistorialLoginRetentionTest`: la purga borra >90 días y conserva lo reciente.
- Suite completa (`./mvnw test`) verde: sin regresiones.
- Frontend: lint limpio de la página nueva.

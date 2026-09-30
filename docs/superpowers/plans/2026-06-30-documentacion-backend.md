# Documentación del Backend GEA (Javadoc + Arquitectura) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Dejar el backend completamente documentado de forma que cualquier persona (nueva en el equipo, auditor o desarrollador) entienda **qué hace cada método** y **cómo se conecta el sistema entre sí**, mediante: (1) Javadoc en español sobre cada clase y método público, (2) `package-info.java` en cada paquete, y (3) un documento `ARQUITECTURA.md` que describe capas y flujos de extremo a extremo.

**Alcance:** `src/main/java/com/calendario/callapp/callapp_backend` — ~137 archivos, ~9.600 LOC, 12 capas. **No** se modifica lógica: solo se añaden comentarios y un documento. Los tests existentes deben seguir compilando y pasando sin cambios.

**Tech Stack:** Java 17 · Spring Boot · Maven · Javadoc (HTML doclet)

---

## Convención de documentación (estándar único a aplicar)

Para mantener coherencia con el estilo ya presente en los `package-info.java` actuales:

- **Idioma:** español.
- **Tildes:** se permiten (archivos en UTF-8). Mantener consistencia; si un archivo ya evita tildes, no mezclar dentro del mismo archivo.
- **Formato Javadoc de clase** (obligatorio en toda clase pública):
  ```java
  /**
   * <Resumen en una frase de la responsabilidad de la clase.>
   *
   * <p>Detalle: qué problema resuelve, con qué colabora y cuándo se usa.
   * Enlazar colaboradores con {@link OtraClase}.</p>
   */
  ```
- **Formato Javadoc de método** (obligatorio en todo método público / endpoint):
  ```java
  /**
   * <Qué hace el método, en imperativo: "Crea...", "Devuelve...", "Valida...".>
   *
   * @param x  <significado y restricciones del parámetro>
   * @return   <qué representa el valor devuelto>
   * @throws   <Excepcion>  <condición que la dispara>
   */
  ```
- **Endpoints (controllers):** además del Javadoc, indicar en la descripción el **verbo + ruta**, los **roles** que lo pueden invocar (de `@PreAuthorize`) y el **flujo** al que pertenece (p. ej. "paso 3 del ciclo solicitud→publicación").
- **Comentarios de bloque inline (`//`):** solo en algoritmos no obvios (recurrencia de fechas, generación de PDF, cálculo de reportes). Explicar el *porqué*, no el *qué*.
- **Prohibido:** comentarios que repiten el nombre del método sin aportar (`// getter de nombre`), `TODO`/`FIXME` sin contexto, comentar getters/setters/Lombok triviales.
- **Verificación por archivo:** tras documentar, el archivo debe seguir compilando (`./mvnw -q -o compile` al final de cada capa, no por archivo).

---

## Inventario (lo que cubre cada capa y qué debe explicar el comentario)

| Capa | Nº | Qué debe dejar claro la documentación |
|---|---|---|
| `controller` | 13 | Cada endpoint: verbo+ruta, roles, qué recibe/devuelve, a qué servicio delega, en qué flujo encaja. |
| `service` (interfaces) | 5 | Contrato: qué promete cada operación, independiente de la implementación. |
| `service/impl` | 18 | Reglas de negocio, validaciones, transacciones, efectos secundarios (correos, push, auditoría). |
| `repository` | 15 | Qué consulta cada método derivado/@Query y por qué existe (índice de uso). |
| `entity` | 23 | Qué representa la tabla, relaciones (FK), invariantes, enums de estado. |
| `dto/request` | 19 | Qué payload entra, validaciones (`@NotNull`, etc.) y endpoint origen. |
| `dto/response` | 18 | Qué forma de datos sale y a qué consumidor (app/front) sirve. |
| `security` | 9 | Cadena de filtros JWT, rate limiting, blacklist, user details, handlers. |
| `config` | 9 | Qué configura cada bean (async, cache, seguridad, seeders, auditing). |
| `mapper` | 5 | Mapeo entidad↔DTO (MapStruct), campos no triviales. |
| `exception` | 1 | `GlobalExceptionHandler`: cómo se traduce cada excepción a HTTP. |
| `util` | 2 | `ApiResponse` (envoltura de respuestas) y listener de auditoría. |

**Servicios de mayor complejidad** (requieren comentario inline de algoritmos, no solo Javadoc de método):
- `SolicitudEventoServiceImpl` (1.261 LOC) — ciclo de vida de solicitudes, series recurrentes, publicación.
- `ReporteServiceImpl` (1.134 LOC) — agregaciones y generación de reportes.
- `SolicitudAnuncioServiceImpl` (468 LOC) — flujo de anuncios.
- `NotificacionServiceImpl` (413 LOC) — orquestación de correo/push.
- `AgendaPdfService` (299 LOC) — render de PDF.

---

## Task 1: Documento de arquitectura (`docs/ARQUITECTURA.md`)

Este documento es el que explica "cómo se conecta el sistema entre sí". Se escribe primero porque guía el resto.

**Archivos:**
- Crear: `docs/ARQUITECTURA.md`

- [ ] **Step 1.1: Visión general y diagrama de capas.** Describir el flujo canónico de una petición: `Cliente (app/front) → JwtAuthenticationFilter / RateLimiterFilter → Controller → Service (interfaz) → ServiceImpl → Repository → Entity/DB`, y el camino de vuelta `Entity → Mapper → DTO response → ApiResponse → JSON`. Incluir diagrama Mermaid de capas.
- [ ] **Step 1.2: Flujo de autenticación.** Documentar login local y Microsoft: `AuthController → AuthServiceImpl/MicrosoftAuthServiceImpl → JwtService (emisión) → JwtAuthenticationFilter (validación por request) → TokenBlacklistService (logout)`. Roles del sistema y qué puede cada uno.
- [ ] **Step 1.3: Flujo de ciclo de vida de una Solicitud de Evento.** El flujo central del negocio: `crear → (editar) → listarParaRevision → aprobar/rechazar/devolver → publicar`, incluyendo series recurrentes (acciones masivas por `idGrupo`) y los estados de `EstadoSolicitud`. Diagrama de estados Mermaid.
- [ ] **Step 1.4: Flujo de Solicitud de Anuncio.** Análogo al de eventos, señalando diferencias.
- [ ] **Step 1.5: Flujo de notificaciones.** Cómo y cuándo `NotificacionServiceImpl` dispara correo (`PlantillaCorreoServiceImpl`) y push (`PushNotificationService` + `DispositivoUsuario`), y qué eventos los detonan.
- [ ] **Step 1.6: Flujo de reportes y dashboard.** Qué agrega `ReporteServiceImpl`/`DashboardServiceImpl`, exportación PDF (`AgendaPdfService`), y persistencia en `ReporteGenerado`.
- [ ] **Step 1.7: Auditoría y archivos.** Hibernate Envers (`AuditRevisionListener`, `AuditRevisionEntity`, `AuditoriaController`) y almacenamiento de adjuntos (`ArchivoStorageService`/`ArchivoServiceImpl` + `uploads/`).
- [ ] **Step 1.8: Mapa de endpoints.** Tabla: ruta → método → roles → servicio → descripción (1 línea). Sirve de índice navegable.
- [ ] **Step 1.9: Verificación.** Revisar que cada flujo nombra clases reales (cotejar contra el árbol de paquetes) y que los diagramas Mermaid renderizan.

---

## Task 2: Capa `entity` (23 archivos) — el modelo de datos

Se documenta antes que el resto porque todo lo demás referencia entidades.

**Archivos:** todos en `entity/` + actualizar/crear `entity/package-info.java`.

- [ ] **Step 2.1:** Documentar `BaseEntity`, `Usuario`, `Rol`/`RolEntity`, `Oficina`, `LugarFisico`: responsabilidad, tabla, relaciones, invariantes.
- [ ] **Step 2.2:** Documentar `SolicitudEvento`, `SolicitudEventoParticipante`, `PublicacionEvento`, `TipoEventoCatalogo`, `FrecuenciaRecurrencia`: relaciones y campos de recurrencia.
- [ ] **Step 2.3:** Documentar `SolicitudAnuncio`, `PublicacionAnuncio`, `ArchivoAdjunto`, `ReporteGenerado`.
- [ ] **Step 2.4:** Documentar `DispositivoUsuario`, `NotificacionEnviada`, `AuditRevisionEntity`, y enums (`EstadoSolicitud`, `TipoIngreso`, `TipoParticipante`, `AuthProvider`): qué representa cada valor del enum.
- [ ] **Step 2.5:** `package-info.java` de `entity` describiendo el modelo de dominio global.

---

## Task 3: Capa `repository` (15 archivos)

**Archivos:** todos en `repository/` + `package-info.java` (ya existe, revisar).

- [ ] **Step 3.1:** Javadoc de clase en cada repositorio (qué entidad gestiona).
- [ ] **Step 3.2:** Javadoc por método derivado/`@Query` no trivial: qué devuelve, con qué filtros, y desde qué servicio se llama.

---

## Task 4: Capa `service` (interfaces, 5 archivos)

**Archivos:** `service/*.java` (interfaces: `ArchivoStorageService`, `AuthService`, `DispositivoUsuarioService`, `LugarFisicoService`) + `package-info.java` (existe).

- [ ] **Step 4.1:** Documentar el contrato de cada interfaz y cada método (qué promete, sin detalles de implementación).

---

## Task 5: Capa `service/impl` — servicios simples (13 archivos)

**Archivos:** todos los impl **excepto** los 5 grandes de la Task 6.

- [ ] **Step 5.1:** Auth/identidad: `AuthServiceImpl`, `MicrosoftAuthServiceImpl`, `UsuarioServiceImpl`, `DispositivoUsuarioServiceImpl`.
- [ ] **Step 5.2:** Catálogos/lugares/oficinas: `LugarFisicoServiceImpl`, `OficinaServiceImpl`, `TipoEventoServiceImpl`.
- [ ] **Step 5.3:** Soporte: `ArchivoServiceImpl`, `DashboardServiceImpl`, `AuditoriaServiceImpl`, `PlantillaCorreoServiceImpl`, `PushNotificationService`.
- [ ] **Step 5.4:** Javadoc de clase + método en cada uno: reglas de negocio, `@Transactional`, efectos secundarios.

---

## Task 6: Capa `service/impl` — servicios complejos (5 archivos)

Aquí va el mayor esfuerzo: Javadoc de método **+ comentarios inline** en los algoritmos no obvios.

- [ ] **Step 6.1:** `SolicitudEventoServiceImpl` (1.261 LOC): documentar cada operación pública del ciclo de vida; comentar inline la lógica de series recurrentes y generación de fechas (cotejar con `RecurrenciaFechasTest`).
- [ ] **Step 6.2:** `ReporteServiceImpl` (1.134 LOC): documentar cada generador de reporte; comentar inline agregaciones/agrupaciones.
- [ ] **Step 6.3:** `SolicitudAnuncioServiceImpl` (468 LOC): ciclo de vida de anuncios.
- [ ] **Step 6.4:** `NotificacionServiceImpl` (413 LOC): qué evento dispara qué notificación; comentar inline la orquestación correo/push.
- [ ] **Step 6.5:** `AgendaPdfService` (299 LOC): comentar inline el armado del PDF (secciones, estilos, paginación).

---

## Task 7: Capa `controller` (13 archivos)

**Archivos:** todos en `controller/` + `package-info.java` (existe).

- [ ] **Step 7.1:** Javadoc de clase: qué recurso expone y para qué consumidor.
- [ ] **Step 7.2:** Javadoc por endpoint: verbo+ruta, roles (`@PreAuthorize`), payload in/out, servicio destino y flujo. Empezar por `AuthController`, `SolicitudEventoController`, `SolicitudAnuncioController`, `ReporteController` (los más usados).
- [ ] **Step 7.3:** Resto de controllers (Archivo, Auditoria, Dashboard, DispositivoUsuario, LugarFisico, Oficina, TipoEvento, Usuario).

---

## Task 8: Capas `security` y `config` (18 archivos)

- [ ] **Step 8.1:** `security/`: `SecurityConfig` (cadena de filtros y reglas de autorización), `JwtAuthenticationFilter`, `JwtService`, `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`, `RateLimiterFilter`, `TokenBlacklistService`, `CustomUserDetailsService`. Explicar el orden de filtros y qué hace cada uno por petición.
- [ ] **Step 8.2:** `config/`: `SecurityConfig`-relacionados, `AsyncConfig`, `CacheConfig`, `JpaAuditingConfig`, `PasswordConfig`, `DataInitializer`, `OficinaDataSeeder`, `AppSecurityProperties`, `SecurityProperties`. Qué bean crea cada clase y por qué.

---

## Task 9: Capas `dto`, `mapper`, `exception`, `util` (45 archivos)

- [ ] **Step 9.1:** `dto/request` (19): Javadoc de clase indicando endpoint origen y resumen de validaciones.
- [ ] **Step 9.2:** `dto/response` (18): Javadoc de clase indicando consumidor y forma de datos.
- [ ] **Step 9.3:** `mapper` (5): qué mapea cada uno; documentar mapeos no triviales.
- [ ] **Step 9.4:** `exception/GlobalExceptionHandler`: documentar cada handler (excepción → status HTTP → cuerpo).
- [ ] **Step 9.5:** `util/ApiResponse` y `util/AuditRevisionListener`.
- [ ] **Step 9.6:** Clase raíz `CallappBackendApplication` y `package-info.java` raíz (visión global del backend).

---

## Task 10: Verificación final

- [ ] **Step 10.1: Compilación.** `./mvnw -q -o compile` → sin errores (los comentarios no rompen nada).
- [ ] **Step 10.2: Tests intactos.** `./mvnw -q -o test` → la misma suite que antes pasa (no se tocó lógica).
- [ ] **Step 10.3: Javadoc genera sin warnings graves.** `./mvnw -q -o javadoc:javadoc` → revisar que el HTML se produce; corregir `@param`/`@return` faltantes que reporte el doclet.
- [ ] **Step 10.4: Cobertura de documentación.** Verificar que no quede ninguna clase pública sin Javadoc de clase y ningún endpoint sin Javadoc (búsqueda de `public class`/`@*Mapping` sin bloque `/**` encima).
- [ ] **Step 10.5: Consistencia del `ARQUITECTURA.md`.** Cotejar que todos los nombres de clase citados existan y que el mapa de endpoints coincida con los controllers reales.

---

## Orden de ejecución recomendado

`Task 1 (arquitectura) → 2 (entity) → 3 (repository) → 4 (service) → 5 (impl simples) → 6 (impl complejos) → 7 (controllers) → 8 (security/config) → 9 (dto/mapper/util) → 10 (verificación)`.

Cada Task termina con `./mvnw -o compile` antes de pasar a la siguiente para detectar cualquier ruptura temprano.

## Criterio de "hecho"

1. Toda clase pública tiene Javadoc de clase.
2. Todo método público/endpoint tiene Javadoc con `@param`/`@return`/`@throws` donde aplique.
3. Cada paquete tiene `package-info.java`.
4. Los 5 servicios complejos tienen comentarios inline en sus algoritmos.
5. `docs/ARQUITECTURA.md` explica las capas y los 7 flujos clave con diagramas.
6. `compile`, `test` y `javadoc:javadoc` se ejecutan sin errores.

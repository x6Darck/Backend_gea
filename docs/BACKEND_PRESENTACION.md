# CallApp Backend

## Resumen
CallApp es un backend en Spring Boot orientado a la gestion institucional de:

- solicitudes de eventos creadas por oficinas
- solicitudes de anuncios creadas desde la app movil autenticada
- revision, aprobacion, rechazo y publicacion por el equipo de comunicaciones
- visualizacion publica de eventos y anuncios ya publicados
- reportes, auditoria, archivos y notificaciones

La arquitectura sigue una separacion clara entre:

- `Solicitud`: dato interno que se revisa y cambia de estado
- `Publicacion`: contenido final visible en app movil

## Stack tecnico
- Java 21
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA + MySQL
- Hibernate Envers para auditoria
- OpenPDF para reportes PDF
- Apache Commons CSV para exportacion CSV
- JavaMailSender para correos

## Roles del sistema
- `SUPER_ADMIN`: control total del sistema
- `COMUNICACIONES`: revisa, aprueba, rechaza y publica
- `OFICINA`: crea y consulta solicitudes de evento
- `USUARIO_APP`: consume publicaciones
- `USUARIO_AUTENTICADO_APP`: consume publicaciones y crea solicitudes de anuncio

Compatibilidad heredada:
- `ADMIN`
- `USUARIO`

## Modulos funcionales
- `auth`: login JWT local y login Microsoft para movil
- `usuarios`: administracion de usuarios y oficinas
- `eventos`: solicitudes, revision y publicacion de eventos
- `anuncios`: solicitudes, revision y publicacion de anuncios
- `dashboard`: resumen para panel web
- `reportes`: resumen, historial y exportaciones
- `archivos`: subida y exposicion controlada de piezas graficas
- `auditoria`: historial de cambios por entidad (Hibernate Envers)
- `seguridad`: historial de inicios de sesion (exito/fallo, local y Microsoft), solo SUPER_ADMIN
- `notificaciones`: correos y trazabilidad de envios

## Flujo principal de negocio
### Eventos
1. `OFICINA` crea una solicitud de evento.
2. La solicitud queda en `PENDIENTE`.
3. `COMUNICACIONES` la aprueba o rechaza.
4. Si se aprueba, crea la `PublicacionEvento`.
5. El evento queda disponible en la app.

### Anuncios
1. `USUARIO_AUTENTICADO_APP` crea una solicitud de anuncio.
2. La solicitud queda en `PENDIENTE`.
3. `COMUNICACIONES` la aprueba o rechaza.
4. Si se aprueba, crea la `PublicacionAnuncio`.
5. El anuncio queda visible en la app.

## Seguridad
### Autenticacion
- `POST /auth/login`: login local con correo y password
- `POST /auth/microsoft/mobile`: login movil con `idToken` de Microsoft

### Autorizacion
La seguridad se aplica en dos niveles:

- reglas globales en [`SecurityConfig.java`](../src/main/java/com/calendario/callapp/callapp_backend/security/SecurityConfig.java)
- reglas finas por endpoint con `@PreAuthorize`

### Header esperado
```http
Authorization: Bearer TU_JWT
```

## Configuracion importante
Archivo principal:
- [`application.properties`](../src/main/resources/application.properties)

Propiedades clave:
- `server.port`
- `spring.datasource.*`
- `jwt.secret`
- `jwt.expiration`
- `microsoft.tenant-id`
- `microsoft.client-id`
- `spring.mail.*`
- `app.notifications.email-enabled`
- `app.notifications.university-recipients`
- `file.upload-dir`
- `file.allowed-extensions`
- `file.allowed-content-types`
- `file.max-size-bytes`

## Como ejecutar el proyecto
### Compilar
```powershell
cmd /c mvnw.cmd -q -DskipTests compile
```

### Ejecutar
```powershell
cmd /c mvnw.cmd spring-boot:run
```

### Ejecutar en otro puerto
```powershell
$env:SERVER_PORT=8081
cmd /c mvnw.cmd spring-boot:run
```

### Tests
```powershell
cmd /c mvnw.cmd -q test
```

## Modelo de datos principal
- `usuarios`
- `oficinas`
- `tipos_evento`
- `solicitudes_evento`
- `solicitud_evento_participantes`
- `publicaciones_evento`
- `solicitudes_anuncio`
- `publicaciones_anuncio`
- `reportes_generados`
- `archivos_adjuntos`
- `notificaciones_enviadas`
- `historial_login` (intentos de login, retencion 90 dias)
- tablas de auditoria `*_aud` y `revinfo`

## Endpoints
## 1. Autenticacion
### `POST /auth/login`
Uso:
- login web tradicional con correo y password

Body:
```json
{
  "correo": "admin@unilibre.edu.co",
  "password": "123456"
}
```

Respuesta:
```json
{
  "id": 4,
  "token": "JWT",
  "nombre": "Facultad Derecho",
  "correo": "admin@unilibre.edu.co",
  "rol": "ADMIN",
  "idOficina": 4,
  "oficinaNombre": "Facultad Derecho",
  "fotoUrl": null,
  "tipo": "Bearer"
}
```
El JWT tambien se manda como cookie HttpOnly `gea_auth` para el frontend web
(el body lo usa la app movil como Bearer). Cada intento (exito o fallo)
queda registrado para el panel de seguridad (ver seccion 15).

### `POST /auth/microsoft/mobile`
Uso:
- login movil con `idToken` emitido por Microsoft (flujo PKCE)

Body:
```json
{
  "idToken": "TOKEN_DE_MICROSOFT"
}
```

### `POST /auth/microsoft/mobile/code`
Uso:
- variante para apps registradas en Azure como cliente confidencial: recibe
  el codigo de autorizacion en vez del idToken directo, el backend lo
  intercambia con el client_secret (nunca sale del servidor)

Body:
```json
{
  "code": "CODIGO_DE_AUTORIZACION"
}
```

## 2. Usuarios
### `GET /admin/usuarios`
Uso:
- lista usuarios
- filtros disponibles: `q`, `rol`

Ejemplo:
```http
GET /admin/usuarios?q=jean&rol=OFICINA
```

### `GET /admin/usuarios/{id}`
Uso:
- detalle de un usuario

### `POST /admin/usuarios`
Uso:
- crear usuario desde dashboard

Body ejemplo:
```json
{
  "nombre": "Jean Pier",
  "correo": "jean@unilibre.edu.co",
  "telefono": "3000000000",
  "password": "123456",
  "rol": "OFICINA",
  "idOficina": 1,
  "authProvider": "LOCAL"
}
```

### `PUT /admin/usuarios/{id}`
Uso:
- editar usuario

## 3. Oficinas
### `GET /admin/oficinas`
Uso:
- listar oficinas

### `POST /admin/oficinas`
Uso:
- crear oficina

Body ejemplo:
```json
{
  "nombre": "Bienestar",
  "programaAcademico": "Institucional",
  "descripcion": "Gestiona actividades de bienestar universitario",
  "activa": true
}
```

## 4. Tipos de evento
### `GET /usuario/tipos-evento`
Uso:
- listar catalogo visible para formularios

### `POST /usuario/tipos-evento`
Uso:
- crear o ampliar catalogo de tipos

Body ejemplo:
```json
{
  "nombre": "ACADEMICO",
  "descripcion": "Eventos de caracter academico",
  "colorHex": "#2563EB",
  "activo": true
}
```

## 5. Solicitudes de evento
### `POST /oficina/solicitudes-evento`
Uso:
- crear solicitud de evento por parte de una oficina

Body ejemplo:
```json
{
  "nombreEvento": "Torneo de ajedrez",
  "descripcionEvento": "Evento institucional abierto",
  "fechaEvento": "2026-04-10",
  "horaInicio": "08:00:00",
  "horaFin": "12:00:00",
  "lugar": "Auditorio",
  "responsableEvento": "Bienestar",
  "tipoEvento": "ACADEMICO",
  "participantes": [
    {
      "nombre": "Invitado 1",
      "cargo": "Docente",
      "descripcion": "Ponente principal",
      "fotoUrl": "https://...",
      "tipo": "INVITADO"
    }
  ]
}
```

### `GET /oficina/solicitudes-evento`
Uso:
- listar solicitudes propias
- filtros: `q`, `estado`, `mes`, `anio`

### `GET /oficina/solicitudes-evento/{id}`
Uso:
- detalle de solicitud propia

### `PUT /oficina/solicitudes-evento/{id}`
Uso:
- actualizar solicitud propia si sigue editable

### `DELETE /oficina/solicitudes-evento/{id}`
Uso:
- eliminar solicitud no publicada

## 6. Revision de eventos
### `GET /comunicaciones/solicitudes-evento`
Uso:
- bandeja de revision de eventos
- filtros: `q`, `estado`, `mes`, `anio`

### `GET /comunicaciones/solicitudes-evento/{id}`
Uso:
- detalle para aprobar o rechazar

### `POST /comunicaciones/solicitudes-evento/{id}/aprobar`
Uso:
- cambiar estado a aprobado

### `POST /comunicaciones/solicitudes-evento/{id}/rechazar`
Uso:
- rechazar con motivo

Body:
```json
{
  "motivo": "Falta informacion del responsable"
}
```

### `POST /comunicaciones/solicitudes-evento/{id}/publicar`
Uso:
- crear o actualizar publicacion final del evento

Body ejemplo:
```json
{
  "tituloVisible": "Torneo Universidad Libre Ajedrez",
  "descripcionVisible": "Ya disponible en el campus",
  "piezaGraficaUrl": "/archivos/public/TOKEN",
  "fechaPublicacion": "2026-04-08T09:00:00"
}
```

## 7. Eventos publicados
### `GET /app/eventos/publicados`
Uso:
- consultar calendario publico
- filtros: `filtro`, `fecha`, `q`, `tipoEvento`, `mes`, `anio`

Ejemplos:
```http
GET /app/eventos/publicados?filtro=dia&fecha=2026-04-10
GET /app/eventos/publicados?filtro=mes&fecha=2026-04-01
GET /app/eventos/publicados?q=ajedrez&tipoEvento=ACADEMICO
```

### `GET /app/eventos/publicados/{id}`
Uso:
- detalle de un evento publicado

### `GET /app/eventos/proximos`
Uso:
- panel lateral de proximos eventos
- filtros: `limit`, `q`

## 8. Solicitudes de anuncio
### `POST /app/solicitudes-anuncio`
Uso:
- crear solicitud de anuncio desde la app autenticada

Body ejemplo:
```json
{
  "titulo": "Se requieren monitorias",
  "descripcion": "Convocatoria abierta",
  "categoria": "Academico",
  "lugar": "Bloque A",
  "correoContacto": "coord@unilibre.edu.co",
  "responsableAnuncio": "Coordinacion Academica",
  "fechaInicioPublicacion": "2026-04-01",
  "fechaFinPublicacion": "2026-04-20",
  "horaInicio": "08:00:00",
  "horaFin": "17:00:00"
}
```

### `GET /app/solicitudes-anuncio/mis-solicitudes`
Uso:
- listar solicitudes propias
- filtros: `q`, `estado`, `mes`, `anio`

## 9. Revision de anuncios
### `GET /comunicaciones/solicitudes-anuncio`
Uso:
- bandeja de revision de anuncios

### `GET /comunicaciones/solicitudes-anuncio/{id}`
Uso:
- detalle de solicitud de anuncio

### `POST /comunicaciones/solicitudes-anuncio/{id}/aprobar`
Uso:
- aprobar anuncio

### `POST /comunicaciones/solicitudes-anuncio/{id}/rechazar`
Uso:
- rechazar con motivo

Body:
```json
{
  "motivo": "Falta fecha de finalizacion"
}
```

### `POST /comunicaciones/solicitudes-anuncio/{id}/publicar`
Uso:
- publicar anuncio

Body ejemplo:
```json
{
  "tituloVisible": "Monitorias abiertas 2026-1",
  "descripcionVisible": "Revisa requisitos y aplica",
  "piezaGraficaUrl": "/archivos/public/TOKEN",
  "fechaPublicacion": "2026-04-01T08:00:00"
}
```

## 10. Anuncios publicados
### `GET /app/anuncios/publicados`
Uso:
- consulta publica de anuncios
- filtros: `q`, `categoria`, `mes`, `anio`

### `GET /app/anuncios/publicados/{id}`
Uso:
- detalle de anuncio publicado

## 11. Dashboard
### `GET /dashboard/resumen`
Uso:
- resumen del panel para roles internos
- devuelve conteos y proximos eventos

## 12. Reportes
### `POST /reportes/solicitudes`
Uso:
- crear registro de reporte y dejarlo en historial

Body ejemplo:
```json
{
  "nombre": "Reporte semana 1",
  "descripcion": "Eventos y anuncios de la semana",
  "formato": "PDF",
  "desde": "2026-04-01",
  "hasta": "2026-04-07"
}
```

### `GET /reportes/solicitudes`
Uso:
- listar historial de reportes generados

### `GET /reportes/solicitudes/resumen`
Uso:
- resumen inmediato por rango de fechas

### `GET /reportes/solicitudes/export/csv`
Uso:
- exportacion directa en CSV

### `GET /reportes/solicitudes/export/pdf`
Uso:
- exportacion directa en PDF

### `GET /reportes/solicitudes/{id}/export`
Uso:
- exportar reporte previamente registrado

## 13. Archivos
### `POST /comunicaciones/archivos/upload`
Uso:
- subir pieza grafica o archivo permitido

Formato:
- `multipart/form-data`
- campo esperado: `archivo`

Respuesta:
```json
{
  "id": 1,
  "nombreArchivo": "uuid.png",
  "nombreOriginal": "pieza.png",
  "tokenAcceso": "TOKEN",
  "url": "/archivos/public/TOKEN",
  "contentType": "image/png",
  "tamano": 12345
}
```

### `GET /archivos/public/{tokenAcceso}`
Uso:
- descarga publica controlada de archivo

## 14. Auditoria
### `GET /admin/auditoria/usuarios/{id}`
Uso:
- historial de cambios de usuario

### `GET /admin/auditoria/solicitudes-evento/{id}`
Uso:
- historial de cambios de una solicitud de evento

### `GET /admin/auditoria/solicitudes-anuncio/{id}`
Uso:
- historial de cambios de una solicitud de anuncio

## 15. Seguridad — historial de login
### `GET /admin/seguridad/historial-login`
Uso:
- panel de seguridad, solo rol SUPER_ADMIN (mas restringido que `/admin/auditoria/**`, que tambien permite ADMIN/COMUNICACIONES)
- registra cada intento de login (exito y fallo, local y Microsoft) con IP, metodo, motivo de fallo y fecha
- filtros: `correo`, `ip`, `exito`, `desde`, `hasta`; paginado (`page`, `size`)
- retencion automatica de 90 dias (purga diaria programada)

Ejemplo:
```http
GET /admin/seguridad/historial-login?exito=false&desde=2026-07-01&page=0&size=25
```

## Respuestas frecuentes para frontend
- `200 OK`: lectura o actualizacion correcta
- `201 Created`: creacion correcta
- `204 No Content`: eliminacion correcta
- `400 Bad Request`: validacion o regla de negocio
- `401 Unauthorized`: token invalido o ausente
- `403 Forbidden`: rol sin permiso
- `404 Not Found`: recurso inexistente

## Migraciones Flyway
El esquema se versiona con Flyway en
[`src/main/resources/db/migration`](../src/main/resources/db/migration)
(`V1` a `V9` actualmente — la mas reciente agrega `historial_login` para el
panel de seguridad).

## Correos y archivos
### Plantillas HTML
- [`evento-publicado.html`](../src/main/resources/mail-templates/evento-publicado.html)
- [`anuncio-publicado.html`](../src/main/resources/mail-templates/anuncio-publicado.html)

### Restricciones de archivos
- extensiones permitidas: `jpg`, `jpeg`, `png`, `pdf`
- tipos permitidos: `image/jpeg`, `image/png`, `application/pdf`
- tamano maximo configurable por propiedad

## Puntos fuertes para presentar
- separacion clara entre solicitud y publicacion
- seguridad JWT con roles
- login Microsoft preparado para app movil
- filtros por dia, semana, mes y anio
- dashboard con conteos y proximos eventos
- reportes PDF y CSV
- auditoria con Envers
- subida segura de archivos por token
- notificaciones por correo con plantilla HTML
- base de datos ya migrada y alineada con el dominio actual

## Estado actual del backend
El backend esta funcional, integrado con el frontend, y cubre el flujo
principal del sistema mas rate limiting, panel de seguridad (historial de
login) y despliegue nativo automatizado (`deploy/native/install.sh`, ver
tambien `docs/produccion-checklist.md` para el paso a paso pendiente de
produccion). Lo que normalmente seguiria despues es:

- documentacion OpenAPI/Swagger
- endurecimiento de credenciales productivas SMTP y Microsoft en el servidor real

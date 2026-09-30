# Checklist de paso a producción — GEA

Pendiente de esta sesión: el código está listo y en `main` (backend y
frontend). Esto es lo que falta hacer, paso a paso, y qué variables cambiar.
`deploy/native/install.sh` automatiza casi todo lo mecánico — esta lista es
específicamente lo que **hay que decidir y completar a mano** antes/durante.

Contexto del servidor real: XAMPP (Apache + MariaDB 10.4.32).
---

## 0. Antes de empezar

- [ ] Confirmar el **dominio real** (ej. `gea.unilibrecucuta.edu.co`) y que el
      DNS ya apunta a la IP del servidor.
- [ ] Confirmar que XAMPP (Apache + MariaDB) están corriendo en el servidor.
- [ ] Acceso SSH/RDP + sudo al servidor.
- [ ] Los repos `GEA_BACKEND` y `GEA_FRONT` ya clonados en el servidor —
      confirmar la ruta real del frontend, porque `install.sh` trae
      hardcodeado por defecto `/opt/lampp/htdocs/gea/GEA_FRONT-main` (se
      puede pisar con `FRONTEND_REPO_PATH=...` si cambió).

## 1. Base de datos

- [ ] Crear la base de datos y un **usuario dedicado** (nunca `root`) en la
      MariaDB de XAMPP — por phpMyAdmin o `mysql -u root`. Ver
      `deploy/native/README.md` sección 2 para el `CREATE DATABASE` /
      `CREATE USER` / `GRANT` exactos (**importante**: incluye
      `CREATE VIEW`/`SHOW VIEW`, sin eso Flyway falla al migrar el esquema
      baseline).
- [ ] Decidir qué hacer con los datos iniciales:
  - **Si el servidor ya tiene datos reales** (eventos, anuncios, usuarios
    reales de uso previo): **NO** importar ningún dump completo — eso
    borraría todo. Solo hace falta que Flyway aplique la migración nueva
    (`V9`, tabla `historial_login`) automáticamente al arrancar el backend
    con el código nuevo. No se requiere ninguna acción manual aparte.
  - **Si es un servidor nuevo/vacío**: se puede importar el dump de
    referencia (`gea_db_dump.sql`, en la raíz de este repo y también en
    `Downloads`) — trae catálogos (oficinas, roles, tipos de evento, lugares
    físicos) y 11 usuarios institucionales reales, sin eventos/anuncios de
    prueba.
    **⚠️ CRÍTICO si se usa este dump:** todos los usuarios que trae tienen la
    contraseña `123456` (un placeholder, no la contraseña real de nadie) —
    **hay que cambiarla de inmediato** después de importar, antes de que el
    sistema quede accesible públicamente. No es una contraseña real filtrada,
    pero sí una conocida y genérica.

## 2. Variables de entorno del backend (`.env`)

```bash
cd GEA_BACKEND
cp deploy/native/.env.example .env
nano .env   # completar cada CAMBIAR_POR_...
```

| Variable | Qué poner |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Credenciales reales del usuario creado en el paso 1 (nunca `root`) |
| `JWT_SECRET` | Generar con `openssl rand -base64 48` — nunca reusar el de pruebas |
| `CORS_ALLOWED_ORIGINS` | `https://<dominio-real>` |
| `FRONTEND_URL` | `https://<dominio-real>` |
| **`COOKIE_SECURE`** | **`true`** — esta es la señal clave: activa cookies solo-HTTPS **y** hace que `install.sh` active automáticamente el modo "Apache-puente" para XAMPP (no hay que pasarlo a mano) |
| `RATE_LIMIT_TRUST_PROXY` | `true` — necesario para que el limitador de intentos vea la IP real del usuario detrás de Nginx/Apache, no la de Apache |
| `MICROSOFT_TENANT_ID` / `MICROSOFT_CLIENT_ID` / `MICROSOFT_CLIENT_SECRET` | Del registro real "GeaApp" en Azure Entra ID (vacío = login Microsoft deshabilitado, el backend arranca igual si no está listo) |
| `MICROSOFT_REDIRECT_URI` | El de la app móvil (deep link), confirmar que coincide con lo registrado en Azure — no es la URL del sitio web |
| `SMTP_USER` / `SMTP_PASS` | Cuenta de correo real + **contraseña de aplicación** (no la contraseña normal de la cuenta) |
| `UNIVERSITY_RECIPIENTS` | Lista real de correos que reciben notificaciones |
| `JAVA_OPTS` (`-Xmx`) | Ajustar según la RAM real del servidor (tabla de referencia en `docs/superpowers/plans/2026-07-01-plan-migracion-nube-servidor-unico.md`) |

## 3. Frontend

- [ ] Editar `GEA_FRONT/.env.production`: cambiar `VITE_API_URL` (hoy apunta
      a la IP del servidor de pruebas, `10.4.100.141:8080`) al dominio real:
      ```
      VITE_API_URL=https://<dominio-real>
      ```
      Sin puerto. Esto se hornea en el build (`npm run build`), así que debe
      estar correcto **antes** de correr `install.sh`.

## 4. Certificado TLS

- [ ] Certificado real en `/etc/nginx/certs/fullchain.pem` y
      `/etc/nginx/certs/privkey.pem` — con `certbot` si el dominio es
      alcanzable públicamente, o el que entregue la CA interna de la
      universidad.

## 5. Correr `install.sh`

```bash
sudo ./deploy/native/install.sh
```

- Detecta XAMPP y activa el modo Apache-puente solo (porque `COOKIE_SECURE=true`).
- Si además se va a publicar la PWA de la app móvil:
  ```bash
  sudo DEPLOY_PWA=true ./deploy/native/install.sh
  ```
  (requiere el Flutter SDK instalado en el servidor; revisar
  `gea_app/dart_define.produccion.json` con el dominio real primero).

## 6. Después de instalar

- [ ] Backend arriba: `curl http://127.0.0.1:8084/actuator/health`
- [ ] Entrar por el dominio real en el navegador y probar login (correo y,
      si aplica, Microsoft).
- [ ] Si se importó el dump de referencia: **cambiar la contraseña `123456`**
      de cada usuario real (recordatorio del punto 1 — no dejarlo para después).
- [ ] Si el servidor **ya tenía usuarios con sesión activa** de antes de este
      código: pedirles que cierren sesión y vuelvan a entrar una vez — un fix
      de esta sesión (incluir el `id` del usuario en la respuesta de login)
      solo toma efecto en logins nuevos, no en sesiones ya guardadas en el
      navegador.
- [ ] Probar el panel de seguridad nuevo (`/seguridad`, solo SuperAdmin):
      confirmar que un login de prueba (correcto y con contraseña
      equivocada) aparece en el historial.



# 1. Guía de arranque

Esta parte describe cómo poner en marcha **los tres sistemas de GEA en un entorno de desarrollo local**: el backend (Spring Boot), el frontend web (React + Vite) y la aplicación móvil (Flutter). Todos los valores indicados están extraídos directamente de los archivos de configuración reales de cada repositorio.

## 1.1 Requisitos previos

Antes de clonar y arrancar los proyectos, asegúrate de tener instaladas las siguientes herramientas. Las versiones provienen de `pom.xml`, `package.json` y `pubspec.yaml`.

| Herramienta | Versión requerida | Propósito |
|-------------|-------------------|-----------|
| **JDK (Java)** | **21** (`java.version=21` en `pom.xml`) | Compilar y ejecutar el backend Spring Boot 3.4.3. |
| **Maven** | No es necesario instalarlo: el repo incluye el *wrapper* `mvnw` / `mvnw.cmd` | Gestión de dependencias y build del backend. |
| **Node.js** | **18 o superior** (el entorno de referencia usa Node v22) | Ejecutar Vite y el panel web. Vite 8 requiere Node moderno. |
| **npm** | El que acompaña a Node | Instalar dependencias del frontend. |
| **Flutter SDK** | Dart **^3.11.5** (`environment.sdk` en `pubspec.yaml`) | Compilar y ejecutar la app móvil. |
| **MySQL** | **8.x** | Base de datos del backend (`gea`). |
| **Git** | Cualquier versión reciente | Clonar los repositorios. |
| **Google Chrome** (opcional) | — | Sólo necesario para regenerar el PDF de este manual (`build.js`). |

> Nota: el backend usa el conector `mysql-connector-j` y el dialecto de MySQL 8. No se utiliza H2 en ejecución; H2 sólo está declarado con `scope=test` para pruebas de integración en memoria.

## 1.2 Estructura de repositorios

GEA se compone de tres repositorios independientes. Las rutas locales corresponden al entorno de desarrollo de referencia.

| Sistema | Repositorio GitHub | Ruta local de referencia |
|---------|--------------------|--------------------------|
| Backend (API REST) | `x6Darck/GEA_BACKEND` | `C:\Users\Administrador\Documents\test\GEA_BACKEND` |
| Frontend (panel web) | `x6Darck/GEA_FRONT` | `C:\Users\Administrador\Documents\Proyecto 2\GEA_FRONT` |
| App móvil (Flutter) | `x6Darck/GEA_MOVIL` | `C:\Users\Administrador\Documents\gea_app` |

> La ruta local es indicativa; puedes clonar cada repositorio donde prefieras. Lo importante es respetar la configuración de puertos y variables que se describe a continuación.

## 1.3 Backend — levantar en local

El backend es una aplicación Spring Boot que expone la API REST bajo el *context-path* `/api` en el puerto **8083**, con un puerto de administración (Actuator) separado en el **8084**.

**Pasos:**

1. **Clonar el repositorio:**

   ```bash
   git clone https://github.com/x6Darck/GEA_BACKEND.git
   cd GEA_BACKEND
   ```

2. **Crear la base de datos** en tu servidor MySQL local. El nombre por defecto es `gea` (definido en `DB_URL`):

   ```sql
   CREATE DATABASE gea CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

   La URL de conexión por defecto es:
   `jdbc:mysql://localhost:3306/gea?useSSL=false&serverTimezone=America/Bogota&allowPublicKeyRetrieval=true&characterEncoding=UTF-8`

3. **Configurar las credenciales de la base de datos.** Por defecto el backend usa usuario `root` y contraseña `1234`. Si tu MySQL local difiere, puedes:
   - Definir las variables de entorno `DB_USERNAME` y `DB_PASSWORD` (recomendado), o
   - Editar directamente `src/main/resources/application.properties`.

   En PowerShell (Windows):

   ```powershell
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "tu_password"
   ```

4. **Levantar la aplicación** con el wrapper de Maven incluido en el repo. En Windows usa `mvnw.cmd`:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   En Linux/macOS:

   ```bash
   ./mvnw spring-boot:run
   ```

   Durante el arranque ocurre lo siguiente automáticamente:
   - **Flyway** ejecuta las migraciones de `classpath:db/migration`, desde la baseline `V1__baseline_schema.sql` hasta la más reciente (`V9`, actualmente). El esquema queda versionado; `baseline-on-migrate=true` permite tomar como base una BD ya existente.
   - Hibernate arranca con `ddl-auto=validate`, es decir **valida** el esquema contra las entidades pero **no lo modifica** (las migraciones las hace Flyway, no Hibernate).
   - Los componentes `DataInitializer` y `OficinaDataSeeder` (en `config/`) siembran los datos iniciales (usuarios/roles base y catálogo de oficinas).

5. **Verificar que el backend responde:**
   - API REST: `http://localhost:8083/api`
   - Documentación Swagger UI: `http://localhost:8083/api/swagger-ui.html`
   - Health check (Actuator, puerto de management 8084): `http://localhost:8084/actuator/health`

> El servidor escucha en `server.address=0.0.0.0`, por lo que también es accesible desde otros dispositivos de la red local mediante la IP de la máquina (necesario para probar la app móvil en un teléfono físico — ver 1.5).

## 1.4 Frontend — levantar en local

El panel administrativo está construido con **React 19** y **Vite 8**. La URL del backend se inyecta mediante la variable `VITE_API_URL`.

**Pasos:**

1. **Clonar y entrar al directorio:**

   ```bash
   git clone https://github.com/x6Darck/GEA_FRONT.git
   cd GEA_FRONT
   ```

2. **Instalar dependencias:**

   ```bash
   npm install
   ```

3. **Crear el archivo `.env.local`** en la raíz del proyecto apuntando al backend local:

   ```ini
   VITE_API_URL=http://localhost:8083
   ```

   Cómo se usa: en `src/services/api.js` la instancia de Axios construye su `baseURL` como `` `${API_URL}/api` `` a partir de `import.meta.env.VITE_API_URL`, con *fallback* a `http://localhost:8083` si la variable no está definida. Es decir, **se indica sólo el host y el puerto; el sufijo `/api` lo añade el cliente automáticamente** (coincidiendo con el `context-path` del backend).

   > Para el entorno productivo existe `.env.production` con `VITE_API_URL=https://api.gea.tudominio.com`. No lo uses en desarrollo local.

4. **Arrancar el servidor de desarrollo:**

   ```bash
   npm run dev
   ```

   Vite levanta el panel en `http://localhost:5173`. Este origen ya está incluido en la lista CORS por defecto del backend (`CORS_ALLOWED_ORIGINS`), por lo que las peticiones funcionan sin configuración adicional.

> Otros scripts disponibles (`package.json`): `npm run build` (build de producción), `npm run preview` (servir el build) y `npm run lint` (ESLint).

## 1.5 App Flutter — levantar en local

La aplicación móvil usa **Flutter (Dart ^3.11.5)**, **Riverpod** para el estado y **GoRouter** para la navegación. La URL del backend se inyecta en tiempo de compilación con `--dart-define=API_BASE_URL=...`.

**Pasos:**

1. **Obtener las dependencias:**

   ```bash
   flutter pub get
   ```

2. **Generar las localizaciones** (el proyecto declara `flutter: generate: true` y usa `flutter_localizations` + `intl`):

   ```bash
   flutter gen-l10n
   ```

3. **Ejecutar la app** indicando la URL del backend. En un **emulador o dispositivo físico** debes usar la **IP de la PC** que ejecuta el backend, no `localhost` (en el teléfono `localhost` apunta al propio teléfono):

   ```bash
   flutter run --dart-define=API_BASE_URL=http://<IP_DE_TU_PC>:8083/api
   ```

   Ejemplo, si tu PC tiene la IP `192.168.1.20`:

   ```bash
   flutter run --dart-define=API_BASE_URL=http://192.168.1.20:8083/api
   ```

   Cómo se usa: en `lib/core/config/app_config.dart`, `AppConfig.apiBaseUrl` se lee con `String.fromEnvironment('API_BASE_URL', ...)`. Si no pasas el `--dart-define`, se aplica el valor por defecto `http://172.46.12.57:8083/api`. **A diferencia del frontend web, aquí sí debes incluir el sufijo `/api` en la URL.**

   > Recuerda que el backend escucha en `0.0.0.0:8083`, lo que permite que el teléfono lo alcance por la IP de la PC siempre que ambos estén en la misma red y el firewall lo permita.

4. **Builds de producción** (referencia, ver comentario en `app_config.dart`):

   ```bash
   flutter build apk --dart-define=API_BASE_URL=https://api.gea.tudominio.com/api
   ```

## 1.6 Variables de entorno (referencia completa)

Tabla maestra de todas las variables configurables. Para el backend, cada fila corresponde a un patrón `${VAR:valor_por_defecto}` de `application.properties`.

### Backend (`application.properties`)

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `DB_URL` | `jdbc:mysql://localhost:3306/gea?useSSL=false&serverTimezone=America/Bogota&allowPublicKeyRetrieval=true&characterEncoding=UTF-8` | URL JDBC de la base de datos MySQL. |
| `DB_USERNAME` | `root` | Usuario de la base de datos. |
| `DB_PASSWORD` | `1234` | Contraseña de la base de datos. |
| `DB_POOL_MAX` | `20` | Tamaño máximo del pool de conexiones HikariCP (`maximum-pool-size`). |
| `DB_POOL_MIN` | `5` | Conexiones mínimas en reposo del pool (`minimum-idle`). |
| `DDL_AUTO` | `validate` | Estrategia DDL de Hibernate. En producción usar `validate` o `none`; `update` puede corromper datos. |
| `FLYWAY_ENABLED` | `true` | Activa/desactiva las migraciones Flyway al arranque. |
| `JWT_SECRET` | `dev_only_change_this_secret_in_production_min32chars` | Secreto de firma de los JWT. Mínimo 32 caracteres. **Nunca usar el valor por defecto en producción.** |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Orígenes permitidos por CORS (lista separada por comas). |
| `SMTP_HOST` | `smtp.gmail.com` | Host del servidor SMTP para el envío de correos. |
| `SMTP_PORT` | `587` | Puerto SMTP. |
| `SMTP_USER` | `gea.notificaciones@unilibrecucuta.edu.co` | Usuario/cuenta del servidor de correo. |
| `SMTP_PASS` | *(vacío)* | Contraseña SMTP (debe definirse para que el envío de correo funcione). |
| `NOTIF_RECIPIENTS` | *(vacío)* | Destinatarios de notificaciones de eventos universitarios (`university-recipients`). |
| `PUBLISHED_RECIPIENTS` | *(vacío)* | Destinatarios de notificaciones de contenido publicado (`published-recipients`). |
| `FRONTEND_URL` | `http://localhost:5173` | URL base del frontend, usada para componer enlaces en los correos (`app.frontend-base-url`). |
| `UPLOAD_DIR` | `uploads/` | Directorio de almacenamiento de archivos subidos (`file.upload-dir`). |
| `NOTIF_POOL_CORE` | `3` | *Core pool size* del executor asíncrono de notificaciones. |
| `NOTIF_POOL_MAX` | `6` | *Max pool size* del executor asíncrono de notificaciones. |
| `NOTIF_QUEUE_CAPACITY` | `30` | Capacidad de la cola del executor asíncrono de notificaciones. |
| `MANAGEMENT_PORT` | `8084` | Puerto del servidor de administración (Actuator). |

**Valores fijos relevantes** (no parametrizables por variable, pero útiles como referencia): `server.port=8083`, `server.address=0.0.0.0`, `server.servlet.context-path=/api`, `jwt.expiration=86400000` (24 h en ms), `spring.servlet.multipart.max-file-size=10MB`, extensiones permitidas `jpg,jpeg,png,pdf`, *rate limit* 60 peticiones/minuto.

### Frontend (`.env.local` / `.env.production`)

| Variable | Valor (local) | Valor (producción) | Descripción |
|----------|---------------|--------------------|-------------|
| `VITE_API_URL` | `http://localhost:8083` | `https://api.gea.tudominio.com` | Host+puerto del backend. El cliente Axios le añade el sufijo `/api`. *Fallback* en código: `http://localhost:8083`. |

### App móvil (Flutter `--dart-define`)

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `API_BASE_URL` | `http://172.46.12.57:8083/api` | URL **completa** de la API (incluye `/api`). Se define con `--dart-define=API_BASE_URL=...` al ejecutar o compilar. En dispositivo físico usar la IP de la PC; en producción, la URL pública del backend. |

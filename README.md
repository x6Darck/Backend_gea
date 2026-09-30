# GEA - Backend API

GEA (Gestión de Eventos y Anuncios) es una API REST robusta y segura desarrollada en **Java Spring Boot**, diseñada para administrar recursos, calendarios institucionales, reservas de oficinas, lugares físicos y anuncios públicos.

## 🚀 Arquitectura y Tecnologías
- **Java 21**
- **Spring Boot 3.2.x** (Web, Data JPA, Security, Mail)
- **MySQL 8.0+** (Almacenamiento Persistente)
- **Spring Security & JWT** (Autenticación y Autorización, Control de Roles)
- **Hibernate Envers** (Auditoría de datos)
- **Swagger / OpenAPI** (Documentación de API)
- **Maven** (Gestión de Dependencias)

## 📁 Estructura del Proyecto

El sistema está desarrollado bajo principios de **Clean Architecture** (Arquitectura por Capas):
```text
src/main/java/com/calendario/callapp/callapp_backend/
├── config/         # Configuraciones de Seguridad, CORS y Swagger
├── controller/     # Endpoints de la API REST
├── dto/            # Objetos de Transferencia de Datos y Mappers
├── entity/         # Entidades del Dominio (Persistencia)
├── exception/      # Manejo Global de Excepciones
├── repository/     # Interfaces JPA para acceso a DB
├── security/       # Filtros y validación de tokens JWT
└── service/        # Lógica de Negocio
```

## 🛠️ Instalación y Ejecución Local

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/x6Darck/GEA_BACKEND.git
   cd GEA_BACKEND
   ```

2. **Configurar el entorno:**
   Copia el archivo de plantilla para las variables de entorno y define tus credenciales reales (base de datos, correo, secreto JWT).
   ```bash
   cp .env.example .env
   ```
   Asegúrate de crear la base de datos `callapp_db` en MySQL.

3. **Compilar el proyecto:**
   ```bash
   ./mvnw clean install -DskipTests
   ```

4. **Ejecutar la API:**
   ```bash
   ./mvnw spring-boot:run
   ```
   La aplicación se ejecutará en `http://localhost:8083`.

## 🐳 Despliegue con Docker (servidor de producción)

El repo incluye `docker-compose.yml` con 3 servicios (`db`, `backend`,
`frontend`) listos para levantar el stack completo en un servidor único.

1. **Clonar ambos repositorios** como carpetas hermanas (o ajustar la ruta,
   ver paso 3):
   ```bash
   git clone <url-de-GEA_BACKEND> GEA_BACKEND
   git clone <url-de-GEA_FRONT> GEA_FRONT
   ```

2. **Crear el archivo de configuración real** a partir de la plantilla, y
   completar cada valor (contraseñas, dominio, secreto JWT, credenciales de
   correo — instrucciones dentro del propio archivo):
   ```bash
   cd GEA_BACKEND
   cp .env.example .env
   ```
   `.env` nunca se sube a git (ya está en `.gitignore`); `.env.example` sí
   se versiona como plantilla.

3. Si `GEA_FRONT` no queda como carpeta hermana de `GEA_BACKEND`, ajustar
   `FRONTEND_REPO_PATH` dentro de `.env`.

4. **Colocar el certificado TLS real** del dominio (Let's Encrypt o CA de
   la universidad) en `deploy/certs/fullchain.pem` y `deploy/certs/privkey.pem`
   (carpeta fuera de git).

5. **Levantar el stack:**
   ```bash
   docker compose up -d --build
   ```
   Verificar que los 3 contenedores queden `Up`/`healthy`:
   ```bash
   docker compose ps
   ```

Ver también `deploy/backup/README.md` (backups de la base de datos) y el
plan completo de migración en
`docs/superpowers/plans/2026-07-01-plan-migracion-nube-servidor-unico.md`.

### ¿El servidor no tiene Docker?

Si el servidor de destino corre Linux nativo sin Docker (MySQL, Nginx y Java
instalados directo en el sistema operativo), usar en cambio la guía paso a
paso de `deploy/native/README.md` — instala el backend como servicio de
`systemd`, el frontend como build estático servido por Nginx nativo, y
adapta el tuning de MySQL y el backup a ese escenario.

## 🛡️ Seguridad y Buenas Prácticas
- **Autenticación Basada en Tokens**: Todo endpoint privado requiere el envío de un token JWT válido en el header `Authorization: Bearer <token>`.
- **Excepciones Globales**: Un `ControllerAdvice` maneja los errores, retornando siempre objetos JSON consistentes.
- **Auditoría Activa**: Entidades críticas mantienen un registro histórico mediante *Hibernate Envers*.
- **No Hardcoding**: Toda credencial sensible se administra mediante el sistema de configuración del entorno (`application.properties` enlazado a variables de entorno).

## 📄 Documentación API
Una vez iniciada la aplicación, la documentación interactiva Swagger está disponible en:
- `http://localhost:8083/api/swagger-ui.html`

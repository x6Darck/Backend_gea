````markdown
# GEA - Backend API

GEA (Gestión de Eventos y Anuncios) es una plataforma institucional para la gestión de eventos, calendarios, espacios físicos, reservas y anuncios públicos.

Este repositorio contiene el **backend de GEA**, desarrollado como una API REST con **Java y Spring Boot**, encargada de centralizar la lógica de negocio, persistencia de datos, autenticación, autorización y comunicación con las aplicaciones web y móvil.

GEA fue desarrollado como un **proyecto real para una institución universitaria**, siendo este backend, junto con las aplicaciones web y móvil, diseñado, estructurado y programado individualmente.

---

## 🚀 Características Principales

- **Gestión de eventos:** creación, consulta, actualización y administración del calendario institucional.
- **Gestión de anuncios:** administración y publicación de información institucional.
- **Gestión de espacios físicos:** administración de lugares y recursos disponibles.
- **Gestión de reservas:** manejo de solicitudes y reservas de espacios.
- **Autenticación segura:** autenticación mediante JWT.
- **Autorización basada en roles:** control de acceso a los diferentes recursos de la API.
- **Auditoría de datos:** registro histórico de cambios mediante Hibernate Envers.
- **Validación de datos:** validación de solicitudes mediante DTOs y Bean Validation.
- **Manejo global de excepciones:** respuestas de error estandarizadas mediante `ControllerAdvice`.
- **Envío de correos:** integración con servicios SMTP mediante Spring Mail.
- **Documentación de API:** documentación interactiva mediante Swagger / OpenAPI.
- **Configuración mediante variables de entorno:** separación de credenciales y configuraciones sensibles del código fuente.
- **Contenerización:** configuración para despliegue mediante Docker y Docker Compose.

---

## 🏗️ Arquitectura

El backend utiliza una **arquitectura por capas**, separando las responsabilidades principales de la aplicación.

Esta estructura permite mantener una separación clara entre la exposición de los endpoints, la lógica de negocio y el acceso a los datos.

```text
src/main/java/com/calendario/callapp/callapp_backend/

├── config/
│   └── Configuración de seguridad, CORS, Swagger y aplicación
│
├── controller/
│   └── Endpoints y controladores REST
│
├── dto/
│   └── Objetos de transferencia de datos y mappers
│
├── entity/
│   └── Entidades utilizadas para la persistencia
│
├── exception/
│   └── Excepciones y manejo global de errores
│
├── repository/
│   └── Interfaces de acceso a datos mediante Spring Data JPA
│
├── security/
│   └── Filtros JWT y componentes relacionados con seguridad
│
└── service/
    └── Lógica y reglas de negocio
````

### Flujo general de una solicitud

```text
Cliente
   │
   ▼
Controller
   │
   ▼
DTO / Validación
   │
   ▼
Service
   │
   ▼
Repository
   │
   ▼
MySQL
```

La autenticación y autorización se gestionan mediante **Spring Security y JWT** antes de permitir el acceso a los endpoints protegidos.

---

## 🧰 Stack Tecnológico

| Tecnología        | Uso                            |
| ----------------- | ------------------------------ |
| Java 21           | Lenguaje principal             |
| Spring Boot 3.2.x | Framework backend              |
| Spring Web        | Desarrollo de API REST         |
| Spring Data JPA   | Persistencia de datos          |
| Spring Security   | Autenticación y autorización   |
| JWT               | Autenticación basada en tokens |
| MySQL 8.0+        | Base de datos                  |
| Hibernate Envers  | Auditoría de cambios           |
| Spring Mail       | Envío de correos               |
| Swagger / OpenAPI | Documentación de la API        |
| Maven             | Gestión de dependencias        |
| Docker            | Contenerización y despliegue   |

---

## 🔐 Seguridad

La API utiliza **Spring Security** y **JSON Web Tokens (JWT)** para proteger los recursos privados.

El flujo de autenticación es:

```text
Usuario
   │
   ▼
POST /auth/login
   │
   ▼
Validación de credenciales
   │
   ▼
Generación de JWT
   │
   ▼
Authorization: Bearer <token>
   │
   ▼
JWT Filter
   │
   ▼
Endpoint protegido
```

### Medidas implementadas

* Autenticación mediante JWT.
* Autorización basada en roles.
* Protección de endpoints mediante Spring Security.
* Aplicación stateless.
* Validación de tokens mediante filtros.
* Validación de datos de entrada.
* Manejo global de excepciones.
* Variables de entorno para información sensible.
* Auditoría de operaciones mediante Hibernate Envers.

Las credenciales, secretos JWT y demás información sensible se gestionan mediante variables de entorno y **no deben almacenarse directamente en el código fuente**.

---

## 📚 Documentación de la API

La API cuenta con documentación interactiva mediante **Swagger / OpenAPI**.

Una vez iniciada la aplicación localmente, puede accederse a:

```text
http://localhost:8083/api/swagger-ui.html
```

Desde Swagger es posible consultar la documentación de los endpoints disponibles y realizar pruebas sobre la API.

---

## 📋 Requisitos

Para ejecutar el proyecto localmente se requiere:

* Java 21
* MySQL 8.0 o superior
* Git
* Maven

Para el despliegue mediante contenedores:

* Docker
* Docker Compose

---

## ⚙️ Instalación y Ejecución Local

### 1. Clonar el repositorio

```bash
git clone <https://github.com/x6Darck/Backend_gea.git>
cd Backend_gea
```

### 2. Configurar las variables de entorno

El proyecto utiliza un archivo `.env.example` como plantilla de configuración.

Copia la plantilla:

#### Linux / macOS

```bash
cp .env.example .env
```

#### Windows PowerShell

```powershell
Copy-Item .env.example .env
```

Completa las variables necesarias para:

* Base de datos.
* Usuario y contraseña de MySQL.
* Secreto utilizado para JWT.
* Configuración SMTP.
* Configuraciones específicas del entorno.

> **Importante:** el archivo `.env` contiene información sensible y no debe subirse al repositorio.

---

### 3. Crear la base de datos

Crear la base de datos en MySQL:

```sql
CREATE DATABASE callapp_db;
```

---

### 4. Compilar el proyecto

#### Linux / macOS

```bash
./mvnw clean install -DskipTests
```

#### Windows

```powershell
.\mvnw.cmd clean install -DskipTests
```

---

### 5. Ejecutar la aplicación

#### Linux / macOS

```bash
./mvnw spring-boot:run
```

#### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

La API estará disponible en:

```text
http://localhost:8083
```

---

## 🐳 Despliegue con Docker

El proyecto incluye configuración mediante **Docker Compose** para facilitar el despliegue del ecosistema GEA.

La infraestructura contempla los siguientes servicios:

```text
                 ┌─────────────────┐
                 │     Frontend    │
                 │      React      │
                 └────────┬────────┘
                          │
                          ▼
                 ┌─────────────────┐
                 │     Backend     │
                 │  Spring Boot   │
                 └────────┬────────┘
                          │
                          ▼
                 ┌─────────────────┐
                 │      MySQL      │
                 │    Database     │
                 └─────────────────┘
```

Para construir y levantar los servicios:

```bash
docker compose up -d --build
```

Para verificar el estado:

```bash
docker compose ps
```

---

## 🗂️ Estructura del Proyecto

```text
GEA_BACKEND/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/calendario/callapp/
│   │   │
│   │   └── resources/
│   │
│   └── test/
│
├── deploy/
│   ├── backup/
│   └── native/
│
├── docs/
│
├── .env.example
├── .gitignore
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 🧪 Pruebas y Calidad

El proyecto utiliza las herramientas de testing proporcionadas por el ecosistema Spring para validar el comportamiento de la aplicación.

Para ejecutar las pruebas:

### Linux / macOS

```bash
./mvnw test
```

### Windows

```powershell
.\mvnw.cmd test
```

---

## 🔗 Ecosistema GEA

El backend forma parte de un ecosistema compuesto por tres aplicaciones principales:

### 🖥️ GEA Frontend

Plataforma web desarrollada en **React** para la administración institucional de eventos, calendarios, espacios físicos y anuncios.

**Tecnologías principales:**

* React 19
* Vite
* Shadcn/UI
* Lucide Icons
* Axios
* React Router DOM

Repositorio:

```text
https://github.com/x6Darck/Front_gea
```

---

### 📱 GEA Mobile

Aplicación móvil desarrollada en **Flutter** para la consulta de eventos, calendario y anuncios institucionales.

La aplicación utiliza **Clean Architecture**, Riverpod para la gestión de estado y comunicación con el backend mediante Dio.

**Tecnologías principales:**

* Flutter
* Dart
* Riverpod
* Dio
* GoRouter
* Flutter Secure Storage
* Shared Preferences

Repositorio:

```text
https://github.com/x6Darck/Movil_gea
```

---

### 🔄 Comunicación entre aplicaciones

```text
                    ┌───────────────────┐
                    │   GEA Frontend    │
                    │      React        │
                    └─────────┬─────────┘
                              │
                              │ REST API
                              │
                    ┌─────────▼─────────┐
                    │                   │
                    │    GEA Backend    │
                    │   Spring Boot     │
                    │                   │
                    └─────────┬─────────┘
                              │
                              │ JPA
                              │
                    ┌─────────▼─────────┐
                    │       MySQL       │
                    └───────────────────┘
                              ▲
                              │
                              │ REST API
                              │
                    ┌─────────┴─────────┐
                    │                   │
                    │    GEA Mobile     │
                    │      Flutter      │
                    │                   │
                    └───────────────────┘
```

El backend funciona como punto central de comunicación entre las diferentes aplicaciones del ecosistema GEA.

---

## 🌐 Entornos

La aplicación está preparada para trabajar con diferentes configuraciones de entorno.

Las aplicaciones cliente pueden apuntar a diferentes instancias del backend dependiendo del entorno utilizado:

```text
Desarrollo
     │
     ▼
GEA Backend
     │
     ├── GEA Frontend
     │
     └── GEA Mobile


Pruebas
     │
     ▼
GEA Backend
     │
     ├── GEA Frontend
     │
     └── GEA Mobile


Producción
     │
     ▼
GEA Backend
     │
     ├── GEA Frontend
     │
     └── GEA Mobile
```

Las configuraciones específicas de cada entorno se mantienen fuera del repositorio mediante variables y archivos de configuración no versionados.

---

## 📌 Estado del Proyecto

**Estado: Completado**

GEA fue desarrollado como un proyecto real para una institución universitaria, contemplando el desarrollo de su backend, plataforma web y aplicación móvil.

---

## 👨‍💻 Desarrollo

GEA fue **diseñado, estructurado y desarrollado individualmente**, incluyendo:

* Arquitectura del sistema.
* Diseño y desarrollo del backend.
* Diseño y desarrollo de la plataforma web.
* Desarrollo de la aplicación móvil.
* Diseño de la base de datos.
* Implementación de autenticación y autorización.
* Integración entre las diferentes aplicaciones.
* Documentación técnica.
* Configuración de despliegue.

El proyecto fue desarrollado con un enfoque orientado a la mantenibilidad, seguridad, escalabilidad y separación de responsabilidades.

---

## 📄 Propiedad y Uso

GEA es un proyecto desarrollado para una institución universitaria como parte de un proyecto real de desarrollo de software.

Este repositorio se presenta con fines demostrativos y de **portafolio profesional**. La publicación del proyecto no implica la transferencia de derechos de propiedad intelectual ni autorización para copiar, modificar, distribuir o utilizar el software con fines comerciales.

Los derechos sobre el proyecto, sus componentes y materiales asociados corresponden a las partes que hayan sido establecidas en los acuerdos del proyecto.

El repositorio no incluye:

* Credenciales.
* Contraseñas.
* Datos personales.
* Información sensible.
* Configuraciones privadas.
* Certificados de producción.
* Secretos de autenticación.

---

## 👤 Autor

**Jean Pier Gómez**

Desarrollo individual del ecosistema GEA.

---

> GEA — Gestión de Eventos y Anuncios
>
> Sistema institucional desarrollado con Java, Spring Boot, React y Flutter.

````


Y hay otra cosa que haría: **no pondría los diagramas ASCII de arquitectura como elemento principal si tienes capturas reales del sistema**. Para un reclutador, una portada con una captura de GEA + una explicación corta + stack tecnológico suele comunicar mucho más rápidamente que varias páginas de texto.

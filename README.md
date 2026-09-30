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

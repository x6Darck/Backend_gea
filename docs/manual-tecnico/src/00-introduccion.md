<div class="cover">
<h1>Manual Técnico</h1>
<p class="subtitle">Sistema GEA — Gestión de Eventos y Anuncios<br>Universidad Libre, Seccional Cúcuta</p>
<p class="subtitle">Versión 1.0 · 2026</p>
</div>

# 0. Introducción

## 0.1 Propósito de este manual

Este manual técnico es la referencia integral de la plataforma **GEA (Gestión de Eventos y Anuncios)** de la Universidad Libre, Seccional Cúcuta. Está dirigido a **desarrolladores y personal técnico** que se incorporan al proyecto y necesitan comprender, mantener, extender o desplegar el sistema sin contar con conocimiento previo de su historia o de las decisiones de diseño tomadas.

El documento cubre **de forma exhaustiva los tres sistemas** que componen la plataforma: el **backend** (API REST en Spring Boot), el **frontend web** (panel administrativo en React) y la **aplicación móvil** (cliente en Flutter). Para cada uno se describe su estructura de código, sus dependencias, su modelo de datos, los flujos de negocio que implementa y los procedimientos de arranque, configuración y despliegue. También se documenta la **integración entre sistemas** y con los servicios externos (correo institucional, notificaciones push y autenticación corporativa).

El manual está organizado en partes secuenciales, pensadas para leerse en orden por un perfil nuevo pero también para consultarse de manera puntual:

| Parte | Contenido |
|-------|-----------|
| 0. Introducción | Visión general, arquitectura global, stack tecnológico y glosario. |
| 1. Arranque | Requisitos previos y puesta en marcha del entorno de desarrollo. |
| 2. Backend | Estructura, capas, seguridad, modelo de datos y API del backend Spring Boot. |
| 3. Frontend | Panel administrativo web en React. |
| 4. App Flutter | Aplicación móvil, arquitectura limpia y manejo de estado con Riverpod. |
| 5. Integración | Comunicación entre sistemas y servicios externos. |
| 6. Despliegue | Procedimientos de build y publicación de cada sistema. |
| 7. Troubleshooting | Diagnóstico de problemas frecuentes. |
| 8. Anexos | Material de referencia complementario. |

## 0.2 ¿Qué es GEA?

GEA es una **plataforma institucional para la gestión, aprobación y publicación de eventos y anuncios** de la Universidad Libre, Seccional Cúcuta. Su objetivo es centralizar el ciclo de vida de toda comunicación institucional —desde que una dependencia la solicita hasta que se publica y se difunde a la comunidad universitaria— sustituyendo procesos manuales y dispersos por un flujo controlado, auditable y con trazabilidad.

Funcionalmente, GEA ofrece:

- **Calendario público de eventos**, accesible desde la aplicación móvil y desde el navegador, que muestra los eventos ya publicados de la institución.
- **Difusión de anuncios** institucionales (comunicados, noticias, piezas gráficas) hacia los usuarios de la app.
- **Flujo de aprobación multi-rol**: las **oficinas** (dependencias académicas y administrativas) crean *solicitudes* de eventos o anuncios; el área de **Comunicaciones** y los **administradores** las revisan, aprueban o rechazan y, finalmente, las **publican**. Cada solicitud transita por estados bien definidos (**PENDIENTE → APROBADA → PUBLICADA**, o **RECHAZADA**), lo que garantiza control editorial y separación de responsabilidades.
- **Notificaciones** automáticas por correo electrónico institucional y notificaciones *push* a los dispositivos móviles cuando se publican contenidos de interés.
- **Reportes** sobre la actividad de la plataforma (solicitudes, publicaciones, eventos por oficina, etc.), exportables a PDF y Excel.
- **Auditoría** de los cambios sobre las entidades del dominio, registrada de forma histórica.

Los **usuarios** de la plataforma se agrupan en perfiles con distintos niveles de acceso. Los roles administrativos (**SUPER_ADMIN**, **ADMIN**, **COMUNICACIONES**, **OFICINA**) operan sobre el panel web; los usuarios finales de la app pueden ser anónimos (**USUARIO_APP**) o autenticados (**USUARIO_AUTENTICADO_APP**) cuando inician sesión con sus credenciales institucionales. La gestión de eventos, anuncios, oficinas y lugares físicos se realiza desde el panel administrativo, mientras que la app móvil es principalmente un cliente de consumo y consulta para la comunidad universitaria.

## 0.3 Arquitectura global

GEA está construido como un sistema **cliente-servidor de tres capas** con un backend central y múltiples clientes. El **backend Spring Boot** es el núcleo: expone una **API REST** sobre HTTP/HTTPS y concentra toda la lógica de negocio, la seguridad y el acceso a datos. Es el único componente que se comunica con la base de datos y con los servicios externos; los clientes nunca acceden directamente a la persistencia.

Existen tres tipos de **clientes** que consumen la misma API:

- El **panel web en React**, usado por los roles administrativos para gestionar el contenido y los catálogos.
- La **aplicación móvil en Flutter** (Android/iOS), orientada a la comunidad universitaria.
- El acceso vía **navegador (PWA)**, que reutiliza la capa web para consulta del calendario y anuncios públicos.

Toda la comunicación entre clientes y backend se realiza mediante **peticiones REST sobre HTTP/HTTPS**, autenticadas con **JSON Web Tokens (JWT)**. El backend escucha en el puerto **8083** con el *context-path* **/api**, de modo que todos los recursos cuelgan de la ruta base `http://<host>:8083/api`.

La persistencia se apoya en una **base de datos MySQL central** (`gea`), accedida mediante JPA/Hibernate con un pool de conexiones HikariCP y versionada con migraciones Flyway. El historial de cambios se conserva mediante Hibernate Envers (tablas de auditoría con sufijo `_audit`).

El backend se integra además con **tres servicios externos**:

- **SMTP institucional** — envío de correos de notificación a oficinas y destinatarios configurados.
- **Firebase Cloud Messaging (FCM)** — entrega de notificaciones *push* a los dispositivos móviles registrados.
- **Microsoft Azure AD** — autenticación corporativa (SSO) para el inicio de sesión institucional de usuarios autenticados.

La Figura 1 resume esta arquitectura y las relaciones entre componentes.

![Figura 1: Arquitectura global del sistema GEA](../assets/diagrams/arquitectura-global.svg)

<p class="figure-caption">Figura 1: Arquitectura global del sistema GEA</p>

## 0.4 Stack tecnológico

Las tablas siguientes detallan, por sistema, las tecnologías principales con su versión y propósito. Las versiones provienen de los archivos de configuración reales de cada repositorio (`pom.xml`, `package.json` y `pubspec.yaml`).

### Backend (Spring Boot)

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 21 | Lenguaje y plataforma de ejecución. |
| Spring Boot | 3.4.3 | Framework base (web, seguridad, JPA, mail, cache, actuator). |
| Spring Security + OAuth2 Resource Server | 3.4.3 | Autenticación/autorización y validación de tokens. |
| JJWT | 0.11.5 | Generación y verificación de JSON Web Tokens. |
| Spring Data JPA / Hibernate | 3.4.3 | Mapeo objeto-relacional y acceso a datos. |
| Hibernate Envers | 3.4.3 | Auditoría histórica de entidades. |
| MySQL Connector/J | runtime | Conector JDBC a la base de datos MySQL. |
| Flyway (core + mysql) | gestionado por Spring Boot | Migraciones versionadas de esquema. |
| HikariCP | gestionado por Spring Boot | Pool de conexiones a base de datos. |
| Caffeine | gestionado por Spring Boot | Caché en memoria. |
| Firebase Admin SDK | 9.2.0 | Envío de notificaciones push (FCM). |
| MapStruct | 1.5.5.Final | Mapeo entre entidades y DTOs. |
| Lombok | 1.18.34 | Reducción de código repetitivo (boilerplate). |
| springdoc-openapi | 2.6.0 | Documentación interactiva de la API (Swagger UI). |
| OpenPDF / Apache POI / JFreeChart | 1.3.39 / 5.3.0 / 1.5.5 | Generación de reportes (PDF, Excel y gráficos). |

### Frontend web (React)

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| React / React DOM | 19.2.4 | Librería de interfaz de usuario. |
| Vite | 8.0.1 | Empaquetador y servidor de desarrollo. |
| React Router DOM | 7.13.1 | Enrutamiento del panel administrativo. |
| Axios | 1.13.6 | Cliente HTTP para consumir la API REST. |
| Recharts | 3.8.1 | Gráficos y visualización de reportes. |
| Lucide React | 0.577.0 | Iconografía. |
| React Toastify | 11.0.5 | Notificaciones en interfaz. |
| ESLint | 9.39.4 | Análisis estático y calidad de código. |

### App móvil (Flutter)

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Flutter / Dart SDK | Dart ^3.11.5 | Framework y lenguaje de la app multiplataforma. |
| flutter_riverpod / riverpod_annotation | 2.5.1 / 2.3.5 | Manejo de estado y lógica de presentación. |
| go_router | 13.2.0 | Navegación declarativa. |
| dio | 5.4.1 | Cliente HTTP para la API REST. |
| flutter_secure_storage | 9.0.0 | Almacenamiento seguro de tokens. |
| shared_preferences | 2.2.2 | Persistencia local de preferencias. |
| aad_oauth | 1.0.1 | Inicio de sesión con Microsoft Azure AD. |
| connectivity_plus | 6.1.0 | Detección de conectividad (modo offline). |
| table_calendar | 3.1.0 | Vista de calendario de eventos. |
| intl | 0.20.2 | Internacionalización y formato de fechas. |
| fpdart / equatable | 0.6.0 / 2.0.5 | Programación funcional y comparación de valores. |
| cached_network_image / flutter_svg | 3.4.1 / 2.0.10 | Carga y renderizado de imágenes y SVG. |

## 0.5 Glosario

| Término | Definición |
|---------|------------|
| **Solicitud** | Petición creada por una oficina para publicar un evento o un anuncio. Es el punto de entrada del flujo de aprobación y atraviesa los estados PENDIENTE, APROBADA, RECHAZADA o PUBLICADA. Se materializa en las entidades `SolicitudEvento` y `SolicitudAnuncio`. |
| **Publicación** | Contenido ya aprobado y difundido a la comunidad. Resulta de publicar una solicitud aprobada. Se materializa en `PublicacionEvento` y `PublicacionAnuncio`. |
| **Evento** | Actividad institucional con fecha, lugar y participantes (charla, ceremonia, taller, etc.) que se muestra en el calendario público una vez publicada. |
| **Anuncio** | Comunicado o noticia institucional difundida a los usuarios de la app, habitualmente acompañado de una pieza gráfica. |
| **Oficina** | Dependencia académica o administrativa de la universidad que origina solicitudes. Entidad `Oficina`; rol asociado **OFICINA**. |
| **Pieza Gráfica** | Recurso visual (imagen) que acompaña un anuncio o evento; gestionado como archivo adjunto (`ArchivoAdjunto`). |
| **Lugar Físico** | Espacio donde se realiza un evento (auditorio, aula, etc.). Entidad `LugarFisico`. |
| **SIAPAC** | Sistema institucional de información académico-administrativo de la Universidad Libre con el que se relacionan datos de usuarios y dependencias. |
| **Rol** | Perfil de autorización del usuario. Valores: SUPER_ADMIN, ADMIN, COMUNICACIONES, OFICINA, USUARIO_APP, USUARIO_AUTENTICADO_APP (y USUARIO como rol base). Enum `Rol`. |
| **Estado** | Fase del ciclo de vida de una solicitud (enum `EstadoSolicitud`): **PENDIENTE** (recién creada, a la espera de revisión), **APROBADA** (validada, lista para publicar), **PUBLICADA** (visible para la comunidad) y **RECHAZADA** (descartada por el revisor). |
| **JWT** (JSON Web Token) | Token firmado que transporta la identidad y los permisos del usuario en cada petición a la API; base de la autenticación sin estado del backend. |
| **Riverpod** | Librería de manejo de estado de Flutter usada en la app para la capa de presentación (providers y notifiers). |
| **Clean Architecture** | Estilo arquitectónico que separa estrictamente UI, lógica de presentación y capa de datos/dominio. Es la arquitectura de referencia de la app móvil. |
| **DTO** (Data Transfer Object) | Objeto plano que transporta datos entre el cliente y la API, desacoplado de las entidades de persistencia. El mapeo entidad ↔ DTO se realiza con MapStruct. |
| **FCM** (Firebase Cloud Messaging) | Servicio de Google para el envío de notificaciones push a los dispositivos móviles registrados. |
| **Azure AD** | Microsoft Entra/Azure Active Directory; proveedor de identidad institucional usado para el inicio de sesión corporativo (SSO). |
| **Flyway** | Herramienta de migraciones versionadas que mantiene el esquema de la base de datos sincronizado y reproducible. |
| **Envers** | Módulo de Hibernate que registra el historial de cambios de las entidades en tablas de auditoría (sufijo `_audit`). |
| **Actuator** | Módulo de Spring Boot que expone endpoints operativos (health, info, metrics) para monitoreo del backend. |

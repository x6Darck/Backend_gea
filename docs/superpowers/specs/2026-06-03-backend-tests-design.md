# Expansión de Tests Backend
**Fecha:** 2026-06-03  
**Estado:** Aprobado

## Goal

Ampliar la suite de tests del backend más allá de los 2 smoke tests existentes. Cubrir los tres flujos más críticos: autenticación, ciclo de vida de eventos, e integridad del cache de usuarios.

## Approach

`@SpringBootTest` con H2 in-memory (ya configurado en `application-test.properties`). Los tests llaman directamente a los servicios — sin HTTP, sin MockMvc — lo cual es más rápido y menos frágil.

## Clases de test nuevas

### `AuthFlowTest`
`src/test/java/.../smoke/AuthFlowTest.java`

4 tests:
1. Login correcto → retorna token JWT válido
2. Login con contraseña incorrecta → `ResponseStatusException` 401
3. Login con correo no registrado → `ResponseStatusException` 401
4. Login con cuenta inactiva → `ResponseStatusException` 401

### `EventFlowTest`
`src/test/java/.../smoke/EventFlowTest.java`

3 tests sobre las entidades del repositorio (sin pasar por el servicio que requiere `Authentication`):
1. Crear `SolicitudEvento` con estado `PENDIENTE` → persiste correctamente
2. Cambiar estado a `APROBADA` → persiste y es recuperable
3. Cambiar estado a `PUBLICADA` → persiste y es recuperable

### `UserCacheTest`
`src/test/java/.../smoke/UserCacheTest.java`

3 tests que verifican el comportamiento del cache Caffeine:
1. Carga inicial de usuario → retorna correctamente desde BD
2. Segunda carga sin evicción → retorna desde cache (estado INACTIVO en BD, pero cache dice ACTIVO)
3. Después de evicción → retorna estado actualizado desde BD

## Setup común

Los tests crean datos de prueba directamente en H2 via repositorios. No dependen del `DataInitializer` ni del `OficinaDataSeeder`.

Cada clase usa `@Transactional` para rollback automático o `@BeforeEach` para limpiar el estado.

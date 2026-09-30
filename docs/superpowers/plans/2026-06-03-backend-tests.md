# Backend Tests Expansion — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement task-by-task.

**Goal:** Agregar 10 tests en 3 clases nuevas cubriendo autenticación, flujo de eventos y cache de usuarios.

**Tech Stack:** JUnit 5 · AssertJ · Spring Boot Test · H2 in-memory

---

## Ruta del repo
`C:\Users\Administrador\Documents\test\GEA_BACKEND`

## Package base de tests
`com.calendario.callapp.callapp_backend.smoke`

## application-test.properties existente
```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=VALUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false
jwt.secret=test_secret_minimo_32_caracteres_x123
app.notifications.email-enabled=false
app.security.rate-limit.enabled=false
management.server.port=0
spring.mail.host=localhost
spring.mail.port=25
jwt.expiration=86400000
```

---

## Task 1: `AuthFlowTest` — 4 tests de autenticación

**Archivo:** `src/test/java/com/calendario/callapp/callapp_backend/smoke/AuthFlowTest.java`

- [ ] **Step 1: Crear `AuthFlowTest.java`**

```java
package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.dto.request.AuthRequest;
import com.calendario.callapp.callapp_backend.dto.response.AuthResponse;
import com.calendario.callapp.callapp_backend.entity.Rol;
import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.security.JwtService;
import com.calendario.callapp.callapp_backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthFlowTest {

    @Autowired private AuthServiceImpl authService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private static final String TEST_EMAIL = "auth.test@gea.edu.co";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        // Limpiar usuario de prueba si ya existe de una ejecución anterior
        usuarioRepository.findByCorreo(TEST_EMAIL).ifPresent(usuarioRepository::delete);

        RolEntity rol = rolRepository.findByNombre("USUARIO_APP")
                .orElseGet(() -> {
                    RolEntity r = new RolEntity();
                    r.setNombre("USUARIO_APP");
                    return rolRepository.save(r);
                });

        Usuario usuario = new Usuario();
        usuario.setNombre("Test Auth");
        usuario.setCorreo(TEST_EMAIL);
        usuario.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        usuario.setRolEntity(rol);
        usuario.setEstado("ACTIVO");
        usuarioRepository.save(usuario);
    }

    @Test
    void login_con_credenciales_correctas_retorna_token_valido() {
        AuthRequest request = new AuthRequest();
        request.setCorreo(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(jwtService.isTokenValid(response.getToken())).isTrue();
        assertThat(jwtService.extractUsername(response.getToken())).isEqualTo(TEST_EMAIL);
    }

    @Test
    void login_con_password_incorrecto_lanza_401() {
        AuthRequest request = new AuthRequest();
        request.setCorreo(TEST_EMAIL);
        request.setPassword("contraseña_incorrecta");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void login_con_correo_no_registrado_lanza_401() {
        AuthRequest request = new AuthRequest();
        request.setCorreo("noexiste@gea.edu.co");
        request.setPassword(TEST_PASSWORD);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void login_con_cuenta_inactiva_lanza_401() {
        // Desactivar la cuenta
        usuarioRepository.findByCorreo(TEST_EMAIL).ifPresent(u -> {
            u.setEstado("INACTIVO");
            usuarioRepository.save(u);
        });

        AuthRequest request = new AuthRequest();
        request.setCorreo(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }
}
```

**NOTA:** Si `UsuarioRepository` no tiene `findByCorreo()`, buscar el método equivalente con `grep -n "findByCorreo\|getByCorreo" src/main/java/com/calendario/callapp/callapp_backend/repository/UsuarioRepository.java` y usar el que existe.

- [ ] **Step 2: Ejecutar**

```bash
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND"
mvn test -Dtest=AuthFlowTest -q 2>&1 | tail -10
```

Resultado esperado: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/calendario/callapp/callapp_backend/smoke/AuthFlowTest.java
git commit -m "test: add AuthFlowTest covering login success, wrong password, unknown user, inactive account"
```

---

## Task 2: `EventFlowTest` — 3 tests del ciclo de vida de solicitudes

**Archivo:** `src/test/java/com/calendario/callapp/callapp_backend/smoke/EventFlowTest.java`

- [ ] **Step 1: Crear `EventFlowTest.java`**

```java
package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import com.calendario.callapp.callapp_backend.entity.TipoEventoCatalogo;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.OficinaRepository;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.SolicitudEventoRepository;
import com.calendario.callapp.callapp_backend.repository.TipoEventoCatalogoRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventFlowTest {

    @Autowired private SolicitudEventoRepository solicitudEventoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OficinaRepository oficinaRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private TipoEventoCatalogoRepository tipoEventoCatalogoRepository;

    private Usuario testUser;
    private Oficina testOficina;
    private TipoEventoCatalogo testTipo;

    @BeforeEach
    void setUp() {
        RolEntity rol = rolRepository.findByNombre("OFICINA")
                .orElseGet(() -> {
                    RolEntity r = new RolEntity();
                    r.setNombre("OFICINA");
                    return rolRepository.save(r);
                });

        testOficina = new Oficina();
        testOficina.setNombre("Oficina Test");
        testOficina = oficinaRepository.save(testOficina);

        testUser = new Usuario();
        testUser.setNombre("Test User Evento");
        testUser.setCorreo("evento.test@gea.edu.co");
        testUser.setPassword("dummy");
        testUser.setRolEntity(rol);
        testUser.setOficina(testOficina);
        testUser.setEstado("ACTIVO");
        testUser = usuarioRepository.save(testUser);

        testTipo = new TipoEventoCatalogo();
        testTipo.setNombre("Conferencia");
        testTipo.setColorHex("#CE1126");
        testTipo = tipoEventoCatalogoRepository.save(testTipo);
    }

    @Test
    void solicitud_creada_tiene_estado_pendiente() {
        SolicitudEvento solicitud = buildSolicitud();
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        SolicitudEvento saved = solicitudEventoRepository.save(solicitud);

        SolicitudEvento found = solicitudEventoRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void solicitud_pendiente_puede_cambiar_a_aprobada() {
        SolicitudEvento solicitud = buildSolicitud();
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        SolicitudEvento saved = solicitudEventoRepository.save(solicitud);

        saved.setEstado(EstadoSolicitud.APROBADA);
        solicitudEventoRepository.save(saved);

        SolicitudEvento found = solicitudEventoRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
    }

    @Test
    void solicitud_aprobada_puede_cambiar_a_publicada() {
        SolicitudEvento solicitud = buildSolicitud();
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        SolicitudEvento saved = solicitudEventoRepository.save(solicitud);

        saved.setEstado(EstadoSolicitud.PUBLICADA);
        solicitudEventoRepository.save(saved);

        SolicitudEvento found = solicitudEventoRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEstado()).isEqualTo(EstadoSolicitud.PUBLICADA);
    }

    private SolicitudEvento buildSolicitud() {
        SolicitudEvento s = new SolicitudEvento();
        s.setNombreEvento("Evento de Prueba");
        s.setDescripcionEvento("Descripción de prueba");
        s.setFechaEvento(LocalDate.now().plusDays(7));
        s.setHoraInicio(LocalTime.of(9, 0));
        s.setHoraFin(LocalTime.of(11, 0));
        s.setOficina(testOficina);
        s.setUsuarioSolicitante(testUser);
        s.setTipoEventoCatalogo(testTipo);
        s.setEstado(EstadoSolicitud.PENDIENTE);
        return s;
    }
}
```

**NOTA:** Si `SolicitudEvento` o `TipoEventoCatalogo` tienen campos `@NotNull` adicionales que fallan en H2, el test fallará con `ConstraintViolationException`. En ese caso, leer la entidad y agregar los campos faltantes al método `buildSolicitud()`.

- [ ] **Step 2: Ejecutar**

```bash
mvn test -Dtest=EventFlowTest -q 2>&1 | tail -10
```

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/calendario/callapp/callapp_backend/smoke/EventFlowTest.java
git commit -m "test: add EventFlowTest covering PENDIENTE -> APROBADA -> PUBLICADA state transitions"
```

---

## Task 3: `UserCacheTest` — 3 tests del cache Caffeine

**Archivo:** `src/test/java/com/calendario/callapp/callapp_backend/smoke/UserCacheTest.java`

- [ ] **Step 1: Crear `UserCacheTest.java`**

```java
package com.calendario.callapp.callapp_backend.smoke;

import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserCacheTest {

    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private CacheManager cacheManager;

    private static final String CACHE_EMAIL = "cache.test@gea.edu.co";

    @BeforeEach
    void setUp() {
        // Limpiar cache y usuario de prueba antes de cada test
        var cache = cacheManager.getCache("userDetails");
        if (cache != null) cache.clear();
        usuarioRepository.findByCorreo(CACHE_EMAIL).ifPresent(usuarioRepository::delete);

        RolEntity rol = rolRepository.findByNombre("USUARIO_APP")
                .orElseGet(() -> {
                    RolEntity r = new RolEntity();
                    r.setNombre("USUARIO_APP");
                    return rolRepository.save(r);
                });

        Usuario u = new Usuario();
        u.setNombre("Cache Test User");
        u.setCorreo(CACHE_EMAIL);
        u.setPassword("dummy");
        u.setRolEntity(rol);
        u.setEstado("ACTIVO");
        usuarioRepository.save(u);
    }

    @Test
    void carga_inicial_de_usuario_retorna_datos_correctos() {
        UserDetails details = userDetailsService.loadUserByUsername(CACHE_EMAIL);

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo(CACHE_EMAIL);
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void segunda_carga_sin_eviccion_usa_cache_y_no_refleja_cambios_en_bd() {
        // Primera carga → llena el caché con estado ACTIVO
        userDetailsService.loadUserByUsername(CACHE_EMAIL);

        // Cambiar estado directamente en BD SIN pasar por el servicio (sin evicción de caché)
        usuarioRepository.findByCorreo(CACHE_EMAIL).ifPresent(u -> {
            u.setEstado("INACTIVO");
            usuarioRepository.save(u);
        });

        // Segunda carga → debe venir del caché (ACTIVO), no de la BD (INACTIVO)
        UserDetails details = userDetailsService.loadUserByUsername(CACHE_EMAIL);
        assertThat(details.isEnabled()).isTrue(); // caché dice ACTIVO
    }

    @Test
    void despues_de_eviccion_carga_refleja_estado_actualizado_en_bd() {
        // Primera carga → llena el caché con estado ACTIVO
        userDetailsService.loadUserByUsername(CACHE_EMAIL);

        // Cambiar estado en BD
        usuarioRepository.findByCorreo(CACHE_EMAIL).ifPresent(u -> {
            u.setEstado("INACTIVO");
            usuarioRepository.save(u);
        });

        // Evictar el caché explícitamente
        userDetailsService.invalidarCacheUsuario(CACHE_EMAIL);

        // Tercera carga → debe ir a la BD y reflejar INACTIVO
        UserDetails details = userDetailsService.loadUserByUsername(CACHE_EMAIL);
        assertThat(details.isEnabled()).isFalse(); // BD dice INACTIVO
    }
}
```

**NOTA:** Este test NO usa `@Transactional` a nivel de clase porque el cache funciona entre transacciones. Los `usuarioRepository.save()` deben hacer commit real para que el caché lo vea. Si hay problemas con `@Transactional` y caché, revisar la configuración.

- [ ] **Step 2: Ejecutar todos los tests**

```bash
cd "C:\Users\Administrador\Documents\test\GEA_BACKEND"
mvn test -Dtest="AuthFlowTest,EventFlowTest,UserCacheTest" -q 2>&1 | tail -15
```

Resultado esperado: `Tests run: 10, Failures: 0, Errors: 0`

- [ ] **Step 3: Commit final**

```bash
git add src/test/java/com/calendario/callapp/callapp_backend/smoke/UserCacheTest.java
git commit -m "test: add UserCacheTest verifying Caffeine cache behavior on state changes"
```

---

## Verificación final — toda la suite

```bash
mvn test -q 2>&1 | tail -5
```

Resultado esperado: `Tests run: 12, Failures: 0, Errors: 0` (10 nuevos + 2 existentes del AuthSmokeTest)

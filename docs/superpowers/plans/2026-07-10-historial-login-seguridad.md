# Historial de Login (Panel de Seguridad SuperAdmin) — Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dar a los SuperAdmin un panel donde consultar el historial de inicios de sesión (exitosos y fallidos, local y Microsoft) con IP, método, motivo de fallo y fecha, filtrable y con retención de 90 días.

**Architecture:** Tabla nueva `historial_login` (solo-inserción) que se llena desde la capa de servicio en cada intento de login, en una transacción independiente (`REQUIRES_NEW`) para que los fallos sobrevivan al rollback del login. La IP se resuelve con un componente compartido `ClientIpResolver` extraído de `RateLimiterFilter` (DRY, endurecido contra spoofing). Lectura vía endpoint admin-only paginado + página React nueva calcada del patrón de Reportes.

**Tech Stack:** Spring Boot 3.4 / Java 21, Hibernate/JPA, Flyway, Spring Security (`@PreAuthorize`), `@Scheduled`; React 19 + React Router 7 + Recharts-free (tabla simple), Vite.

**Spec:** `docs/superpowers/specs/2026-07-10-historial-login-seguridad-design.md`

---

## Estructura de archivos

**Backend (crear):**
- `src/main/resources/db/migration/V9__historial_login.sql` — tabla + índices
- `entity/HistorialLogin.java` — entidad standalone (no BaseEntity, no Envers)
- `entity/MotivoFalloLogin.java` — enum de motivos de fallo
- `repository/HistorialLoginRepository.java` — Spring Data + query de filtros paginada
- `util/ClientIpResolver.java` — resolución de IP compartida (extraída del filter)
- `service/impl/HistorialLoginService.java` — registrar éxito/fallo (REQUIRES_NEW) + purga @Scheduled
- `dto/response/HistorialLoginResponse.java` — DTO de lectura
- `controller/SeguridadController.java` — endpoint admin-only paginado
- Tests: `ClientIpResolverTest`, `HistorialLoginServiceTest`, `AuthHistorialLoginTest`, `HistorialLoginRetentionTest`

**Backend (modificar):**
- `security/RateLimiterFilter.java` — usar `ClientIpResolver` en vez de su método privado
- `service/impl/AuthServiceImpl.java` — registrar éxito + 2 ramas de fallo
- `service/impl/MicrosoftAuthServiceImpl.java` — registrar éxito + fallo

**Frontend (crear):**
- `src/services/seguridad.service.js` — cliente del endpoint
- `src/pages/SecurityPanel.jsx` + `SecurityPanel.module.css` — página del panel

**Frontend (modificar):**
- `src/App.jsx` — ruta `/seguridad` protegida SUPER_ADMIN
- `src/components/layout/Sidebar.jsx` — ítem de navegación SUPER_ADMIN

---

## Task 1: Migración Flyway — tabla `historial_login`

**Files:**
- Create: `src/main/resources/db/migration/V9__historial_login.sql`

- [ ] **Step 1: Escribir la migración**

```sql
-- Historial de intentos de inicio de sesión (exitosos y fallidos), para el
-- panel de seguridad de SuperAdmin. Tabla solo-inserción: nunca se actualiza,
-- una purga programada borra lo más antiguo de 90 días (ver HistorialLoginService).
CREATE TABLE historial_login (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    correo_intentado  VARCHAR(160) NOT NULL,
    id_usuario        BIGINT NULL,
    exito             BOOLEAN NOT NULL,
    metodo            VARCHAR(30) NOT NULL,
    motivo_fallo      VARCHAR(40) NULL,
    ip                VARCHAR(45) NULL,
    user_agent        VARCHAR(400) NULL,
    fecha             TIMESTAMP NOT NULL,
    CONSTRAINT fk_historial_login_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON DELETE SET NULL
);

-- fecha: ordena el panel (DESC) y filtra la purga por retención.
CREATE INDEX idx_historial_login_fecha ON historial_login (fecha);
-- filtros del panel:
CREATE INDEX idx_historial_login_usuario ON historial_login (id_usuario);
CREATE INDEX idx_historial_login_ip ON historial_login (ip);
```

- [ ] **Step 2: Verificar que Flyway aplica la migración al arrancar los tests**

Run: `./mvnw -q -Dtest=AuthFlowTest test`
Expected: PASS — la migración corre sin error en H2 al levantar el contexto (si el SQL tuviera un typo, el contexto de Spring fallaría al arrancar). Confirma que `V9` es compatible con H2 y MySQL.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V9__historial_login.sql
git commit -m "feat(seguridad): migracion tabla historial_login"
```

---

## Task 2: Enum `MotivoFalloLogin` y entidad `HistorialLogin`

**Files:**
- Create: `src/main/java/com/calendario/callapp/callapp_backend/entity/MotivoFalloLogin.java`
- Create: `src/main/java/com/calendario/callapp/callapp_backend/entity/HistorialLogin.java`

- [ ] **Step 1: Crear el enum de motivos de fallo**

```java
package com.calendario.callapp.callapp_backend.entity;

/**
 * Motivo por el que un intento de login fue rechazado. Se guarda solo en el
 * historial admin-only ({@link HistorialLogin}); al usuario final el login
 * sigue devolviendo un mensaje genérico (defensa anti-enumeración).
 */
public enum MotivoFalloLogin {
    /** El correo no corresponde a ningún usuario registrado. */
    CORREO_NO_REGISTRADO,
    /** El usuario existe pero su cuenta está inactiva. */
    CUENTA_INACTIVA,
    /** Contraseña incorrecta (login local). */
    CREDENCIALES_INVALIDAS,
    /** Token de Microsoft inválido, expirado o de otra audiencia. */
    TOKEN_INVALIDO,
    /** Cualquier otro fallo no clasificado. */
    OTRO
}
```

- [ ] **Step 2: Crear la entidad**

```java
package com.calendario.callapp.callapp_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Un intento de inicio de sesión (exitoso o fallido) registrado para el panel
 * de seguridad. Tabla {@code historial_login}: solo-inserción, nunca se
 * actualiza; por eso NO extiende {@link BaseEntity} ni se anota {@code @Audited}
 * (ya es en sí mismo un registro de auditoría).
 */
@Entity
@Table(name = "historial_login")
@Data
public class HistorialLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo tal como se escribió en el formulario de login. */
    @Column(name = "correo_intentado", nullable = false, length = 160)
    private String correoIntentado;

    /** Usuario real asociado, o null si el correo no existe. */
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(nullable = false)
    private Boolean exito;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthProvider metodo;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_fallo", length = 40)
    private MotivoFalloLogin motivoFallo;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 400)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
```

- [ ] **Step 3: Compilar**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/calendario/callapp/callapp_backend/entity/MotivoFalloLogin.java src/main/java/com/calendario/callapp/callapp_backend/entity/HistorialLogin.java
git commit -m "feat(seguridad): entidad HistorialLogin y enum MotivoFalloLogin"
```

---

## Task 3: Repositorio con query de filtros paginada

**Files:**
- Create: `src/main/java/com/calendario/callapp/callapp_backend/repository/HistorialLoginRepository.java`

- [ ] **Step 1: Crear el repositorio**

```java
package com.calendario.callapp.callapp_backend.repository;

import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface HistorialLoginRepository extends JpaRepository<HistorialLogin, Long> {

    /**
     * Busca registros aplicando solo los filtros no nulos, ordenados por fecha
     * descendente (más reciente primero). Todos los parámetros son opcionales:
     * un null desactiva ese filtro.
     */
    @Query("""
            SELECT h FROM HistorialLogin h
            WHERE (:correo IS NULL OR LOWER(h.correoIntentado) LIKE LOWER(CONCAT('%', :correo, '%')))
              AND (:ip IS NULL OR h.ip = :ip)
              AND (:exito IS NULL OR h.exito = :exito)
              AND (:desde IS NULL OR h.fecha >= :desde)
              AND (:hasta IS NULL OR h.fecha < :hasta)
            ORDER BY h.fecha DESC
            """)
    Page<HistorialLogin> buscar(@Param("correo") String correo,
                                @Param("ip") String ip,
                                @Param("exito") Boolean exito,
                                @Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta,
                                Pageable pageable);

    /** Borra los registros anteriores a la fecha de corte (purga por retención). */
    @Modifying
    @Query("DELETE FROM HistorialLogin h WHERE h.fecha < :corte")
    int deleteByFechaBefore(@Param("corte") LocalDateTime corte);
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/calendario/callapp/callapp_backend/repository/HistorialLoginRepository.java
git commit -m "feat(seguridad): repositorio HistorialLogin con filtros y purga"
```

---

## Task 4: `ClientIpResolver` compartido (extraído de RateLimiterFilter)

**Files:**
- Create: `src/main/java/com/calendario/callapp/callapp_backend/util/ClientIpResolver.java`
- Test: `src/test/java/com/calendario/callapp/callapp_backend/security/ClientIpResolverTest.java`
- Modify: `src/main/java/com/calendario/callapp/callapp_backend/security/RateLimiterFilter.java`

- [ ] **Step 1: Escribir el test del resolver**

```java
package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private ClientIpResolver resolverConProxy(boolean trustProxy) {
        ClientIpResolver r = new ClientIpResolver();
        ReflectionTestUtils.setField(r, "trustProxy", trustProxy);
        return r;
    }

    @Test
    void con_trust_proxy_toma_el_ultimo_salto_de_x_forwarded_for() {
        // El cliente puede falsear los saltos anteriores; Nginx agrega el real al FINAL.
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "1.1.1.1, 200.10.20.30");
        req.setRemoteAddr("127.0.0.1");

        assertThat(resolverConProxy(true).resolve((HttpServletRequest) req)).isEqualTo("200.10.20.30");
    }

    @Test
    void sin_trust_proxy_ignora_el_header_y_usa_remote_addr() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "1.1.1.1");
        req.setRemoteAddr("10.0.0.5");

        assertThat(resolverConProxy(false).resolve((HttpServletRequest) req)).isEqualTo("10.0.0.5");
    }

    @Test
    void con_x_forwarded_for_con_basura_cae_a_remote_addr() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "no-es-una-ip");
        req.setRemoteAddr("10.0.0.9");

        assertThat(resolverConProxy(true).resolve((HttpServletRequest) req)).isEqualTo("10.0.0.9");
    }
}
```

- [ ] **Step 2: Correr el test para verlo fallar**

Run: `./mvnw -q -Dtest=ClientIpResolverTest test`
Expected: FAIL de compilación — `ClientIpResolver` no existe todavía.

- [ ] **Step 3: Crear `ClientIpResolver` (misma lógica que ya tenía el filter)**

```java
package com.calendario.callapp.callapp_backend.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resuelve la IP real del cliente de forma endurecida contra spoofing. Única
 * implementación compartida entre el rate-limiter y el historial de login, para
 * que no puedan divergir (la lógica es sensible: de qué IP nos fiamos).
 *
 * <p>Solo confía en {@code X-Forwarded-For} cuando el backend está detrás de un
 * proxy reverso de confianza ({@code app.security.rate-limit.trust-proxy=true},
 * es decir Nginx). Nginx usa {@code $proxy_add_x_forwarded_for}, que SIEMPRE
 * agrega su propio remote_addr al FINAL de la cadena: por eso la posición
 * confiable es la última. Un cliente puede anteponer valores falsos, que Nginx
 * conserva intactos; tomar la primera posición permitiría falsificar la IP.</p>
 */
@Component
public class ClientIpResolver {

    @Value("${app.security.rate-limit.trust-proxy:false}")
    private boolean trustProxy;

    public String resolve(HttpServletRequest request) {
        if (trustProxy) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                String[] partes = xForwardedFor.split(",");
                String candidate = partes[partes.length - 1].trim();
                if (candidate.matches("^[0-9a-fA-F.:]+$") && candidate.length() <= 45) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
```

- [ ] **Step 4: Correr el test para verlo pasar**

Run: `./mvnw -q -Dtest=ClientIpResolverTest test`
Expected: PASS (3 tests).

- [ ] **Step 5: Refactorizar `RateLimiterFilter` para usar el resolver**

En `security/RateLimiterFilter.java`:
1. Inyectar el resolver. Si la clase usa constructor generado por Lombok (`@RequiredArgsConstructor`), agregar el campo `private final ClientIpResolver clientIpResolver;`. Si construye a mano, agregarlo al constructor.
2. Reemplazar la llamada interna a su método privado de IP por `clientIpResolver.resolve(request)`.
3. Borrar el método privado `resolveClientIp(...)` (o como se llame) y el campo `trustProxy` del filter — ahora viven en el resolver. Añadir el import `com.calendario.callapp.callapp_backend.util.ClientIpResolver`.

- [ ] **Step 6: Correr el test de regresión del filter + el del resolver**

Run: `./mvnw -q -Dtest=RateLimiterFilterTest,ClientIpResolverTest test`
Expected: PASS ambos — el comportamiento del rate-limiter no cambió (red de seguridad del refactor).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/calendario/callapp/callapp_backend/util/ClientIpResolver.java src/test/java/com/calendario/callapp/callapp_backend/security/ClientIpResolverTest.java src/main/java/com/calendario/callapp/callapp_backend/security/RateLimiterFilter.java
git commit -m "refactor(seguridad): extraer ClientIpResolver compartido del rate-limiter"
```

---

## Task 5: `HistorialLoginService` — registrar éxito/fallo (REQUIRES_NEW)

**Files:**
- Create: `src/main/java/com/calendario/callapp/callapp_backend/service/impl/HistorialLoginService.java`
- Test: `src/test/java/com/calendario/callapp/callapp_backend/security/HistorialLoginServiceTest.java`

- [ ] **Step 1: Escribir el test del servicio**

```java
package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import com.calendario.callapp.callapp_backend.entity.MotivoFalloLogin;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import com.calendario.callapp.callapp_backend.service.impl.HistorialLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NO se anota @Transactional: registrar…() usa REQUIRES_NEW y commitea aparte;
 * un @Transactional de test haría rollback y ocultaría el commit real. Se limpia
 * la tabla a mano al inicio para aislamiento.
 */
@SpringBootTest
@ActiveProfiles("test")
class HistorialLoginServiceTest {

    @Autowired private HistorialLoginService service;
    @Autowired private HistorialLoginRepository repository;

    @Test
    void registrar_exito_persiste_fila_con_exito_true() {
        repository.deleteAll();
        String correo = "exito." + System.nanoTime() + "@gea.edu.co";

        service.registrarExito(correo, 42L, AuthProvider.LOCAL);

        List<HistorialLogin> filas = repository.findAll();
        assertThat(filas).hasSize(1);
        HistorialLogin h = filas.get(0);
        assertThat(h.getCorreoIntentado()).isEqualTo(correo);
        assertThat(h.getIdUsuario()).isEqualTo(42L);
        assertThat(h.getExito()).isTrue();
        assertThat(h.getMetodo()).isEqualTo(AuthProvider.LOCAL);
        assertThat(h.getMotivoFallo()).isNull();
        assertThat(h.getFecha()).isNotNull();
    }

    @Test
    void registrar_fallo_persiste_fila_con_motivo() {
        repository.deleteAll();
        String correo = "fallo." + System.nanoTime() + "@gea.edu.co";

        service.registrarFallo(correo, null, AuthProvider.LOCAL, MotivoFalloLogin.CREDENCIALES_INVALIDAS);

        List<HistorialLogin> filas = repository.findAll();
        assertThat(filas).hasSize(1);
        HistorialLogin h = filas.get(0);
        assertThat(h.getExito()).isFalse();
        assertThat(h.getMotivoFallo()).isEqualTo(MotivoFalloLogin.CREDENCIALES_INVALIDAS);
        assertThat(h.getIdUsuario()).isNull();
    }
}
```

- [ ] **Step 2: Correr el test para verlo fallar**

Run: `./mvnw -q -Dtest=HistorialLoginServiceTest test`
Expected: FAIL de compilación — `HistorialLoginService` no existe.

- [ ] **Step 3: Crear el servicio**

```java
package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import com.calendario.callapp.callapp_backend.entity.MotivoFalloLogin;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import com.calendario.callapp.callapp_backend.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Registra intentos de login (éxito/fallo) en {@link HistorialLogin} y purga los
 * antiguos por retención.
 *
 * <p>registrar…() usa {@code REQUIRES_NEW} a propósito: {@code AuthServiceImpl.login}
 * es {@code @Transactional(readOnly=true)} y LANZA una excepción en los fallos;
 * registrar en su misma transacción no persistiría (read-only + rollback). Una
 * transacción nueva e independiente commitea el registro pase lo que pase con el
 * login.</p>
 *
 * <p>La IP y el User-Agent se resuelven del request actual del hilo de forma
 * null-safe: si no hay request (ej. un test que llama directo al servicio) se
 * guardan null y el login sigue funcionando.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistorialLoginService {

    private final HistorialLoginRepository repository;
    private final ClientIpResolver clientIpResolver;

    /** Días de retención antes de la purga automática. */
    private static final int RETENCION_DIAS = 90;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExito(String correo, Long idUsuario, AuthProvider metodo) {
        guardar(correo, idUsuario, true, metodo, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(String correo, Long idUsuario, AuthProvider metodo, MotivoFalloLogin motivo) {
        guardar(correo, idUsuario, false, metodo, motivo);
    }

    private void guardar(String correo, Long idUsuario, boolean exito,
                         AuthProvider metodo, MotivoFalloLogin motivo) {
        try {
            HistorialLogin h = new HistorialLogin();
            h.setCorreoIntentado(correo != null ? correo : "");
            h.setIdUsuario(idUsuario);
            h.setExito(exito);
            h.setMetodo(metodo);
            h.setMotivoFallo(motivo);
            h.setFecha(LocalDateTime.now());

            HttpServletRequest req = requestActual();
            if (req != null) {
                h.setIp(clientIpResolver.resolve(req));
                String ua = req.getHeader("User-Agent");
                if (ua != null && ua.length() > 400) ua = ua.substring(0, 400);
                h.setUserAgent(ua);
            }
            repository.save(h);
        } catch (Exception e) {
            // El registro de auditoría nunca debe tumbar el login. Si falla, se
            // deja traza en el log de la app y se sigue.
            log.warn("No se pudo registrar el intento de login en historial: {}", e.getMessage());
        }
    }

    private HttpServletRequest requestActual() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return (attrs instanceof ServletRequestAttributes sra) ? sra.getRequest() : null;
    }

    /**
     * Purga diaria (03:30) de los registros anteriores a la retención. El
     * scheduling ya está habilitado en la app (ver TokenBlacklistService).
     */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgarAntiguos() {
        int borrados = repository.deleteByFechaBefore(LocalDateTime.now().minusDays(RETENCION_DIAS));
        if (borrados > 0) {
            log.info("Historial de login: purgados {} registros anteriores a {} días", borrados, RETENCION_DIAS);
        }
    }
}
```

- [ ] **Step 4: Correr el test para verlo pasar**

Run: `./mvnw -q -Dtest=HistorialLoginServiceTest test`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/calendario/callapp/callapp_backend/service/impl/HistorialLoginService.java src/test/java/com/calendario/callapp/callapp_backend/security/HistorialLoginServiceTest.java
git commit -m "feat(seguridad): HistorialLoginService con registro REQUIRES_NEW y purga programada"
```

---

## Task 6: Cablear el registro en el login local (`AuthServiceImpl`)

**Files:**
- Modify: `src/main/java/com/calendario/callapp/callapp_backend/service/impl/AuthServiceImpl.java`
- Test: `src/test/java/com/calendario/callapp/callapp_backend/security/AuthHistorialLoginTest.java`

> Contexto: `login()` (líneas ~35-73) tiene tres ramas de fallo (correo no existe,
> cuenta inactiva, contraseña incorrecta) que lanzan `ResponseStatusException`, y
> una rama de éxito que construye el `AuthResponse`. Hay que registrar en las cuatro.

- [ ] **Step 1: Escribir el test de integración del login + historial**

```java
package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.dto.request.AuthRequest;
import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.MotivoFalloLogin;
import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NO @Transactional: el registro usa REQUIRES_NEW y debe commitear de verdad;
 * limpieza manual del historial por test.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthHistorialLoginTest {

    @Autowired private AuthServiceImpl authService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HistorialLoginRepository historialRepository;

    private static final String PASS = "password123";
    private String correo;

    @BeforeEach
    void setUp() {
        historialRepository.deleteAll();
        correo = "hist.login." + System.nanoTime() + "@gea.edu.co";
        RolEntity rol = rolRepository.findByNombre("USUARIO_APP")
                .orElseGet(() -> { RolEntity r = new RolEntity(); r.setNombre("USUARIO_APP"); return rolRepository.save(r); });
        Usuario u = new Usuario();
        u.setNombre("Hist Login");
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode(PASS));
        u.setRolEntity(rol);
        u.setEstado("ACTIVO");
        u.setAuthProvider(AuthProvider.LOCAL);
        usuarioRepository.save(u);
    }

    @Test
    void login_exitoso_registra_fila_exito() {
        AuthRequest req = new AuthRequest();
        req.setCorreo(correo);
        req.setPassword(PASS);

        authService.login(req);

        var filas = historialRepository.findAll();
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getExito()).isTrue();
        assertThat(filas.get(0).getMetodo()).isEqualTo(AuthProvider.LOCAL);
        assertThat(filas.get(0).getIdUsuario()).isNotNull();
    }

    @Test
    void login_password_incorrecto_registra_fila_fallo() {
        AuthRequest req = new AuthRequest();
        req.setCorreo(correo);
        req.setPassword("mala");

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(RuntimeException.class);

        var filas = historialRepository.findAll();
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getExito()).isFalse();
        assertThat(filas.get(0).getMotivoFallo()).isEqualTo(MotivoFalloLogin.CREDENCIALES_INVALIDAS);
    }

    @Test
    void login_correo_inexistente_registra_fila_fallo() {
        AuthRequest req = new AuthRequest();
        req.setCorreo("no.existe." + System.nanoTime() + "@gea.edu.co");
        req.setPassword(PASS);

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(RuntimeException.class);

        var filas = historialRepository.findAll();
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getExito()).isFalse();
        assertThat(filas.get(0).getMotivoFallo()).isEqualTo(MotivoFalloLogin.CORREO_NO_REGISTRADO);
    }
}
```

- [ ] **Step 2: Correr el test para verlo fallar**

Run: `./mvnw -q -Dtest=AuthHistorialLoginTest test`
Expected: FAIL — hay 0 filas en el historial (el registro aún no está cableado).

- [ ] **Step 3: Inyectar el servicio y registrar en cada rama**

En `AuthServiceImpl.java`:
1. Añadir imports: `com.calendario.callapp.callapp_backend.entity.AuthProvider`, `MotivoFalloLogin`.
2. Añadir el campo (la clase usa `@RequiredArgsConstructor`): `private final HistorialLoginService historialLoginService;`
3. En la rama "correo no registrado" (antes del `throw`):
   ```java
   historialLoginService.registrarFallo(correo, null, AuthProvider.LOCAL, MotivoFalloLogin.CORREO_NO_REGISTRADO);
   ```
4. En la rama "cuenta inactiva" (antes del `throw`):
   ```java
   historialLoginService.registrarFallo(correo, usuario.getId(), AuthProvider.LOCAL, MotivoFalloLogin.CUENTA_INACTIVA);
   ```
5. En la rama "contraseña incorrecta" (antes del `throw`):
   ```java
   historialLoginService.registrarFallo(correo, usuario.getId(), AuthProvider.LOCAL, MotivoFalloLogin.CREDENCIALES_INVALIDAS);
   ```
6. Justo antes del `return AuthResponse.builder()...` (rama de éxito):
   ```java
   historialLoginService.registrarExito(correo, usuario.getId(), AuthProvider.LOCAL);
   ```

- [ ] **Step 4: Correr el test para verlo pasar**

Run: `./mvnw -q -Dtest=AuthHistorialLoginTest test`
Expected: PASS (3 tests).

- [ ] **Step 5: Verificar que no se rompió el login existente**

Run: `./mvnw -q -Dtest=AuthFlowTest test`
Expected: PASS — el registro de historial no cambia el comportamiento del login.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/calendario/callapp/callapp_backend/service/impl/AuthServiceImpl.java src/test/java/com/calendario/callapp/callapp_backend/security/AuthHistorialLoginTest.java
git commit -m "feat(seguridad): registrar intentos de login local en el historial"
```

---

## Task 7: Cablear el registro en el login Microsoft (`MicrosoftAuthServiceImpl`)

**Files:**
- Modify: `src/main/java/com/calendario/callapp/callapp_backend/service/impl/MicrosoftAuthServiceImpl.java`

> Este camino no tiene un test de servicio directo fácil (requiere validar un
> idToken real contra Microsoft), así que se cablea por analogía con el local y se
> cubre con la compilación + la suite completa. El registro es best-effort (el
> servicio ya captura sus propias excepciones internamente).

- [ ] **Step 1: Inyectar el servicio y registrar éxito**

En `MicrosoftAuthServiceImpl.java`:
1. Añadir imports: `com.calendario.callapp.callapp_backend.entity.AuthProvider` (si no está), `MotivoFalloLogin`.
2. Añadir el campo (la clase usa `@RequiredArgsConstructor`): `private final HistorialLoginService historialLoginService;`
3. Justo antes del `return AuthResponse.builder()...` final (rama de éxito, tras obtener `guardado`):
   ```java
   historialLoginService.registrarExito(guardado.getCorreo(), guardado.getId(), AuthProvider.MICROSOFT);
   ```

- [ ] **Step 2: Registrar los fallos de autenticación Microsoft**

En cada `throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ...)` del flujo de `autenticar(...)`/`autenticarConCodigo(...)` (token inválido, audiencia incorrecta, cuenta inactiva), añadir justo antes del throw el registro correspondiente. Para las ramas donde ya se conoce el correo:
   ```java
   historialLoginService.registrarFallo(correo, null, AuthProvider.MICROSOFT, MotivoFalloLogin.TOKEN_INVALIDO);
   ```
   Para la rama de cuenta inactiva (donde ya hay usuario):
   ```java
   historialLoginService.registrarFallo(correo, usuario.getId(), AuthProvider.MICROSOFT, MotivoFalloLogin.CUENTA_INACTIVA);
   ```
   Si en la rama de token inválido aún no se conoce el correo (no se pudo decodificar), pasar `"desconocido"` como correo:
   ```java
   historialLoginService.registrarFallo("desconocido", null, AuthProvider.MICROSOFT, MotivoFalloLogin.TOKEN_INVALIDO);
   ```

- [ ] **Step 3: Compilar y correr la suite de auth**

Run: `./mvnw -q -Dtest=AuthFlowTest,AuthSmokeTest,AuthHistorialLoginTest test`
Expected: PASS — el login Microsoft compila y los caminos locales siguen intactos.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/calendario/callapp/callapp_backend/service/impl/MicrosoftAuthServiceImpl.java
git commit -m "feat(seguridad): registrar intentos de login Microsoft en el historial"
```

---

## Task 8: DTO de respuesta + endpoint admin-only paginado

**Files:**
- Create: `src/main/java/com/calendario/callapp/callapp_backend/dto/response/HistorialLoginResponse.java`
- Create: `src/main/java/com/calendario/callapp/callapp_backend/controller/SeguridadController.java`
- Test: `src/test/java/com/calendario/callapp/callapp_backend/security/SeguridadControllerTest.java`

- [ ] **Step 1: Crear el DTO de respuesta**

```java
package com.calendario.callapp.callapp_backend.dto.response;

import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Vista de lectura de un registro de {@code historial_login} para el panel. */
@Data
@Builder
public class HistorialLoginResponse {

    private Long id;
    private String correoIntentado;
    private Long idUsuario;
    private Boolean exito;
    private String metodo;
    private String motivoFallo;
    private String ip;
    private String userAgent;
    private LocalDateTime fecha;

    public static HistorialLoginResponse desde(HistorialLogin h) {
        return HistorialLoginResponse.builder()
                .id(h.getId())
                .correoIntentado(h.getCorreoIntentado())
                .idUsuario(h.getIdUsuario())
                .exito(h.getExito())
                .metodo(h.getMetodo() != null ? h.getMetodo().name() : null)
                .motivoFallo(h.getMotivoFallo() != null ? h.getMotivoFallo().name() : null)
                .ip(h.getIp())
                .userAgent(h.getUserAgent())
                .fecha(h.getFecha())
                .build();
    }
}
```

- [ ] **Step 2: Escribir el test del controller (seguridad de acceso + filtro)**

```java
package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private HistorialLoginRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        HistorialLogin h = new HistorialLogin();
        h.setCorreoIntentado("visible.panel@gea.edu.co");
        h.setExito(true);
        h.setMetodo(AuthProvider.LOCAL);
        h.setIp("200.1.2.3");
        h.setFecha(LocalDateTime.now());
        repository.save(h);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void superadmin_puede_ver_el_historial() throws Exception {
        mockMvc.perform(get("/admin/seguridad/historial-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].correoIntentado").value("visible.panel@gea.edu.co"));
    }

    @Test
    @WithMockUser(roles = "COMUNICACIONES")
    void otro_rol_recibe_403() throws Exception {
        mockMvc.perform(get("/admin/seguridad/historial-login"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 3: Correr el test para verlo fallar**

Run: `./mvnw -q -Dtest=SeguridadControllerTest test`
Expected: FAIL — el endpoint no existe (404, no 200/403).

- [ ] **Step 4: Crear el controller**

```java
package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.response.HistorialLoginResponse;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Panel de seguridad (solo SuperAdmin): historial de inicios de sesión.
 * Reservado a SUPER_ADMIN — a diferencia de /admin/auditoria (que también deja
 * entrar a COMUNICACIONES), este expone IPs y por eso es más restringido.
 */
@RestController
@RequestMapping("/admin/seguridad")
@RequiredArgsConstructor
public class SeguridadController {

    private final HistorialLoginRepository historialLoginRepository;

    @GetMapping("/historial-login")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<HistorialLoginResponse>>> historialLogin(
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) Boolean exito,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        // Normalizar filtros vacíos a null, y las fechas a límites de día.
        String correoFiltro = (correo != null && !correo.isBlank()) ? correo.trim() : null;
        String ipFiltro = (ip != null && !ip.isBlank()) ? ip.trim() : null;
        LocalDateTime desdeDt = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDt = hasta != null ? hasta.plusDays(1).atStartOfDay() : null; // 'hasta' inclusivo

        int tam = Math.min(Math.max(size, 1), 100); // techo de 100 por página
        Page<HistorialLoginResponse> resultado = historialLoginRepository
                .buscar(correoFiltro, ipFiltro, exito, desdeDt, hastaDt, PageRequest.of(Math.max(page, 0), tam))
                .map(HistorialLoginResponse::desde);

        return ResponseEntity.ok(ApiResponse.success(resultado));
    }
}
```

- [ ] **Step 5: Correr el test para verlo pasar**

Run: `./mvnw -q -Dtest=SeguridadControllerTest test`
Expected: PASS (2 tests: SUPER_ADMIN 200, COMUNICACIONES 403).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/calendario/callapp/callapp_backend/dto/response/HistorialLoginResponse.java src/main/java/com/calendario/callapp/callapp_backend/controller/SeguridadController.java src/test/java/com/calendario/callapp/callapp_backend/security/SeguridadControllerTest.java
git commit -m "feat(seguridad): endpoint admin-only paginado del historial de login"
```

---

## Task 9: Test de retención (purga)

**Files:**
- Test: `src/test/java/com/calendario/callapp/callapp_backend/security/HistorialLoginRetentionTest.java`

- [ ] **Step 1: Escribir el test de purga**

```java
package com.calendario.callapp.callapp_backend.security;

import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.HistorialLogin;
import com.calendario.callapp.callapp_backend.repository.HistorialLoginRepository;
import com.calendario.callapp.callapp_backend.service.impl.HistorialLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HistorialLoginRetentionTest {

    @Autowired private HistorialLoginService service;
    @Autowired private HistorialLoginRepository repository;

    private HistorialLogin fila(LocalDateTime fecha) {
        HistorialLogin h = new HistorialLogin();
        h.setCorreoIntentado("ret@gea.edu.co");
        h.setExito(true);
        h.setMetodo(AuthProvider.LOCAL);
        h.setFecha(fecha);
        return h;
    }

    @Test
    void purga_borra_lo_mayor_a_90_dias_y_conserva_lo_reciente() {
        repository.deleteAll();
        repository.save(fila(LocalDateTime.now().minusDays(91))); // debe borrarse
        repository.save(fila(LocalDateTime.now().minusDays(10))); // debe quedar

        service.purgarAntiguos();

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getFecha()).isAfter(LocalDateTime.now().minusDays(90));
    }
}
```

- [ ] **Step 2: Correr el test para verlo pasar (el código de purga ya existe de Task 5)**

Run: `./mvnw -q -Dtest=HistorialLoginRetentionTest test`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/calendario/callapp/callapp_backend/security/HistorialLoginRetentionTest.java
git commit -m "test(seguridad): verificar purga por retencion de 90 dias"
```

---

## Task 10: Suite completa de backend (regresión)

- [ ] **Step 1: Correr toda la suite**

Run: `./mvnw test`
Expected: BUILD SUCCESS, 0 failures / 0 errors. Confirma que ninguna de las piezas nuevas ni el refactor de `RateLimiterFilter` rompió algo existente.

- [ ] **Step 2: Confirmar que no quedó el warning de N+1 ni "applying in memory"**

Run: `./mvnw -q -Dtest=SeguridadControllerTest test 2>&1 | grep -c "HHH90003004"`
Expected: `0` — el listado paginado no dispara la advertencia de paginación en memoria (no hay JOIN FETCH a colecciones aquí).

---

## Task 11: Servicio frontend (`seguridad.service.js`)

**Files:**
- Create: `src/services/seguridad.service.js`

> Revisar primero `src/services/reportes.service.js` para copiar exactamente el
> patrón de cliente HTTP del proyecto (instancia de axios, manejo de `skipGlobalError`,
> forma de pasar query params).

- [ ] **Step 1: Crear el servicio (ajustar el import de `api` al que use reportes.service.js)**

```js
/**
 * Cliente del panel de seguridad (SuperAdmin): historial de inicios de sesión.
 * Espeja el patrón de reportes.service.js.
 */
import api from './api';

/**
 * @param {Object} filtros - { correo, ip, exito, desde, hasta, page, size }
 * @returns {Promise<Object>} Page: { content, totalElements, totalPages, number, size }
 */
export const getHistorialLogin = async (filtros = {}) => {
  const params = {};
  if (filtros.correo) params.correo = filtros.correo;
  if (filtros.ip) params.ip = filtros.ip;
  if (filtros.exito !== undefined && filtros.exito !== '') params.exito = filtros.exito;
  if (filtros.desde) params.desde = filtros.desde;
  if (filtros.hasta) params.hasta = filtros.hasta;
  params.page = filtros.page ?? 0;
  params.size = filtros.size ?? 25;

  const res = await api.get('/admin/seguridad/historial-login', { params });
  // El backend envuelve en ApiResponse { data: Page }
  return res.data?.data ?? res.data;
};
```

- [ ] **Step 2: Verificar que el proyecto compila (Vite) sin romper imports**

Run: `npx eslint src/services/seguridad.service.js`
Expected: 0 errores (warnings preexistentes del repo son aceptables).

- [ ] **Step 3: Commit**

```bash
git add src/services/seguridad.service.js
git commit -m "feat(seguridad): cliente frontend del historial de login"
```

---

## Task 12: Página `SecurityPanel.jsx` + ruta + navegación

**Files:**
- Create: `src/pages/SecurityPanel.jsx`
- Create: `src/pages/SecurityPanel.module.css`
- Modify: `src/App.jsx`
- Modify: `src/components/layout/Sidebar.jsx`

> Revisar `src/pages/Reports.jsx` (la barra `filterToolbar`/`filterRow` y la tabla)
> y `Reports.module.css` para reutilizar las mismas clases y estética. No inventar
> estilos nuevos si una clase equivalente ya existe.

- [ ] **Step 1: Crear la página**

```jsx
/**
 * Panel de seguridad (solo SuperAdmin): historial de inicios de sesión al
 * sistema, con filtros (correo, IP, éxito/fallido, rango de fechas) y paginación
 * en servidor. Reutiliza el patrón visual de la página de Reportes.
 */
import React, { useState, useEffect, useContext, useCallback } from 'react';
import { Shield, Search, CheckCircle2, XCircle, ChevronLeft, ChevronRight } from 'lucide-react';
import styles from './SecurityPanel.module.css';
import { getHistorialLogin } from '../services/seguridad.service';
import Spinner from '../components/ui/Spinner';
import { AuthContext } from '../context/AuthContext';

const METODO_LABEL = { LOCAL: 'Correo', MICROSOFT: 'Microsoft' };
const MOTIVO_LABEL = {
  CORREO_NO_REGISTRADO: 'Correo no registrado',
  CUENTA_INACTIVA: 'Cuenta inactiva',
  CREDENCIALES_INVALIDAS: 'Contraseña incorrecta',
  TOKEN_INVALIDO: 'Token inválido',
  OTRO: 'Otro',
};

const SecurityPanel = () => {
  const { user } = useContext(AuthContext);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [correo, setCorreo] = useState('');
  const [ip, setIp] = useState('');
  const [exito, setExito] = useState(''); // '', 'true', 'false'
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');

  const fetchData = useCallback(async (targetPage = 0) => {
    try {
      setLoading(true);
      const data = await getHistorialLogin({ correo, ip, exito, desde, hasta, page: targetPage, size: 25 });
      setRows(Array.isArray(data?.content) ? data.content : []);
      setTotalPages(data?.totalPages ?? 0);
      setTotalElements(data?.totalElements ?? 0);
      setPage(data?.number ?? targetPage);
    } catch (e) {
      console.error('Error cargando historial de login:', e);
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [correo, ip, exito, desde, hasta]);

  useEffect(() => { fetchData(0); }, [fetchData]);

  if (!user) return <Spinner message="Cargando perfil..." />;

  const fmtFecha = (f) => f ? new Date(f).toLocaleString('es-CO') : '—';

  return (
    <div className="page-container">
      <div className={styles.header}>
        <h1 className="page-title" style={{ marginBottom: 0, display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Shield size={24} color="var(--primary)" /> Panel de Seguridad
        </h1>
      </div>

      <div className="card">
        {/* Barra de filtros (mismo patrón que Reportes) */}
        <div className={styles.filterToolbar}>
          <div style={{ position: 'relative' }}>
            <Search size={18} style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)', pointerEvents: 'none' }} />
            <input
              type="text"
              placeholder="Filtrar por correo..."
              value={correo}
              onChange={e => setCorreo(e.target.value)}
              className={styles.searchInput}
            />
          </div>
          <div className={styles.filterRow}>
            <input type="text" placeholder="IP" value={ip} onChange={e => setIp(e.target.value)} className={styles.filterInput} />
            <select value={exito} onChange={e => setExito(e.target.value)} className={styles.filterInput}>
              <option value="">Todos</option>
              <option value="true">Exitosos</option>
              <option value="false">Fallidos</option>
            </select>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span className={styles.filterLabel}>Desde</span>
              <input type="date" value={desde} onChange={e => setDesde(e.target.value)} className={styles.filterInput} />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span className={styles.filterLabel}>Hasta</span>
              <input type="date" value={hasta} onChange={e => setHasta(e.target.value)} className={styles.filterInput} />
            </div>
            <button
              className={styles.clearBtn}
              onClick={() => { setCorreo(''); setIp(''); setExito(''); setDesde(''); setHasta(''); }}
            >
              Limpiar
            </button>
          </div>
        </div>

        <div className={styles.tableContainer}>
          {loading ? (
            <Spinner message="Cargando historial..." />
          ) : (
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Estado</th>
                  <th>Correo</th>
                  <th>Método</th>
                  <th>Motivo</th>
                  <th>IP</th>
                  <th>Fecha</th>
                </tr>
              </thead>
              <tbody>
                {rows.map(r => (
                  <tr key={r.id}>
                    <td>
                      {r.exito
                        ? <span className={styles.badgeOk}><CheckCircle2 size={13} /> Exitoso</span>
                        : <span className={styles.badgeFail}><XCircle size={13} /> Fallido</span>}
                    </td>
                    <td style={{ fontWeight: 600 }}>{r.correoIntentado}</td>
                    <td>{METODO_LABEL[r.metodo] || r.metodo}</td>
                    <td style={{ color: 'var(--text-secondary)' }}>{r.motivoFallo ? (MOTIVO_LABEL[r.motivoFallo] || r.motivoFallo) : '—'}</td>
                    <td style={{ fontFamily: 'monospace', fontSize: '12px' }}>{r.ip || '—'}</td>
                    <td style={{ fontSize: '12px' }}>{fmtFecha(r.fecha)}</td>
                  </tr>
                ))}
                {rows.length === 0 && (
                  <tr>
                    <td colSpan="6" style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)' }}>
                      No hay registros con los filtros aplicados.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </div>

        {/* Paginación */}
        {totalPages > 1 && (
          <div className={styles.pagination}>
            <button disabled={page <= 0} onClick={() => fetchData(page - 1)} className={styles.pageBtn}>
              <ChevronLeft size={16} /> Anterior
            </button>
            <span className={styles.pageInfo}>
              Página {page + 1} de {totalPages} · {totalElements} registros
            </span>
            <button disabled={page >= totalPages - 1} onClick={() => fetchData(page + 1)} className={styles.pageBtn}>
              Siguiente <ChevronRight size={16} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default SecurityPanel;
```

- [ ] **Step 2: Crear el CSS de la página**

```css
.header { margin-bottom: 24px; }

.filterToolbar { display: flex; flex-direction: column; gap: 14px; margin-bottom: 20px; }

.searchInput {
  width: 320px; max-width: 100%; padding: 10px 14px 10px 42px;
  border-radius: var(--radius-md); border: 1px solid var(--border);
  font-size: 14px; outline: none; background: var(--surface); color: var(--text-main);
}
.searchInput:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }

.filterRow { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }

.filterInput {
  height: 36px; padding: 0 12px; border-radius: var(--radius-sm);
  border: 1px solid var(--border); font-size: 0.85rem; background: var(--surface); color: var(--text-main);
}

.filterLabel { font-size: 0.78rem; color: var(--text-secondary); font-weight: 600; }

.clearBtn {
  margin-left: auto; padding: 0 16px; height: 36px; border-radius: var(--radius-pill);
  border: 1px solid var(--border); background: var(--surface); color: var(--text-secondary);
  font-weight: 600; font-size: 0.82rem; cursor: pointer;
}

.tableContainer { overflow-x: auto; }
.table { width: 100%; border-collapse: collapse; }
.table th {
  text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: 0.04em;
  color: var(--text-muted); padding: 10px 14px; border-bottom: 1px solid var(--border);
}
.table td { padding: 12px 14px; border-bottom: 1px solid var(--border); font-size: 13px; color: var(--text-main); }

.badgeOk, .badgeFail {
  display: inline-flex; align-items: center; gap: 5px; padding: 3px 10px;
  border-radius: var(--radius-pill); font-size: 11px; font-weight: 700;
}
.badgeOk { background: #ecfdf5; color: #059669; border: 1px solid #a7f3d0; }
.badgeFail { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 16px 0 4px; }
.pageBtn {
  display: inline-flex; align-items: center; gap: 4px; padding: 8px 14px;
  border-radius: var(--radius-pill); border: 1px solid var(--border); background: var(--surface);
  color: var(--text-secondary); font-weight: 600; font-size: 0.82rem; cursor: pointer;
}
.pageBtn:disabled { opacity: 0.45; cursor: not-allowed; }
.pageInfo { font-size: 0.82rem; color: var(--text-secondary); }
```

- [ ] **Step 3: Registrar la ruta en `App.jsx`**

1. Añadir el lazy import junto a los demás (línea ~30):
   ```jsx
   const SecurityPanel     = lazy(() => import('./pages/SecurityPanel'));
   ```
2. Añadir el bloque de ruta protegida SOLO SUPER_ADMIN, después del bloque de `/usuarios` (línea ~59):
   ```jsx
   <Route element={<ProtectedRoute allowedRoles={['SUPER_ADMIN']} />}>
     <Route path="/seguridad" element={<ErrorBoundary><Suspense fallback={<Spinner message="Cargando..." />}><SecurityPanel /></Suspense></ErrorBoundary>} />
   </Route>
   ```

- [ ] **Step 4: Añadir el ítem de navegación en `Sidebar.jsx` (solo SUPER_ADMIN)**

1. Añadir `Shield` al import de lucide-react (línea ~15): `import { Calendar, CalendarDays, Megaphone, FileText, Users, Shield, X } from 'lucide-react';`
2. Después del bloque de navegación de `/usuarios` (línea ~121), añadir:
   ```jsx
   {user?.rol?.toString().toUpperCase() === 'SUPER_ADMIN' && (
     <NavLink
       to="/seguridad"
       onClick={handleNavClick}
       className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
     >
       <Shield className={styles.icon} size={20} />
       Seguridad
     </NavLink>
   )}
   ```

- [ ] **Step 5: Lint de todo lo tocado**

Run: `npx eslint src/pages/SecurityPanel.jsx src/App.jsx src/components/layout/Sidebar.jsx`
Expected: 0 errores (warnings preexistentes del repo aceptables).

- [ ] **Step 6: Verificar visualmente con el servidor de desarrollo**

Con `npm run dev` corriendo, entrar como SUPER_ADMIN a `/seguridad`: debe verse la tabla con al menos el registro del propio login recién hecho; probar los filtros (fallidos, por correo) y la paginación. Confirmar que un usuario COMUNICACIONES/OFICINA NO ve el ítem "Seguridad" en el sidebar y que navegar a `/seguridad` lo redirige (ProtectedRoute).

- [ ] **Step 7: Commit**

```bash
git add src/pages/SecurityPanel.jsx src/pages/SecurityPanel.module.css src/App.jsx src/components/layout/Sidebar.jsx
git commit -m "feat(seguridad): panel de historial de login para SuperAdmin"
```

---

## Self-Review (cobertura del spec)

- **Registrar éxitos y fallos** → Tasks 6, 7 (local + Microsoft, todas las ramas).
- **Ambos métodos unificados con `metodo`** → enum `AuthProvider` reutilizado (Task 2), cableado en 6 y 7.
- **Retención 90 días** → purga `@Scheduled` (Task 5) + test (Task 9).
- **Panel con filtros** → endpoint paginado con filtros (Task 8) + página React (Task 12).
- **Solo SuperAdmin** → `@PreAuthorize("hasRole('SUPER_ADMIN')")` (Task 8, test de 403 incluido) + ruta y nav SUPER_ADMIN (Task 12).
- **IP endurecida sin duplicar lógica** → `ClientIpResolver` compartido + refactor del filter con test de regresión (Task 4).
- **Fallos sobreviven al rollback del login** → `REQUIRES_NEW` (Task 5) verificado indirectamente por los tests de fallo en Task 6 (que persisten pese a la excepción).
- **Sin perjudicar lo existente** → Task 10 corre la suite completa; el refactor del filter se cubre con su test de regresión; el registro es best-effort (try/catch) y nunca tumba el login.

## Handoff de ejecución

Este plan está pensado para ejecutarse task por task con TDD (rojo → verde →
commit). Cada task deja el sistema compilando y con sus tests en verde antes de
pasar a la siguiente.

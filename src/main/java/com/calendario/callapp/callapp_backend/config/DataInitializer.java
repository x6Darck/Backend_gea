package com.calendario.callapp.callapp_backend.config;

import com.calendario.callapp.callapp_backend.entity.*;
import com.calendario.callapp.callapp_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * Inicializador de datos maestros y de prueba al arrancar la aplicación.
 *
 * <p>Implementa {@link CommandLineRunner} para ejecutarse una vez al inicio. Crea roles,
 * oficinas, tipos de evento, lugares físicos y usuarios de prueba si no existen.
 * Controlado por la propiedad {@code app.data.seed-demo} (por defecto {@code true});
 * en producción se debe desactivar con {@code app.data.seed-demo=false}.</p>
 */
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final OficinaRepository oficinaRepository;
    private final RolRepository rolRepository;
    private final TipoEventoCatalogoRepository tipoEventoRepository;
    private final SolicitudEventoRepository solicitudEventoRepository;
    private final SolicitudAnuncioRepository solicitudAnuncioRepository;
    private final LugarFisicoRepository lugarFisicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final ArchivoAdjuntoRepository archivoAdjuntoRepository;
    private final org.springframework.core.io.ResourceLoader resourceLoader;

    @org.springframework.beans.factory.annotation.Value("${app.data.seed-demo:true}")
    private boolean seedDemo;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            log.info("Iniciando inicialización de datos de sistema y pruebas...");
            
            // 1. Catálogos Base
            inicializarRoles();
            inicializarOficinas();
            inicializarTiposEvento();
            inicializarLugaresFisicos();
            
            // 2. Usuarios del Sistema
            inicializarUsuarios();
            
            // 3. Datos de Prueba (solo en dev — desactivar con app.data.seed-demo=false)
            if (seedDemo) inicializarDatosPrueba();
            
            log.info("Inicialización de datos completada exitosamente.");
        } catch (Exception e) {
            log.error("Error crítico durante la inicialización de datos: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("null")
    private void inicializarRoles() {
        List.of("SuperAdmin", "Comunicaciones", "Oficina", "Usuario Autenticado", "Consultoria")
            .forEach(nombre -> {
                if (rolRepository.findByNombre(nombre).isEmpty()) {
                    log.info("Creando rol base: {}", nombre);
                    rolRepository.save(crearRol(nombre));
                }
            });
    }

    private RolEntity crearRol(String nombre) {
        RolEntity r = new RolEntity();
        r.setNombre(nombre);
        r.setFechaCreacion(LocalDateTime.now());
        return r;
    }

    @SuppressWarnings("null")
    private void inicializarOficinas() {
        log.info("Sincronizando catálogo de oficinas...");
        List<Oficina> oficinasNuevas = List.of(
            crearOficina("Facultad Derecho", "Derecho", "Facultad de Ciencias Jurídicas"),
            crearOficina("Facultad CEAC", "Ciencias Económicas", "Facultad de Ciencias Económicas, Administrativas y Contables"),
            crearOficina("Facultad Ingeniería", "Ingeniería", "Facultad de Ingeniería y Arquitectura"),
            crearOficina("Rectoría", "Administración", "Despacho del Rector"),
            crearOficina("Biblioteca", "Académico", "Servicios de Biblioteca"),
            crearOficina("Consultorio Jurídico", "Derecho", "Consultorio Jurídico y Centro de Conciliación"),
            crearOficina("Bienestar Universitario", "Bienestar", "Coordinación de Bienestar Universitario"),
            crearOficina("Proyección Social", "Extensión", "Oficina de Proyección Social y Extensión"),
            crearOficina("Marketing y Comunicaciones", "Comunicaciones", "Oficina de Marketing y Comunicaciones"),
            crearOficina("Sistemas", "Administración", "Oficina de Sistemas y Tecnologías"),
            crearOficina("Egresados", "Institucional", "Oficina de Egresados")
        );

        List<String> nombresNuevos = oficinasNuevas.stream()
                .map(o -> o.getNombre().toLowerCase())
                .toList();

        // 1. Sincronizar
        for (Oficina o : oficinasNuevas) {
            oficinaRepository.findByNombreIgnoreCase(o.getNombre()).ifPresentOrElse(
                existente -> {
                    existente.setProgramaAcademico(o.getProgramaAcademico());
                    existente.setDescripcion(o.getDescripcion());
                    existente.setActiva(true);
                    oficinaRepository.save(existente);
                },
                () -> oficinaRepository.save(o)
            );
        }

        // 2. Desactivar obsoletas
        List<Oficina> todas = oficinaRepository.findAll();
        for (Oficina dbOfi : todas) {
            if (!nombresNuevos.contains(dbOfi.getNombre().toLowerCase())) {
                dbOfi.setActiva(false);
                oficinaRepository.save(dbOfi);
            }
        }
    }

    private Oficina crearOficina(String nombre, String programa, String desc) {
        Oficina o = new Oficina();
        o.setNombre(nombre);
        o.setProgramaAcademico(programa);
        o.setDescripcion(desc);
        o.setActiva(true);
        o.setFechaCreacion(LocalDateTime.now());
        return o;
    }

    @SuppressWarnings("null")
    private void inicializarTiposEvento() {
        log.info("Sincronizando catálogo de tipos de evento...");
        // Lista canónica de categorías (orden de presentación)
        List<TipoEventoCatalogo> tipos = List.of(
            crearTipo("Proyección Social", "#0ea5e9", "Eventos de proyección social"),
            crearTipo("Investigación", "#0f766e", "Eventos de investigación y ciencia"),
            crearTipo("Bienestar Institucional", "#f59e0b", "Eventos de bienestar institucional"),
            crearTipo("Cultural", "#8b5cf6", "Eventos culturales y artísticos"),
            crearTipo("Académico", "#ce1126", "Eventos de carácter académico"),
            crearTipo("Aseguramiento de la calidad", "#16a34a", "Eventos de aseguramiento de la calidad"),
            crearTipo("Otra", "#64748b", "Otra categoría de evento")
        );

        for (TipoEventoCatalogo t : tipos) {
            tipoEventoRepository.findByNombreIgnoreCase(t.getNombre()).ifPresentOrElse(
                existente -> {
                    existente.setColorHex(t.getColorHex());
                    existente.setNombre(t.getNombre()); // Asegurar casing
                    existente.setActivo(true);
                    tipoEventoRepository.save(existente);
                },
                () -> tipoEventoRepository.save(t)
            );
        }

        // Eliminar categorías obsoletas: reasignar sus eventos a "Otra" para no romper FKs
        TipoEventoCatalogo otra = tipoEventoRepository.findByNombreIgnoreCase("Otra").orElse(null);
        if (otra != null) {
            List<String> canonicas = tipos.stream().map(x -> x.getNombre().toLowerCase()).toList();
            for (TipoEventoCatalogo dbTipo : tipoEventoRepository.findAll()) {
                if (!canonicas.contains(dbTipo.getNombre().toLowerCase())) {
                    List<SolicitudEvento> refs = solicitudEventoRepository.findByTipoEventoCatalogoId(dbTipo.getId());
                    for (SolicitudEvento s : refs) {
                        s.setTipoEventoCatalogo(otra);
                    }
                    if (!refs.isEmpty()) solicitudEventoRepository.saveAll(refs);
                    log.info("Eliminando categoría obsoleta '{}' ({} eventos reasignados a 'Otra')", dbTipo.getNombre(), refs.size());
                    tipoEventoRepository.delete(dbTipo);
                }
            }
        }
    }

    private void inicializarLugaresFisicos() {
        log.info("Sincronizando catálogo de lugares físicos...");
        List<LugarFisico> lugaresNuevos = List.of(
            crearLugar("Aula Máxima", "Aula de conferencias principal", 150),
            crearLugar("Sala de Audiencias", "Sala especializada para prácticas jurídicas", 40),
            crearLugar("Teatro", "Teatro institucional para eventos artísticos", 300),
            crearLugar("Biblioteca", "Área general de biblioteca", 100),
            crearLugar("Plaza de Banderas", "Espacio exterior para actos protocolarios", 400),
            crearLugar("Cafetería", "Zona de alimentación y descanso", 120),
            crearLugar("Edificio Postgrados", "Instalaciones de formación avanzada", 80),
            crearLugar("Pasillo Derecho", "Espacio de tránsito para exhibiciones temporales", 50),
            crearLugar("Pasillo Contaduria", "Área de tránsito de la facultad de contaduría", 50),
            crearLugar("Parqueadero", "Zona de estacionamiento institucional", 100),
            crearLugar("Edificio Administrativo", "Bloque de oficinas administrativas", 50),
            crearLugar("Sala de Docentes", "Área de trabajo y descanso para profesores", 30),
            crearLugar("Bienestar Universitario", "Oficinas y áreas de servicios de bienestar", 60),
            crearLugar("Externo", "Ubicación externa a la universidad", null, true)
        );

        List<String> nombresNuevos = lugaresNuevos.stream()
                .map(l -> l.getNombre().toLowerCase())
                .toList();

        // 1. Sincronizar (Crear o Actualizar y activar)
        for (LugarFisico l : lugaresNuevos) {
            lugarFisicoRepository.findByNombreIgnoreCase(l.getNombre()).ifPresentOrElse(
                existente -> {
                    existente.setDescripcion(l.getDescripcion());
                    existente.setCapacidad(l.getCapacidad());
                    existente.setActivo(true);
                    existente.setEsExterno(l.getEsExterno() != null && l.getEsExterno());
                    lugarFisicoRepository.save(existente);
                },
                () -> lugarFisicoRepository.save(l)
            );
        }

        // 2. Desactivar los que no están en la lista oficial
        List<LugarFisico> todosEnDb = lugarFisicoRepository.findAll();
        for (LugarFisico dbLugar : todosEnDb) {
            if (!nombresNuevos.contains(dbLugar.getNombre().toLowerCase())) {
                dbLugar.setActivo(false);
                lugarFisicoRepository.save(dbLugar);
            }
        }
    }

    private LugarFisico crearLugar(String nombre, String desc, Integer capacidad) {
        return crearLugar(nombre, desc, capacidad, false);
    }

    private LugarFisico crearLugar(String nombre, String desc, Integer capacidad, boolean esExterno) {
        LugarFisico l = new LugarFisico();
        l.setNombre(nombre);
        l.setDescripcion(desc);
        l.setCapacidad(capacidad);
        l.setActivo(true);
        l.setEsExterno(esExterno);
        l.setFechaCreacion(LocalDateTime.now());
        return l;
    }

    private TipoEventoCatalogo crearTipo(String nombre, String color, String desc) {
        TipoEventoCatalogo t = new TipoEventoCatalogo();
        t.setNombre(nombre);
        t.setColorHex(color);
        t.setDescripcion(desc);
        t.setActivo(true);
        return t;
    }

    private void inicializarUsuarios() {
        if (usuarioRepository.count() == 0) {
            log.info("Creando cuentas base del sistema...");
            
            Oficina sistemas = oficinaRepository.findByNombreIgnoreCase("Sistemas").orElse(null);
            Oficina comunicaciones = oficinaRepository.findByNombreIgnoreCase("Marketing y Comunicaciones").orElse(null);
            Oficina rectoria = oficinaRepository.findByNombreIgnoreCase("Rectoría").orElse(null);

            if (sistemas != null) {
                crearUsuario("Administrador CallApp", "Cesar@gmail.com", "1234", "SuperAdmin", sistemas);
            }
            if (comunicaciones != null) {
                crearUsuario("Encargado Comunicaciones", "Comunicaciones@gmail.com", "1234", "Comunicaciones", comunicaciones);
            }
            if (rectoria != null) {
                crearUsuario("Oficina Rectoría", "oficina@gmail.com", "1234", "Oficina", rectoria);
            }
            
            // Vincular fotos por defecto
            vincularFotosPorDefecto();
        }
    }

    private void vincularFotosPorDefecto() {
        try {
            String tokenAvatar = seedFile("static/assets/img/default-avatar.png", "avatar-sistema.png", "image/png");
            if (tokenAvatar != null) {
                usuarioRepository.findAll().forEach(u -> {
                    if (u.getFotoUrl() == null || u.getFotoUrl().isBlank()) {
                        u.setFotoUrl("/archivos/public/" + tokenAvatar);
                        usuarioRepository.save(u);
                    }
                });
            }
        } catch (Exception e) {
            log.error("No se pudieron vincular las fotos por defecto: {}", e.getMessage());
        }
    }

    private String seedFile(String resourcePath, String targetName, String contentType) {
        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
            if (!resource.exists()) return null;

            // Evitar duplicados por nombre original
            return archivoAdjuntoRepository.findByNombreOriginal(targetName)
                .map(ArchivoAdjunto::getTokenAcceso)
                .orElseGet(() -> {
                    try {
                        // Guardar físicamente si no existe
                        java.nio.file.Path rootLocation = java.nio.file.Paths.get("uploads").toAbsolutePath().normalize();
                        if (!java.nio.file.Files.exists(rootLocation)) {
                            java.nio.file.Files.createDirectories(rootLocation);
                        }
                        
                        String nombreAlmacenado = java.util.UUID.randomUUID() + "-" + targetName;
                        java.nio.file.Path destino = rootLocation.resolve(nombreAlmacenado);
                        java.nio.file.Files.copy(resource.getInputStream(), destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                        ArchivoAdjunto meta = new ArchivoAdjunto();
                        meta.setNombreOriginal(targetName);
                        meta.setNombreAlmacenado(nombreAlmacenado);
                        meta.setTokenAcceso(java.util.UUID.randomUUID().toString().replace("-", ""));
                        meta.setContentType(contentType);
                        meta.setTamano(resource.contentLength());
                        meta.setPublico(true);
                        meta.setFechaCreacion(LocalDateTime.now());
                        return archivoAdjuntoRepository.save(meta).getTokenAcceso();
                    } catch (Exception ex) {
                        log.error("Error al copiar archivo de semilla {}: {}", targetName, ex.getMessage());
                        return null;
                    }
                });
        } catch (Exception e) {
            return null;
        }
    }

    private void crearUsuario(String nombre, String email, String pass, String rolNombre, Oficina oficina) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(email);
        u.setPassword(passwordEncoder.encode(pass));
        u.setEstado("ACTIVO");
        u.setAuthProvider(AuthProvider.LOCAL);
        u.setFechaCreacion(LocalDateTime.now());
        u.setOficina(oficina);
        
        rolRepository.findByNombre(rolNombre).ifPresent(u::setRolEntity);
        usuarioRepository.save(u);
    }

    private void inicializarDatosPrueba() {
        if (solicitudEventoRepository.count() == 0) {
            log.info("Generando registros de prueba para eventos y anuncios...");
            
            Usuario cesar = usuarioRepository.getByCorreoOptimized("Cesar@gmail.com").orElse(null);
            Usuario oficinaUser = usuarioRepository.getByCorreoOptimized("oficina@gmail.com").orElse(null);
            Oficina sistemas = cesar != null ? cesar.getOficina() : null;
            TipoEventoCatalogo academico = tipoEventoRepository.findByNombreIgnoreCaseAndActivoTrue("ACADEMICO").orElse(null);

            if (cesar != null && sistemas != null && academico != null) {
                // 1. Solicitud de Evento de Prueba
                SolicitudEvento evento = new SolicitudEvento();
                evento.setNombreEvento("Seminario de Innovación Tecnológica");
                evento.setDescripcionEvento("Un seminario para demostrar las capacidades del sistema CallApp.");
                evento.setFechaEvento(LocalDate.now().plusDays(7));
                evento.setHoraInicio(LocalTime.of(8, 0));
                evento.setHoraFin(LocalTime.of(12, 0));
                LugarFisico auditorio = lugarFisicoRepository.findByNombreIgnoreCase("Auditorio Manuelita Saénz").orElse(null);
                if (auditorio != null) {
                    evento.getLugaresFisicos().add(auditorio);
                }
                evento.setResponsableEvento("Cesar Diaz");
                evento.setOficina(sistemas);
                evento.setUsuarioSolicitante(cesar);
                evento.setTipoEventoCatalogo(academico);
                evento.setEstado(EstadoSolicitud.APROBADA);
                evento.setFechaCreacion(LocalDateTime.now());
                
                // Seed pieza gráfica para el evento de prueba
                String tokenPieza = seedFile("static/assets/img/default-event.png", "evento-prueba.png", "image/png");
                if (tokenPieza != null) {
                    evento.setPiezaGraficaUrl("/archivos/public/" + tokenPieza);
                }
                
                solicitudEventoRepository.save(evento);

                // 2. Solicitud de Anuncio de Prueba
                SolicitudAnuncio anuncio = new SolicitudAnuncio();
                anuncio.setTitulo("Recordatorio de Registro CallApp");
                anuncio.setDescripcion("No olvides completar tu perfil en la plataforma.");
                anuncio.setCategoria("Informativo");
                LugarFisico patio = lugarFisicoRepository.findByNombreIgnoreCase("Patio Central").orElse(null);
                if (patio != null) {
                    anuncio.getLugaresFisicos().add(patio);
                }
                anuncio.setFechaInicioPublicacion(LocalDate.now());
                anuncio.setFechaFinPublicacion(LocalDate.now().plusDays(30));
                anuncio.setEstado(EstadoSolicitud.PUBLICADA);
                anuncio.setUsuarioSolicitante(oficinaUser != null ? oficinaUser : cesar);
                anuncio.setFechaCreacion(LocalDateTime.now());
                solicitudAnuncioRepository.save(anuncio);
            }
        }
    }
}

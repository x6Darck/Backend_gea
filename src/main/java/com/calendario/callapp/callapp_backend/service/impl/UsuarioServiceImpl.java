package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.request.UsuarioRequest;
import com.calendario.callapp.callapp_backend.dto.response.UsuarioResponse;
import com.calendario.callapp.callapp_backend.entity.AuthProvider;
import com.calendario.callapp.callapp_backend.entity.Rol;
import com.calendario.callapp.callapp_backend.entity.RolEntity;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import com.calendario.callapp.callapp_backend.repository.OficinaRepository;
import com.calendario.callapp.callapp_backend.repository.RolRepository;
import com.calendario.callapp.callapp_backend.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Objects;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * CRUD de usuarios del sistema: creación, edición, desactivación y búsqueda.
 *
 * <p>Valida unicidad de correo, teléfono y OID de Microsoft antes de persistir
 * para evitar colisiones. La contraseña se encripta con {@link BCryptPasswordEncoder}.
 * La referencia circular con {@link CustomUserDetailsService} se rompe con {@code @Lazy}.</p>
 */
public class UsuarioServiceImpl {

    private final UsuarioRepository usuarioRepository;
    private final OficinaRepository oficinaRepository;
    private final RolRepository rolRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    @Lazy
    private CustomUserDetailsService customUserDetailsService;

    @Transactional
    public UsuarioResponse crearUsuario(UsuarioRequest request) {
        validarDatosBasicos(request);
        validarCorreoDisponible(request.getCorreo(), null);
        validarTelefonoDisponible(request.getTelefono(), null);
        validarMicrosoftOidDisponible(request.getMicrosoftOid(), null);
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setCorreo(request.getCorreo());
        usuario.setTelefono(request.getTelefono());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRolEntity(resolveRolEntity(request));
        usuario.setOficina(resolveOficina(request));
        usuario.setEstado("ACTIVO");
        usuario.setAuthProvider(request.getAuthProvider() != null ? request.getAuthProvider() : AuthProvider.LOCAL);
        usuario.setMicrosoftOid(request.getMicrosoftOid());
        usuario.setFotoUrl(request.getFotoUrl());
        usuario.setFechaCreacion(LocalDateTime.now());

        UsuarioResponse response = toResponse(usuarioRepository.save(usuario));
        log.info("Usuario creado: {} [{}]", request.getCorreo(), request.getRol());
        return response;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios(String q, Rol rol, String estado) {
        // Normalize blank strings to null so JPQL :param IS NULL checks work correctly
        String qNorm = (q == null || q.isBlank()) ? null : q.trim();
        String estadoNorm = (estado == null || estado.isBlank()) ? null : estado.trim().toUpperCase();

        String rolName = null;
        if (rol != null) {
            rolName = switch (rol) {
                case SUPER_ADMIN -> "SuperAdmin";
                case ADMIN -> "Admin";
                case COMUNICACIONES -> "Comunicaciones";
                case OFICINA -> "Oficina";
                case USUARIO_AUTENTICADO_APP -> "Usuario Autenticado";
                case CONSULTORIA -> "Consultoria";
                default -> rol.name();
            };
        }
        return usuarioRepository.searchUsuarios(qNorm, rolName, estadoNorm).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario no encontrado"));
        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse actualizarUsuario(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario no encontrado"));

        validarDatosBasicos(request);
        validarCorreoDisponible(request.getCorreo(), id);
        validarTelefonoDisponible(request.getTelefono(), id);
        validarMicrosoftOidDisponible(request.getMicrosoftOid(), id);
        usuario.setNombre(request.getNombre());
        usuario.setCorreo(request.getCorreo());
        usuario.setTelefono(request.getTelefono());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        usuario.setRolEntity(resolveRolEntity(request));
        usuario.setOficina(resolveOficina(request));
        usuario.setAuthProvider(
                request.getAuthProvider() != null ? request.getAuthProvider() : usuario.getAuthProvider());
        usuario.setMicrosoftOid(request.getMicrosoftOid());
        if (request.getFotoUrl() != null) {
            usuario.setFotoUrl(request.getFotoUrl());
        }
        if (request.getEstado() != null) {
            String estadoNorm = request.getEstado().trim().toUpperCase();
            if (!estadoNorm.equals("ACTIVO") && !estadoNorm.equals("INACTIVO")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido: " + request.getEstado());
            }
            usuario.setEstado(estadoNorm);
        }

        UsuarioResponse response = toResponse(usuarioRepository.save(usuario));
        customUserDetailsService.invalidarCacheUsuario(usuario.getCorreo());
        log.info("Usuario actualizado: id={}, correo={}", id, request.getCorreo());
        return response;
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario no encontrado"));
        if (usuario != null) {
            usuarioRepository.delete(usuario);
            log.info("Usuario eliminado: id={}, correo={}", id, usuario.getCorreo());
        }
    }

    private void validarDatosBasicos(UsuarioRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del usuario no puede estar vacío");
        }
    }

    private void validarCorreoDisponible(String correo, Long usuarioIdActual) {
        if (correo == null || correo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar el correo del usuario");
        }

        usuarioRepository.getByCorreoOptimized(correo).ifPresent(existente -> {
            if (usuarioIdActual == null || !existente.getId().equals(usuarioIdActual)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ya existe un usuario con el correo: " + correo);
            }
        });
    }

    private void validarTelefonoDisponible(String telefono, Long usuarioIdActual) {
        if (telefono == null || telefono.trim().isEmpty() || telefono.trim().equalsIgnoreCase("Sin celular"))
            return;

        usuarioRepository.findByTelefonoExcluyendo(telefono, usuarioIdActual).ifPresent(u -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ya existe un usuario con el teléfono: " + telefono);
        });
    }

    private void validarMicrosoftOidDisponible(String oid, Long usuarioIdActual) {
        if (oid == null || oid.isBlank())
            return;

        usuarioRepository.findByMicrosoftOidExcluyendo(oid, usuarioIdActual).ifPresent(u -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ya existe un usuario vinculado a esta cuenta de Microsoft (OID: " + oid + ")");
        });
    }

    private RolEntity resolveRolEntity(UsuarioRequest request) {
        if (request.getIdRol() == null)
            return null;
        Long idRol = request.getIdRol();
        return idRol != null ? rolRepository.findById(idRol)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Rol no encontrado")) : null;
    }

    private Oficina resolveOficina(UsuarioRequest request) {
        // 1. Si se proporciona un ID de oficina explícito y válido, buscarlo y retornarlo.
        if (request.getIdOficina() != null && request.getIdOficina() > 0) {
            return oficinaRepository.findById(java.util.Objects.requireNonNull(request.getIdOficina()))
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Oficina no encontrada con ID: " + request.getIdOficina()));
        }

        // 2. Si no hay ID de oficina pero el rol es COMUNICACIONES, intentar asignar la oficina por defecto.
        // Nota: Mantenemos el chequeo de ID 2 por compatibilidad con la base de datos actual si es necesario.
        boolean esComunicaciones = (request.getIdRol() != null && request.getIdRol() == 2) || 
                                   (request.getRol() == Rol.COMUNICACIONES);
        
        if (esComunicaciones) {
            return oficinaRepository.findByNombreIgnoreCase("Comunicaciones")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La oficina 'Comunicaciones' es obligatoria para este rol y no se encontró en el sistema."));
        }

        // 3. Si es USUARIO_AUTENTICADO_APP, asignar oficina "Usuarios" automáticamente.
        boolean esUsuarioExterno = (request.getIdRol() != null && request.getIdRol() == 4) || 
                                    (request.getRol() == Rol.USUARIO_AUTENTICADO_APP);
        
        if (esUsuarioExterno) {
            return oficinaRepository.findByNombreIgnoreCase("Usuarios")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La oficina 'Usuarios' es necesaria para este rol y no se encontró."));
        }

        // 4. En cualquier otro caso, la oficina es opcional.
        return null;
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .telefono(usuario.getTelefono())
                .estado(usuario.getEstado())
                .rol(usuario.getRol())
                .idOficina(usuario.getOficina() != null ? usuario.getOficina().getId() : null)
                .oficinaNombre(usuario.getOficina() != null ? usuario.getOficina().getNombre() : null)
                .authProvider(usuario.getAuthProvider())
                .fotoUrl(usuario.getFotoUrl())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .usuarioCreacion(usuario.getUsuarioCreacion())
                .usuarioActualizacion(usuario.getUsuarioActualizacion())
                .build();
    }

}

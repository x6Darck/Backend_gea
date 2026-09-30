package com.calendario.callapp.callapp_backend.util;

import com.calendario.callapp.callapp_backend.entity.AuditRevisionEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Listener que Hibernate Envers invoca al crear cada nueva revisión de auditoría.
 *
 * <p>Enriquece la entidad {@link com.calendario.callapp.callapp_backend.entity.AuditRevisionEntity}
 * con el correo del usuario autenticado (o {@code "SISTEMA/PUBLICO"} si no hay sesión)
 * y la IP del cliente (respetando {@code X-Forwarded-For} para depliegues detrás de proxy).
 * Está registrado en {@code @RevisionEntity(AuditRevisionEntity.class)} mediante la anotación
 * {@code @RevisionListener(AuditRevisionListener.class)}.</p>
 */
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevisionEntity auditEntity = (AuditRevisionEntity) revisionEntity;
        
        // 1. Obtener el usuario del contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            auditEntity.setNombreUsuario(auth.getName());
        } else {
            auditEntity.setNombreUsuario("SISTEMA/PUBLICO");
        }

        // 2. Obtener la IP del cliente
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = request.getRemoteAddr();
            }
            auditEntity.setIpAddress(ipAddress);
        }
    }
}

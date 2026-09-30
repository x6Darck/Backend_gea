-- V3: Agrega EN_REVISION al enum estado y columna observaciones_revision
-- Corresponde a la feature "Moderación En revisión" (SolicitudEvento y SolicitudAnuncio)

-- ── solicitudes_evento ───────────────────────────────────────────────────────
ALTER TABLE solicitudes_evento
    MODIFY COLUMN estado ENUM('APROBADA','EN_REVISION','PENDIENTE','PUBLICADA','RECHAZADA')
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE solicitudes_evento
    ADD COLUMN observaciones_revision VARCHAR(1000)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL
        AFTER motivo_rechazo;

-- ── solicitudes_evento_aud ───────────────────────────────────────────────────
ALTER TABLE solicitudes_evento_aud
    MODIFY COLUMN estado ENUM('APROBADA','EN_REVISION','PENDIENTE','PUBLICADA','RECHAZADA')
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;

ALTER TABLE solicitudes_evento_aud
    ADD COLUMN observaciones_revision VARCHAR(1000)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL
        AFTER motivo_rechazo;

-- ── solicitudes_anuncio ──────────────────────────────────────────────────────
ALTER TABLE solicitudes_anuncio
    MODIFY COLUMN estado ENUM('APROBADA','EN_REVISION','PENDIENTE','PUBLICADA','RECHAZADA')
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE solicitudes_anuncio
    ADD COLUMN observaciones_revision VARCHAR(1000)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL
        AFTER motivo_rechazo;

-- ── solicitudes_anuncio_aud ──────────────────────────────────────────────────
ALTER TABLE solicitudes_anuncio_aud
    MODIFY COLUMN estado ENUM('APROBADA','EN_REVISION','PENDIENTE','PUBLICADA','RECHAZADA')
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;

ALTER TABLE solicitudes_anuncio_aud
    ADD COLUMN observaciones_revision VARCHAR(1000)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL
        AFTER motivo_rechazo;

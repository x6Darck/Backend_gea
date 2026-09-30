-- Indica si un lugar físico es externo a la universidad.
-- Las filas existentes reciben FALSE por defecto.
ALTER TABLE lugares_fisicos
    ADD COLUMN es_externo TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE lugares_fisicos_aud
    ADD COLUMN es_externo TINYINT(1) DEFAULT NULL;

-- Dirección o nombre del lugar cuando el evento se realiza en sitio externo.
ALTER TABLE solicitudes_evento
    ADD COLUMN ubicacion_externa VARCHAR(300) DEFAULT NULL;

ALTER TABLE solicitudes_evento_aud
    ADD COLUMN ubicacion_externa VARCHAR(300) DEFAULT NULL;

ALTER TABLE solicitudes_evento
    ADD COLUMN requiere_servicios_generales TINYINT(1) NOT NULL DEFAULT 0;

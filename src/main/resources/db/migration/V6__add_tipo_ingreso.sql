-- Tipo de ingreso del evento (Libre / Pago / Privado).
-- Obligatorio: las filas existentes se rellenan con 'LIBRE' por el DEFAULT.
ALTER TABLE solicitudes_evento
    ADD COLUMN tipo_ingreso VARCHAR(20) NOT NULL DEFAULT 'LIBRE';

-- Columna espejo para la auditoría de Hibernate Envers (nullable, como el resto de columnas _aud).
ALTER TABLE solicitudes_evento_aud
    ADD COLUMN tipo_ingreso VARCHAR(20) DEFAULT NULL;

-- Índices para las columnas de filtro más usadas por el módulo de reportes,
-- el endpoint público de anuncios y la agenda pública, identificados en la
-- auditoría de escalabilidad (docs/superpowers/load-testing/findings-db.md,
-- hallazgos H1-H5). Sin estos índices, esas consultas hacen table scan a
-- medida que crece el histórico de solicitudes.

-- H1: findByVisibleTrueOrderByFechaPublicacionDesc() sin índice en `visible`.
CREATE INDEX idx_publicacion_anuncio_visible_fecha
  ON publicaciones_anuncio (visible, fecha_publicacion DESC);

-- H2: todas las queries de reportes filtran solicitudes_evento por fecha_creacion,
-- muchas veces combinado con id_oficina.
CREATE INDEX idx_solicitud_evento_fecha_creacion
  ON solicitudes_evento (fecha_creacion);
CREATE INDEX idx_solicitud_evento_oficina_fecha_creacion
  ON solicitudes_evento (id_oficina, fecha_creacion);

-- H3: mismo patrón que H2 pero para solicitudes_anuncio.
CREATE INDEX idx_solicitud_anuncio_fecha_creacion
  ON solicitudes_anuncio (fecha_creacion);
CREATE INDEX idx_solicitud_anuncio_oficina_fecha_creacion
  ON solicitudes_anuncio (id_oficina, fecha_creacion);

-- H4: solicitudes_evento.estado ya está indexado (idx_solicitud_evento_estado,
-- V1); solicitudes_anuncio.estado no lo estaba pese a seguir el mismo patrón
-- de uso (bandejas de moderación filtradas por estado).
CREATE INDEX idx_solicitud_anuncio_estado
  ON solicitudes_anuncio (estado);

-- H5: el ORDER BY de la agenda pública (es_importante DESC, fecha_evento ASC,
-- hora_inicio ASC) no tenía un índice compuesto que lo cubriera.
CREATE INDEX idx_solicitud_evento_importante_fecha
  ON solicitudes_evento (es_importante, fecha_evento, hora_inicio);

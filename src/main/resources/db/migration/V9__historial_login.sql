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
    fecha             DATETIME(6) NOT NULL,
    CONSTRAINT fk_historial_login_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- fecha: ordena el panel (DESC) y filtra la purga por retención.
CREATE INDEX idx_historial_login_fecha ON historial_login (fecha);
-- id_usuario ya tiene índice de soporte por la FK; no se duplica.
CREATE INDEX idx_historial_login_ip ON historial_login (ip);

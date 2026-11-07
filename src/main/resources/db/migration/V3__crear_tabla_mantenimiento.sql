-- Historial de mantenimientos. Si se borra el equipo, se borra su historial.
CREATE TABLE mantenimiento (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    equipo_id              BIGINT        NOT NULL,
    fecha                  DATETIME      NOT NULL,
    horometro_al_registrar DECIMAL(10,1) NOT NULL,
    notas                  VARCHAR(500)  NULL,
    CONSTRAINT pk_mantenimiento PRIMARY KEY (id),
    CONSTRAINT fk_mantenimiento_equipo FOREIGN KEY (equipo_id) REFERENCES equipo (id) ON DELETE CASCADE
);

CREATE INDEX idx_mantenimiento_equipo_fecha ON mantenimiento (equipo_id, fecha);

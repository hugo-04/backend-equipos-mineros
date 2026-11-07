-- Catalogo de tipos de equipo. El intervalo dice cada cuantas horas toca mantenimiento.
CREATE TABLE tipo_equipo (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(60) NOT NULL,
    intervalo_horas INT         NOT NULL,
    CONSTRAINT pk_tipo_equipo PRIMARY KEY (id),
    CONSTRAINT uk_tipo_equipo_nombre UNIQUE (nombre),
    CONSTRAINT ck_tipo_equipo_intervalo CHECK (intervalo_horas > 0)
);

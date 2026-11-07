-- Equipos mineros. "horometro_ultimo_mant" marca donde empezo el ciclo de mantenimiento actual.
CREATE TABLE equipo (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    codigo                VARCHAR(30)   NOT NULL,
    tipo_id               BIGINT        NOT NULL,
    horometro             DECIMAL(10,1) NOT NULL,
    horometro_ultimo_mant DECIMAL(10,1) NOT NULL DEFAULT 0,
    creado_en             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_equipo PRIMARY KEY (id),
    CONSTRAINT uk_equipo_codigo UNIQUE (codigo),
    CONSTRAINT fk_equipo_tipo FOREIGN KEY (tipo_id) REFERENCES tipo_equipo (id),
    CONSTRAINT ck_equipo_horometro_positivo CHECK (horometro >= 0),
    CONSTRAINT ck_equipo_horometro_mayor_o_igual CHECK (horometro >= horometro_ultimo_mant)
);

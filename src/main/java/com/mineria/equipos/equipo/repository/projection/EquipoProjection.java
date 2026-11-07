package com.mineria.equipos.equipo.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un equipo ya con los datos de su tipo unidos por el JOIN. Se usa para leer: trae solo las columnas
 * necesarias y evita cargar entidades (y una consulta extra por cada tipo).
 */
public interface EquipoProjection {

    Long getId();

    String getCodigo();

    Long getTipoId();

    String getTipoNombre();

    Integer getIntervaloHoras();

    BigDecimal getHorometro();

    BigDecimal getHorometroUltimoMant();

    Instant getCreadoEn();
}

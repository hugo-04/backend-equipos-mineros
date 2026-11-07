package com.mineria.equipos.equipo.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Proyección de solo lectura de un equipo con los datos de su tipo ya unidos (JOIN).
 * Trae solo las columnas necesarias y evita cargar entidades (y el N+1 del tipo).
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

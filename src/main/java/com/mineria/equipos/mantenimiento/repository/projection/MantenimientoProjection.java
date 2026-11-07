package com.mineria.equipos.mantenimiento.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

/** Proyección de solo lectura de una línea del historial de mantenimientos. */
public interface MantenimientoProjection {

    Long getId();

    Long getEquipoId();

    Instant getFecha();

    BigDecimal getHorometroAlRegistrar();

    String getNotas();
}

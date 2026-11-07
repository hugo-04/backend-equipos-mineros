package com.mineria.equipos.infrastructure.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;

/** Proyección de lectura de una línea del historial de mantenimientos. */
public interface MantenimientoProjection {

    Long getId();

    Long getEquipoId();

    Instant getFecha();

    BigDecimal getHorometroAlRegistrar();

    String getNotas();
}

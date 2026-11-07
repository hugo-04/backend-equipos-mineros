package com.mineria.equipos.mantenimiento.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

/** Una línea del historial tal como se muestra; es de solo lectura. */
public interface MantenimientoProjection {

    Long getId();

    Long getEquipoId();

    Instant getFecha();

    BigDecimal getHorometroAlRegistrar();

    String getNotas();
}

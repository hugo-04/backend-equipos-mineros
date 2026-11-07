package com.mineria.equipos.equipo.policy;

import java.math.BigDecimal;

/**
 * Decide si un equipo necesita mantenimiento. Está como interfaz para que la regla se pueda cambiar
 * (por calendario, por ejemplo) sin tocar los services. Hoy hay una sola implementación, la de horas.
 */
public interface MantenimientoPolicy {

    boolean requiereMantenimiento(BigDecimal horometro, BigDecimal horometroUltimoMant, int intervaloHoras);
}

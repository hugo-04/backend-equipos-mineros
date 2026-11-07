package com.mineria.equipos.equipo.policy;

import java.math.BigDecimal;

/**
 * Regla de negocio que decide si un equipo necesita mantenimiento.
 * Es una Strategy: hoy hay una sola implementación (por intervalo de horas), pero
 * si mañana se mide por calendario o por condición del equipo, se cambia solo la
 * implementación sin tocar los servicios.
 */
public interface MantenimientoPolicy {

    boolean requiereMantenimiento(BigDecimal horometro, BigDecimal horometroUltimoMant, int intervaloHoras);
}

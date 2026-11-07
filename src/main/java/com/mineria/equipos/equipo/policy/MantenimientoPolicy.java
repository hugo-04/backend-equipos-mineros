package com.mineria.equipos.equipo.policy;

import java.math.BigDecimal;

/**
 * Decide si un equipo necesita mantenimiento y cuánto falta. Está como interfaz para que la regla se pueda
 * cambiar (por calendario, por ejemplo) sin tocar los services ni el mapper. Hoy hay una sola implementación,
 * la de horas. Es el único lugar donde se hacen estas cuentas: la lista y el detalle usan lo mismo.
 */
public interface MantenimientoPolicy {

    /** Horas de uso desde el último mantenimiento: el horómetro menos el que había en ese momento. */
    BigDecimal horasDesdeUltimoMant(BigDecimal horometro, BigDecimal horometroUltimoMant);

    /** Horas que faltan para el próximo mantenimiento. Nunca es negativa: si ya se pasó, es 0. */
    BigDecimal horasRestantes(BigDecimal horometro, BigDecimal horometroUltimoMant, int intervaloHoras);

    boolean requiereMantenimiento(BigDecimal horometro, BigDecimal horometroUltimoMant, int intervaloHoras);
}

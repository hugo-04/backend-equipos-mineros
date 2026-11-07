package com.mineria.equipos.domain.rule;

import java.math.BigDecimal;

/**
 * Regla por defecto: se necesita mantenimiento cuando las horas trabajadas desde el
 * último mantenimiento alcanzan (o superan) el intervalo del tipo de equipo.
 * "Alcanzan" incluye el caso exacto: con 250 h de intervalo, 250 h ya requiere.
 */
public class IntervaloMantenimientoPolicy implements MantenimientoPolicy {

    @Override
    public boolean requiereMantenimiento(BigDecimal horometro, BigDecimal horometroUltimoMant, int intervaloHoras) {
        BigDecimal horasDesdeUltimoMant = horometro.subtract(horometroUltimoMant);
        return horasDesdeUltimoMant.compareTo(BigDecimal.valueOf(intervaloHoras)) >= 0;
    }
}

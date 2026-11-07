package com.mineria.equipos.equipo.policy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/**
 * Regla por defecto: se necesita mantenimiento cuando las horas trabajadas desde el
 * último mantenimiento alcanzan (o superan) el intervalo del tipo de equipo.
 * "Alcanzan" incluye el caso exacto: con 250 h de intervalo, 250 h ya requiere.
 */
@Component
public class IntervaloMantenimientoPolicy implements MantenimientoPolicy {

    @Override
    public boolean requiereMantenimiento(BigDecimal horometro, BigDecimal horometroUltimoMant, int intervaloHoras) {
        BigDecimal horasDesdeUltimoMant = horometro.subtract(horometroUltimoMant);
        return horasDesdeUltimoMant.compareTo(BigDecimal.valueOf(intervaloHoras)) >= 0;
    }
}

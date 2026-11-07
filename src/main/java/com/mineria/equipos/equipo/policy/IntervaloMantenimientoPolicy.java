package com.mineria.equipos.equipo.policy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/**
 * La regla: a un equipo le toca mantenimiento cuando las horas trabajadas desde el último mantenimiento
 * llegan al intervalo de su tipo. Llegar cuenta: con un intervalo de 250 h, a las 250 h exactas ya le toca.
 */
@Component
public class IntervaloMantenimientoPolicy implements MantenimientoPolicy {

    @Override
    public boolean requiereMantenimiento(BigDecimal horometro, BigDecimal horometroUltimoMant, int intervaloHoras) {
        BigDecimal horasDesdeUltimoMant = horometro.subtract(horometroUltimoMant);
        return horasDesdeUltimoMant.compareTo(BigDecimal.valueOf(intervaloHoras)) >= 0;
    }
}

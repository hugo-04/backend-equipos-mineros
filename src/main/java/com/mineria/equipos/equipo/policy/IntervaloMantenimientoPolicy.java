package com.mineria.equipos.equipo.policy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/**
 * La regla: a un equipo le toca mantenimiento cuando las horas trabajadas desde el último mantenimiento
 * llegan al intervalo de su tipo. Llegar cuenta: con un intervalo de 250 h, a las 250 h exactas ya le toca.
 * El estado nunca se guarda; se calcula en cada consulta con estos tres datos.
 */
@Component
public class IntervaloMantenimientoPolicy implements MantenimientoPolicy {

    @Override
    public BigDecimal horasDesdeUltimoMant(BigDecimal horometro, BigDecimal horometroUltimoMant) {
        return horometro.subtract(horometroUltimoMant);
    }

    @Override
    public BigDecimal horasRestantes(BigDecimal horometro, BigDecimal horometroUltimoMant, int intervaloHoras) {
        BigDecimal restantes = BigDecimal.valueOf(intervaloHoras)
                .subtract(horasDesdeUltimoMant(horometro, horometroUltimoMant));
        return restantes.max(BigDecimal.ZERO);
    }

    @Override
    public boolean requiereMantenimiento(BigDecimal horometro, BigDecimal horometroUltimoMant, int intervaloHoras) {
        return horasDesdeUltimoMant(horometro, horometroUltimoMant)
                .compareTo(BigDecimal.valueOf(intervaloHoras)) >= 0;
    }
}

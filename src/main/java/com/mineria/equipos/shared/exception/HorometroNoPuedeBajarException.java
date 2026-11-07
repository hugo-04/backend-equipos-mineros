package com.mineria.equipos.shared.exception;

import java.math.BigDecimal;

/**
 * El horómetro es un acumulado de horas de uso, como el odómetro de un auto: solo sube. Sale al editar un
 * equipo con un valor menor al que ya tiene.
 */
public class HorometroNoPuedeBajarException extends DomainException {

    public static final String CODIGO = "HOROMETRO_NO_PUEDE_BAJAR";

    public HorometroNoPuedeBajarException(BigDecimal actual, BigDecimal recibido) {
        super(CODIGO, "El horómetro no puede bajar: actual " + actual + ", recibido " + recibido);
    }
}

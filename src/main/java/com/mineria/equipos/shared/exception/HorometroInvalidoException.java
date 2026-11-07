package com.mineria.equipos.shared.exception;

/** El horómetro rompe una regla de negocio; el caso típico es querer bajarlo respecto al valor actual. */
public class HorometroInvalidoException extends DomainException {

    public static final String CODIGO = "HOROMETRO_INVALIDO";

    public HorometroInvalidoException(String mensaje) {
        super(CODIGO, mensaje);
    }
}

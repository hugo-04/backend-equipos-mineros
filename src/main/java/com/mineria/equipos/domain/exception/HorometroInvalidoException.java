package com.mineria.equipos.domain.exception;

/** Se lanza cuando un horómetro rompe una regla de negocio, por ejemplo al intentar bajarlo. */
public class HorometroInvalidoException extends DomainException {

    public static final String CODIGO = "HOROMETRO_INVALIDO";

    public HorometroInvalidoException(String mensaje) {
        super(CODIGO, mensaje);
    }
}

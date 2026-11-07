package com.mineria.equipos.domain.exception;

/** Se lanza cuando ya hay otro equipo con el mismo código. */
public class CodigoDuplicadoException extends DomainException {

    public static final String CODIGO = "CODIGO_DUPLICADO";

    public CodigoDuplicadoException(String codigoEquipo) {
        super(CODIGO, "Ya existe un equipo con el código " + codigoEquipo);
    }
}

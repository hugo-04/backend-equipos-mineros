package com.mineria.equipos.shared.exception;

/** El código del equipo es único: esto sale cuando ya hay otro equipo con ese mismo código. */
public class CodigoDuplicadoException extends DomainException {

    public static final String CODIGO = "CODIGO_DUPLICADO";

    public CodigoDuplicadoException(String codigoEquipo) {
        super(CODIGO, "Ya existe un equipo con el código " + codigoEquipo);
    }
}

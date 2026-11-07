package com.mineria.equipos.domain.exception;

import lombok.Getter;

/**
 * Base de todas las excepciones de negocio del servicio.
 * Cada una lleva un código interno estable (por ejemplo EQUIPO_NO_ENCONTRADO) que
 * la capa web usa para armar la respuesta de error sin que el dominio sepa de HTTP.
 */
@Getter
public abstract class DomainException extends RuntimeException {

    private final String codigo;

    protected DomainException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }
}

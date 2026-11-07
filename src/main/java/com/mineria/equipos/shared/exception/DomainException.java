package com.mineria.equipos.shared.exception;

import lombok.Getter;

/**
 * Padre de las excepciones de negocio. Cada una trae un código estable (por ejemplo EQUIPO_NO_ENCONTRADO)
 * y el GlobalExceptionHandler lo usa para armar la respuesta HTTP, así los services no tienen que saber
 * nada de HTTP.
 */
@Getter
public abstract class DomainException extends RuntimeException {

    private final String codigo;

    protected DomainException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }
}

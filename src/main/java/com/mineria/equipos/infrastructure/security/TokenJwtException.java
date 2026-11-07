package com.mineria.equipos.infrastructure.security;

import org.springframework.security.core.AuthenticationException;

import lombok.Getter;

/**
 * El token JWT no se pudo aceptar. Lleva un código para distinguir por qué
 * (TOKEN_EXPIRADO o TOKEN_INVALIDO) y poder explicárselo al cliente.
 */
@Getter
public class TokenJwtException extends AuthenticationException {

    public static final String TOKEN_EXPIRADO = "TOKEN_EXPIRADO";
    public static final String TOKEN_INVALIDO = "TOKEN_INVALIDO";

    private final String codigo;

    public TokenJwtException(String codigo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigo = codigo;
    }
}

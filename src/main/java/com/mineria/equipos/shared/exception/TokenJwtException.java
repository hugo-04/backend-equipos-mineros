package com.mineria.equipos.shared.exception;

import org.springframework.security.core.AuthenticationException;

import lombok.Getter;

/**
 * El token JWT no se aceptó. El código dice por qué (TOKEN_EXPIRADO o TOKEN_INVALIDO) para que el
 * cliente sepa si le toca volver a iniciar sesión.
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

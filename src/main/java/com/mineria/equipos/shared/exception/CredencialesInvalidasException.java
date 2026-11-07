package com.mineria.equipos.shared.exception;

/**
 * Usuario o contraseña incorrectos. El mensaje es el mismo en los dos casos a propósito: así nadie puede
 * probar nombres de usuario para ver cuáles existen.
 */
public class CredencialesInvalidasException extends DomainException {

    public static final String CODIGO = "CREDENCIALES_INVALIDAS";

    public CredencialesInvalidasException() {
        super(CODIGO, "Usuario o contraseña incorrectos");
    }
}

package com.mineria.equipos.shared.exception;

/**
 * Se lanza cuando el usuario o la contraseña no coinciden.
 * El mensaje es genérico a propósito: no revela si lo que falló fue el usuario o la clave.
 */
public class CredencialesInvalidasException extends DomainException {

    public static final String CODIGO = "CREDENCIALES_INVALIDAS";

    public CredencialesInvalidasException() {
        super(CODIGO, "Usuario o contraseña incorrectos");
    }
}

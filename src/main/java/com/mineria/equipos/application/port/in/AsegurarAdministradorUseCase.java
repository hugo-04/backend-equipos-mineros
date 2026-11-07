package com.mineria.equipos.application.port.in;

/** Caso de uso: garantizar que exista el usuario administrador inicial. */
public interface AsegurarAdministradorUseCase {

    /** @return true si lo creó, false si ya existía. */
    boolean asegurar(String username, String password);
}

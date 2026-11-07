package com.mineria.equipos.application.port.in;

import com.mineria.equipos.application.view.SesionView;

/** Caso de uso: iniciar sesión con usuario y contraseña. */
public interface AutenticarUsuarioUseCase {

    SesionView autenticar(Comando comando);

    record Comando(String username, String password) {
    }
}

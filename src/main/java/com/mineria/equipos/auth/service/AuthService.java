package com.mineria.equipos.auth.service;

import com.mineria.equipos.generated.model.LoginRequest;
import com.mineria.equipos.generated.model.LoginResponse;

/** Servicio de autenticación: inicia sesión y garantiza que exista el administrador inicial. */
public interface AuthService {

    /** @throws com.mineria.equipos.shared.exception.CredencialesInvalidasException si usuario o clave no coinciden */
    LoginResponse login(LoginRequest request);

    /** @return true si creó el administrador, false si ya existía */
    boolean asegurarAdministrador(String username, String password);
}

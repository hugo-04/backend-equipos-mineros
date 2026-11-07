package com.mineria.equipos.domain.model;

import lombok.Builder;
import lombok.Value;

/** Usuario que puede iniciar sesión. Nunca guarda la contraseña en claro, solo su hash BCrypt. */
@Value
@Builder(toBuilder = true)
public class Usuario {

    Long id;
    String username;
    String passwordHash;
    Rol rol;
}

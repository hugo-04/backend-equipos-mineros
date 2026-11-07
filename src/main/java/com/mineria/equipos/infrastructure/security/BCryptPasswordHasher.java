package com.mineria.equipos.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mineria.equipos.application.port.out.PasswordHasherPort;

import lombok.RequiredArgsConstructor;

/** Implementa el puerto de contraseñas con BCrypt (a través del PasswordEncoder de Spring Security). */
@Component
@RequiredArgsConstructor
public class BCryptPasswordHasher implements PasswordHasherPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hashear(String passwordEnClaro) {
        return passwordEncoder.encode(passwordEnClaro);
    }

    @Override
    public boolean coincide(String passwordEnClaro, String hash) {
        return passwordEncoder.matches(passwordEnClaro, hash);
    }
}

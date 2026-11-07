package com.mineria.equipos.auth.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.mineria.equipos.auth.service.AuthService;
import com.mineria.equipos.shared.config.AppProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Al arrancar crea el administrador inicial con ADMIN_USER y ADMIN_PASSWORD del entorno.
 * Si no están definidos no falla: avisa en el log (sin ese usuario nadie podrá iniciar sesión).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdministradorInicialRunner implements ApplicationRunner {

    private final AuthService authService;
    private final AppProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = properties.admin();
        if (!admin.estaConfigurado()) {
            log.warn("ADMIN_USER y ADMIN_PASSWORD no están definidos: no se crea el administrador inicial");
            return;
        }
        authService.asegurarAdministrador(admin.username(), admin.password());
    }
}

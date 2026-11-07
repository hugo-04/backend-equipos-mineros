package com.mineria.equipos.infrastructure.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.mineria.equipos.application.port.in.AsegurarAdministradorUseCase;
import com.mineria.equipos.infrastructure.config.AppProperties;

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

    private final AsegurarAdministradorUseCase asegurarAdministrador;
    private final AppProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = properties.admin();
        if (!admin.estaConfigurado()) {
            log.warn("ADMIN_USER y ADMIN_PASSWORD no están definidos: no se crea el administrador inicial");
            return;
        }
        asegurarAdministrador.asegurar(admin.username(), admin.password());
    }
}

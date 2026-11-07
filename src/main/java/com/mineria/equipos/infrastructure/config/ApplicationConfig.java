package com.mineria.equipos.infrastructure.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mineria.equipos.domain.rule.IntervaloMantenimientoPolicy;
import com.mineria.equipos.domain.rule.MantenimientoPolicy;

/**
 * Cableado de las piezas que no son componentes de Spring por sí mismas: el reloj
 * (inyectable para poder fijar la hora en las pruebas) y la política de mantenimiento del dominio,
 * que se mantiene libre de anotaciones de Spring.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public MantenimientoPolicy mantenimientoPolicy() {
        return new IntervaloMantenimientoPolicy();
    }
}

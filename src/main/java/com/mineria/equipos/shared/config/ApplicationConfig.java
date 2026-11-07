package com.mineria.equipos.shared.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans que usan todos los módulos: las propiedades del servicio y el reloj.
 * El reloj se inyecta para poder congelar la hora en las pruebas.
 */
@Configuration
@EnableConfigurationProperties({AppProperties.class, SecurityProperties.class})
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

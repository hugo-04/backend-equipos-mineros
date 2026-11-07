package com.mineria.equipos.shared.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

/**
 * Deja que un frontend en otro dominio llame a la API. Los orígenes vienen de CORS_ALLOWED_ORIGINS.
 * Los métodos y los headers los listo uno por uno a propósito (nada de "*"); Authorization va
 * porque por ahí viaja el token.
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private static final long MAX_AGE_SEGUNDOS = 3600;

    private final AppProperties properties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(properties.cors().allowedOrigins());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Trace-Id"));
        configuracion.setExposedHeaders(List.of("Location", "X-Trace-Id"));
        configuracion.setMaxAge(MAX_AGE_SEGUNDOS);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}

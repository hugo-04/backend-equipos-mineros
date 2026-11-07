package com.mineria.equipos.shared.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

/**
 * CORS para que un frontend en otro origen pueda llamar a la API. Los orígenes salen de
 * CORS_ALLOWED_ORIGINS; métodos y headers están listados a propósito (nada de "*"),
 * incluyendo Authorization para el token.
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
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setExposedHeaders(List.of("Location"));
        configuracion.setMaxAge(MAX_AGE_SEGUNDOS);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}

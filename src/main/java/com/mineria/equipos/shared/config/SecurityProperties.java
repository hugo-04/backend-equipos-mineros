package com.mineria.equipos.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * Los límites de protección (prefijo "app.security"). Los valores por defecto ya sirven para producción;
 * se pueden cambiar con variables de entorno, por ejemplo para apretarlos en una prueba.
 *
 * @param loginMaxIntentos      intentos de login fallidos permitidos por IP dentro de la ventana
 * @param loginVentanaSegundos  duración de la ventana de conteo (y del bloqueo) en segundos
 * @param cuerpoMaxBytes        tamaño máximo del cuerpo de una petición, en bytes
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @DefaultValue("10") @Min(1) int loginMaxIntentos,
        @DefaultValue("300") @Min(1) int loginVentanaSegundos,
        @DefaultValue("16384") @Min(1) long cuerpoMaxBytes) {
}

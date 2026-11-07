package com.mineria.equipos.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Configuración propia del servicio (prefijo "app"), leída del entorno.
 * Se valida al arrancar: si falta el secreto JWT o es muy corto, la app no levanta
 * en vez de fallar más tarde con un error difícil de entender.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String version,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Admin admin,
        @Valid @NotNull Cors cors) {

    /** El secreto HS256 necesita al menos 32 caracteres (256 bits). */
    public record Jwt(@NotBlank @Size(min = 32) String secret, @Min(1) long expirationMinutes) {
    }

    /** Credenciales del administrador inicial. Si vienen vacías no se crea ninguno. */
    public record Admin(String username, String password) {

        public boolean estaConfigurado() {
            return username != null && !username.isBlank() && password != null && !password.isBlank();
        }
    }

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }
}

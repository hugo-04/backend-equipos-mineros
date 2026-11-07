package com.mineria.equipos.shared.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Configuración propia del servicio (prefijo "app"), leída del entorno.
 * Se valida al arrancar: si falta el secreto JWT, es muy corto o es un valor de ejemplo (igual que la clave del
 * administrador), la app no levanta en vez de quedar protegida por una clave adivinable.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String version,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Admin admin,
        @Valid @NotNull Cors cors) {

    /**
     * El secreto HS256 necesita al menos 32 caracteres (256 bits) y no puede ser un valor de ejemplo:
     * si lo es, la aplicación no arranca.
     */
    public record Jwt(@NotBlank @Size(min = 32) String secret, @Min(1) long expirationMinutes) {

        @AssertTrue(message = "JWT_SECRET es débil o de ejemplo: usa un valor aleatorio de 32+ caracteres "
                + "(por ejemplo, openssl rand -hex 32)")
        public boolean isSecretoFuerte() {
            return !SecretosDebiles.esDebil(secret);
        }
    }

    /** Credenciales del administrador inicial. Si vienen vacías no se crea ninguno. */
    public record Admin(String username, String password) {

        private static final int LARGO_MINIMO_PASSWORD = 12;

        public boolean estaConfigurado() {
            return username != null && !username.isBlank() && password != null && !password.isBlank();
        }

        @AssertTrue(message = "ADMIN_PASSWORD es débil o de ejemplo: usa 12+ caracteres aleatorios "
                + "(por ejemplo, openssl rand -base64 18)")
        public boolean isPasswordFuerte() {
            return !estaConfigurado() || (password.length() >= LARGO_MINIMO_PASSWORD && !SecretosDebiles.esDebil(password));
        }
    }

    /**
     * Orígenes permitidos por CORS: URLs completas (http o https, con host y puerto opcional), sin "/" al final y
     * sin comodines. Un "*" o un valor mal escrito hace fallar el arranque en lugar de abrir la API a cualquiera.
     */
    public record Cors(@NotEmpty List<@Pattern(regexp = "^https?://[A-Za-z0-9.-]+(:[0-9]{1,5})?$",
            message = "CORS_ALLOWED_ORIGINS solo admite URLs http(s) completas, sin comodines ni '/' al final") String> allowedOrigins) {
    }
}

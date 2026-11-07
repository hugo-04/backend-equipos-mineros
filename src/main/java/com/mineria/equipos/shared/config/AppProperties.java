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
 * Lo que la app lee del entorno con el prefijo "app": versión, JWT, admin inicial y CORS.
 * Todo se valida al arrancar. Prefiero que la app no levante a que quede protegida por un
 * secreto de ejemplo que cualquiera podría adivinar.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String version,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Admin admin,
        @Valid @NotNull Cors cors) {

    /**
     * HS256 pide mínimo 32 caracteres (256 bits). Además se rechazan los valores de ejemplo, para que
     * nadie despliegue con el secreto que viene en la plantilla del .env.
     */
    public record Jwt(@NotBlank @Size(min = 32) String secret, @Min(1) long expirationMinutes) {

        @AssertTrue(message = "JWT_SECRET es débil o de ejemplo: usa un valor aleatorio de 32+ caracteres "
                + "(por ejemplo, openssl rand -hex 32)")
        public boolean isSecretoFuerte() {
            return !SecretosDebiles.esDebil(secret);
        }
    }

    /** El admin que se crea en el primer arranque. Si no se configura, simplemente no se crea ninguno. */
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
     * Orígenes que pueden llamar a la API desde un navegador. Tienen que ser URLs completas (http o https),
     * sin "/" al final y sin comodines: si alguien pone "*" o lo escribe mal, la app no arranca en vez de
     * quedar abierta a cualquier sitio.
     */
    public record Cors(@NotEmpty List<@Pattern(regexp = "^https?://[A-Za-z0-9.-]+(:[0-9]{1,5})?$",
            message = "CORS_ALLOWED_ORIGINS solo admite URLs http(s) completas, sin comodines ni '/' al final") String> allowedOrigins) {
    }
}

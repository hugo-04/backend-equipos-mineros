package com.mineria.equipos.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Comprueba el arranque con la configuración real: con secretos de ejemplo o débiles el contexto de Spring
 * NO levanta (y el mensaje explica qué corregir sin imprimir el secreto).
 */
class ArranqueConSecretosDebilesTest {

    private static final String JWT_FUERTE = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
    private static final String ADMIN_FUERTE = "Qp7vX2mLk9Wz4RtB";

    @Configuration
    @EnableConfigurationProperties(AppProperties.class)
    static class Config {
    }

    private ApplicationContextRunner arranque(String jwt, String adminPassword) {
        return new ApplicationContextRunner()
                .withUserConfiguration(Config.class)
                .withPropertyValues(
                        "app.version=test",
                        "app.jwt.secret=" + jwt,
                        "app.jwt.expiration-minutes=30",
                        "app.admin.username=admin",
                        "app.admin.password=" + adminPassword,
                        "app.cors.allowed-origins=http://localhost:3000");
    }

    @Test
    @DisplayName("con secretos fuertes la configuración carga")
    void conSecretosFuertes() {
        arranque(JWT_FUERTE, ADMIN_FUERTE).run(contexto -> assertThat(contexto).hasNotFailed());
    }

    @Test
    @DisplayName("rechaza el JWT_SECRET de ejemplo del .env anterior")
    void rechazaJwtDeEjemplo() {
        arranque("cambia-este-secreto-por-uno-largo-y-aleatorio-de-32-o-mas", ADMIN_FUERTE).run(contexto -> {
            assertThat(contexto).hasFailed();
            assertThat(contexto.getStartupFailure()).hasStackTraceContaining("JWT_SECRET es débil");
            assertThat(contexto.getStartupFailure().getMessage()).doesNotContain(JWT_FUERTE);
        });
    }

    @Test
    @DisplayName("rechaza un JWT_SECRET de menos de 32 caracteres")
    void rechazaJwtCorto() {
        arranque("Kx39Qm2vT7pLw8Zr", ADMIN_FUERTE).run(contexto -> assertThat(contexto).hasFailed());
    }

    @Test
    @DisplayName("rechaza un JWT_SECRET repetitivo")
    void rechazaJwtRepetitivo() {
        arranque("a".repeat(40), ADMIN_FUERTE).run(contexto -> assertThat(contexto).hasFailed());
    }

    @Test
    @DisplayName("rechaza la ADMIN_PASSWORD de ejemplo")
    void rechazaAdminDeEjemplo() {
        arranque(JWT_FUERTE, "cambia-esta-clave-de-admin").run(contexto -> {
            assertThat(contexto).hasFailed();
            assertThat(contexto.getStartupFailure()).hasStackTraceContaining("ADMIN_PASSWORD es débil");
        });
    }

    @Test
    @DisplayName("rechaza una ADMIN_PASSWORD de menos de 12 caracteres")
    void rechazaAdminCorta() {
        arranque(JWT_FUERTE, "Qp7vX2mLk9W").run(contexto -> assertThat(contexto).hasFailed());
    }

    @Test
    @DisplayName("sin ADMIN_PASSWORD no se crea admin y la configuración sigue siendo válida")
    void sinAdminEsValido() {
        arranque(JWT_FUERTE, "").run(contexto -> assertThat(contexto).hasNotFailed());
    }
}

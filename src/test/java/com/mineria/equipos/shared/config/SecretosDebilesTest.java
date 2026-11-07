package com.mineria.equipos.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Qué secretos y contraseñas se consideran de ejemplo o demasiado simples.
 */
class SecretosDebilesTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" es débil")
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "cambia-este-secreto-por-uno-largo-y-aleatorio-de-32-o-mas",
            "cambia-esta-clave-de-admin",
            "ChangeMe-ChangeMe-ChangeMe-ChangeMe",
            "mi-password-super-segura-123",
            "ejemplo-de-secreto-que-no-sirve-para-nada",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "abababababababababababababababab",
            "admin1234567890-default"})
    void detectaSecretosDebiles(String secreto) {
        // Act + Assert
        assertThat(SecretosDebiles.esDebil(secreto)).isTrue();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" es fuerte")
    @ValueSource(strings = {
            "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
            "K3x9Qm2vT7pLw8Zr5Nd1Hb6YcA4sJf0e",
            "secreto-solo-para-pruebas-de-al-menos-32-caracteres",
            "clave-de-prueba-1"})
    void aceptaSecretosFuertes(String secreto) {
        // Act + Assert
        assertThat(SecretosDebiles.esDebil(secreto)).isFalse();
    }

    @Test
    @DisplayName("no distingue mayúsculas al buscar palabras de ejemplo")
    void ignoraMayusculas() {
        // Act + Assert
        assertThat(SecretosDebiles.esDebil("QWERTY-Zk39-Pd81-Lm52-Vb07")).isTrue();
    }
}

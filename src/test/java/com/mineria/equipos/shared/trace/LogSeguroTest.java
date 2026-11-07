package com.mineria.equipos.shared.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Que lo que escribe el usuario se limpie (saltos de línea, tabs, textos largos, null) antes de llegar a un log.
 */
class LogSeguroTest {

    @Test
    @DisplayName("un texto normal queda igual")
    void textoNormal() {
        // Act + Assert
        assertThat(LogSeguro.limpiar("admin")).isEqualTo("admin");
    }

    @Test
    @DisplayName("cambia saltos de línea, retornos y tabulaciones para que no puedan fabricar líneas de log falsas")
    void quitaCaracteresDeControl() {
        // Act
        String limpio = LogSeguro.limpiar("admin\r\n2026-01-01 INFO Login correcto: usuario=root\tfalso");

        // Assert
        assertThat(limpio).doesNotContain("\r").doesNotContain("\n").doesNotContain("\t");
        assertThat(limpio).startsWith("admin__2026-01-01");
    }

    @Test
    @DisplayName("recorta los textos largos")
    void recortaTextosLargos() {
        // Act
        String limpio = LogSeguro.limpiar("a".repeat(500));

        // Assert
        assertThat(limpio).hasSize(83).endsWith("...");
    }

    @Test
    @DisplayName("null se escribe como 'null' sin romperse")
    void nulo() {
        // Act + Assert
        assertThat(LogSeguro.limpiar(null)).isEqualTo("null");
    }
}

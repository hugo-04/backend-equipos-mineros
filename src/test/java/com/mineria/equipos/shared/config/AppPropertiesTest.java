package com.mineria.equipos.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las validaciones de la configuración al arrancar, empezando por cuándo se considera configurado al administrador.
 */
class AppPropertiesTest {

    @Test
    @DisplayName("Admin.estaConfigurado: exige usuario y clave no vacíos")
    void estaConfigurado() {
        // Act + Assert
        assertThat(new AppProperties.Admin("a", "b").estaConfigurado()).isTrue();
        assertThat(new AppProperties.Admin(null, "b").estaConfigurado()).isFalse();
        assertThat(new AppProperties.Admin("a", null).estaConfigurado()).isFalse();
        assertThat(new AppProperties.Admin(" ", "b").estaConfigurado()).isFalse();
        assertThat(new AppProperties.Admin("a", "  ").estaConfigurado()).isFalse();
    }
}

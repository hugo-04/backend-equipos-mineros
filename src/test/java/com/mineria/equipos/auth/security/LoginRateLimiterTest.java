package com.mineria.equipos.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mineria.equipos.shared.config.SecurityProperties;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.testsupport.RelojMutable;

class LoginRateLimiterTest {

    private RelojMutable reloj;
    private LoginRateLimiter limitador;

    @BeforeEach
    void setUp() {
        reloj = new RelojMutable(Datos.AHORA);
        // 3 fallos permitidos en una ventana de 60 segundos
        limitador = new LoginRateLimiter(new SecurityProperties(3, 60, 16384), reloj);
    }

    @Test
    @DisplayName("no bloquea a un cliente que no ha fallado")
    void sinFallos() {
        // Act + Assert
        assertThat(limitador.estaBloqueado("1.1.1.1")).isFalse();
    }

    @Test
    @DisplayName("bloquea justo al llegar al máximo de fallos, no antes")
    void bloqueaAlLlegarAlMaximo() {
        // Act
        limitador.registrarFallo("1.1.1.1");
        limitador.registrarFallo("1.1.1.1");
        boolean antes = limitador.estaBloqueado("1.1.1.1");
        limitador.registrarFallo("1.1.1.1");

        // Assert
        assertThat(antes).isFalse();
        assertThat(limitador.estaBloqueado("1.1.1.1")).isTrue();
    }

    @Test
    @DisplayName("los clientes se cuentan por separado")
    void clientesIndependientes() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            limitador.registrarFallo("1.1.1.1");
        }

        // Act + Assert
        assertThat(limitador.estaBloqueado("1.1.1.1")).isTrue();
        assertThat(limitador.estaBloqueado("2.2.2.2")).isFalse();
    }

    @Test
    @DisplayName("el bloqueo se libera cuando los fallos salen de la ventana")
    void seLiberaConElTiempo() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            limitador.registrarFallo("1.1.1.1");
        }
        assertThat(limitador.estaBloqueado("1.1.1.1")).isTrue();

        // Act
        reloj.avanzar(Duration.ofSeconds(59));
        boolean casi = limitador.estaBloqueado("1.1.1.1");
        reloj.avanzar(Duration.ofSeconds(2));

        // Assert
        assertThat(casi).isTrue();
        assertThat(limitador.estaBloqueado("1.1.1.1")).isFalse();
    }

    @Test
    @DisplayName("un login correcto reinicia el conteo")
    void exitoReinicia() {
        // Arrange
        limitador.registrarFallo("1.1.1.1");
        limitador.registrarFallo("1.1.1.1");

        // Act
        limitador.registrarExito("1.1.1.1");
        limitador.registrarFallo("1.1.1.1");
        limitador.registrarFallo("1.1.1.1");

        // Assert: tras el éxito empezó de cero, así que 2 fallos no alcanzan el máximo
        assertThat(limitador.estaBloqueado("1.1.1.1")).isFalse();
    }

    @Test
    @DisplayName("segundosParaReintentar: dice cuánto falta para que el fallo más viejo salga de la ventana")
    void segundosParaReintentar() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            limitador.registrarFallo("1.1.1.1");
        }
        reloj.avanzar(Duration.ofSeconds(20));

        // Act
        long segundos = limitador.segundosParaReintentar("1.1.1.1");

        // Assert: 60 s de ventana - 20 s transcurridos (+1 de margen)
        assertThat(segundos).isBetween(40L, 41L);
        assertThat(limitador.segundosParaReintentar("sin-fallos")).isEqualTo(1);
    }

    @Test
    @DisplayName("limpia las entradas vencidas cuando el mapa crece mucho")
    void limpiaEntradasVencidas() {
        // Arrange: 10 000 clientes con un fallo que luego vence
        for (int i = 0; i < 10_000; i++) {
            limitador.registrarFallo("cliente-" + i);
        }
        reloj.avanzar(Duration.ofSeconds(120));

        // Act: un nuevo fallo dispara la limpieza
        limitador.registrarFallo("nuevo");

        // Assert: los viejos ya no cuentan y el nuevo sí
        assertThat(limitador.estaBloqueado("cliente-1")).isFalse();
        assertThat(limitador.segundosParaReintentar("nuevo")).isBetween(1L, 61L);
    }
}

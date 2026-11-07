package com.mineria.equipos.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.mineria.equipos.application.port.in.AsegurarAdministradorUseCase;
import com.mineria.equipos.shared.config.AppProperties;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ComponentesDeSeguridadTest {

    @Mock
    private AsegurarAdministradorUseCase asegurarAdministrador;

    private AppProperties conAdmin(String usuario, String clave) {
        return new AppProperties("test", new AppProperties.Jwt("x".repeat(32), 30),
                new AppProperties.Admin(usuario, clave), new AppProperties.Cors(List.of("http://localhost:3000")));
    }

    @Test
    @DisplayName("BCrypt: el hash no es la clave, y solo coincide con la clave correcta")
    void bcrypt() {
        // Arrange
        BCryptPasswordHasher hasher = new BCryptPasswordHasher(new BCryptPasswordEncoder());

        // Act
        String hash = hasher.hashear("mi-clave");

        // Assert
        assertThat(hash).isNotEqualTo("mi-clave").startsWith("$2");
        assertThat(hasher.coincide("mi-clave", hash)).isTrue();
        assertThat(hasher.coincide("otra-clave", hash)).isFalse();
    }

    @Test
    @DisplayName("Runner: crea el administrador con los datos del entorno")
    void creaAlAdministrador() {
        // Arrange
        AdministradorInicialRunner runner = new AdministradorInicialRunner(asegurarAdministrador, conAdmin("root", "clave-1"));

        // Act
        runner.run(new DefaultApplicationArguments());

        // Assert
        verify(asegurarAdministrador).asegurar("root", "clave-1");
    }

    @Test
    @DisplayName("Runner: sin ADMIN_USER / ADMIN_PASSWORD no crea nada y no falla")
    void sinCredenciales() {
        // Arrange
        AdministradorInicialRunner runner = new AdministradorInicialRunner(asegurarAdministrador, conAdmin("", ""));

        // Act
        runner.run(new DefaultApplicationArguments());

        // Assert
        verifyNoInteractions(asegurarAdministrador);
    }

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

    @Test
    @DisplayName("Runner: no llama al caso de uso si solo falta la contraseña")
    void faltaLaClave() {
        // Arrange
        AdministradorInicialRunner runner = new AdministradorInicialRunner(asegurarAdministrador, conAdmin("root", null));

        // Act
        runner.run(new DefaultApplicationArguments());

        // Assert
        verify(asegurarAdministrador, never()).asegurar(anyString(), anyString());
    }
}

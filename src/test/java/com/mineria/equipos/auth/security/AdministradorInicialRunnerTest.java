package com.mineria.equipos.auth.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import com.mineria.equipos.auth.service.AuthService;
import com.mineria.equipos.shared.config.AppProperties;

@ExtendWith(MockitoExtension.class)
class AdministradorInicialRunnerTest {

    @Mock
    private AuthService authService;

    private AppProperties conAdmin(String usuario, String clave) {
        return new AppProperties("test", new AppProperties.Jwt("x".repeat(32), 30),
                new AppProperties.Admin(usuario, clave), new AppProperties.Cors(List.of("http://localhost:3000")));
    }

    @Test
    @DisplayName("crea el administrador con los datos del entorno")
    void creaAlAdministrador() {
        // Arrange
        AdministradorInicialRunner runner = new AdministradorInicialRunner(authService, conAdmin("root", "clave-1"));

        // Act
        runner.run(new DefaultApplicationArguments());

        // Assert
        verify(authService).asegurarAdministrador("root", "clave-1");
    }

    @Test
    @DisplayName("sin ADMIN_USER / ADMIN_PASSWORD no crea nada y no falla")
    void sinCredenciales() {
        // Arrange
        AdministradorInicialRunner runner = new AdministradorInicialRunner(authService, conAdmin("", ""));

        // Act
        runner.run(new DefaultApplicationArguments());

        // Assert
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("no llama al servicio si solo falta la contraseña")
    void faltaLaClave() {
        // Arrange
        AdministradorInicialRunner runner = new AdministradorInicialRunner(authService, conAdmin("root", null));

        // Act
        runner.run(new DefaultApplicationArguments());

        // Assert
        verify(authService, never()).asegurarAdministrador(anyString(), anyString());
    }
}

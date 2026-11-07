package com.mineria.equipos.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.application.port.in.AutenticarUsuarioUseCase.Comando;
import com.mineria.equipos.application.port.out.PasswordHasherPort;
import com.mineria.equipos.application.port.out.TokenProviderPort;
import com.mineria.equipos.application.port.out.UsuarioRepositoryPort;
import com.mineria.equipos.application.view.SesionView;
import com.mineria.equipos.domain.exception.CredencialesInvalidasException;
import com.mineria.equipos.testsupport.Datos;

@ExtendWith(MockitoExtension.class)
class AutenticarUsuarioServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private TokenProviderPort tokenProvider;

    @InjectMocks
    private AutenticarUsuarioService service;

    @Test
    @DisplayName("emite el token cuando usuario y contraseña coinciden")
    void loginCorrecto() {
        // Arrange
        SesionView sesion = SesionView.builder().accessToken("jwt").tokenType("Bearer").expiresIn(3600).build();
        when(usuarioRepository.buscarPorUsername("admin")).thenReturn(Optional.of(Datos.admin()));
        when(passwordHasher.coincide("secreto", "$2a$hash")).thenReturn(true);
        when(tokenProvider.emitir(Datos.admin())).thenReturn(sesion);

        // Act
        SesionView resultado = service.autenticar(new Comando("admin", "secreto"));

        // Assert
        assertThat(resultado).isSameAs(sesion);
    }

    @Test
    @DisplayName("rechaza una contraseña incorrecta con CredencialesInvalidasException")
    void passwordIncorrecta() {
        // Arrange
        when(usuarioRepository.buscarPorUsername("admin")).thenReturn(Optional.of(Datos.admin()));
        when(passwordHasher.coincide("mala", "$2a$hash")).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.autenticar(new Comando("admin", "mala")))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(tokenProvider, never()).emitir(any());
    }

    @Test
    @DisplayName("un usuario inexistente da exactamente el mismo error que una clave incorrecta")
    void usuarioInexistente() {
        // Arrange
        when(usuarioRepository.buscarPorUsername("fantasma")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.autenticar(new Comando("fantasma", "x")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Usuario o contraseña incorrectos");
    }

    @Test
    @DisplayName("relanza los errores inesperados sin tragárselos")
    void errorInesperado() {
        // Arrange
        when(usuarioRepository.buscarPorUsername("admin")).thenThrow(new IllegalStateException("BD caída"));

        // Act + Assert
        assertThatThrownBy(() -> service.autenticar(new Comando("admin", "x")))
                .isInstanceOf(IllegalStateException.class);
    }
}

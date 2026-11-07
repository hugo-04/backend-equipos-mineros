package com.mineria.equipos.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.auth.entity.Usuario;
import com.mineria.equipos.auth.mapper.AuthMapperImpl;
import com.mineria.equipos.auth.repository.UsuarioRepository;
import com.mineria.equipos.auth.security.JwtService;
import com.mineria.equipos.generated.model.LoginRequest;
import com.mineria.equipos.generated.model.LoginResponse;
import com.mineria.equipos.shared.config.AppProperties;
import com.mineria.equipos.shared.exception.CredencialesInvalidasException;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.testsupport.Tokens;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private UsuarioRepository usuarioRepository;

    // La codificación y el JWT son los reales: así se prueba que la clave se compara contra su hash BCrypt
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        AppProperties propiedades = new AppProperties("test", new AppProperties.Jwt(Tokens.SECRETO, 30),
                new AppProperties.Admin("a", "b"), new AppProperties.Cors(List.of("http://localhost:3000")));
        JwtService jwtService = new JwtService(propiedades, Clock.fixed(Datos.AHORA, ZoneOffset.UTC));
        service = new AuthServiceImpl(userDetailsService, usuarioRepository, passwordEncoder, jwtService, new AuthMapperImpl());
    }

    private void usuarioExistente(String username, String passwordEnClaro) {
        when(userDetailsService.loadUserByUsername(username)).thenReturn(
                User.withUsername(username).password(passwordEncoder.encode(passwordEnClaro)).roles("ADMIN").build());
    }

    // ---------- login ----------

    @Test
    @DisplayName("login: emite un token Bearer cuando usuario y contraseña coinciden")
    void loginCorrecto() {
        // Arrange
        usuarioExistente("admin", "secreto");

        // Act
        LoginResponse respuesta = service.login(new LoginRequest("admin", "secreto"));

        // Assert
        assertThat(respuesta.getTokenType()).isEqualTo("Bearer");
        assertThat(respuesta.getExpiresIn()).isEqualTo(30 * 60L);
        assertThat(respuesta.getAccessToken().split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("login: rechaza una contraseña incorrecta con CredencialesInvalidasException")
    void passwordIncorrecta() {
        // Arrange
        usuarioExistente("admin", "secreto");

        // Act + Assert
        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "mala")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    @DisplayName("login: un usuario inexistente da exactamente el mismo error que una clave incorrecta")
    void usuarioInexistente() {
        // Arrange
        when(userDetailsService.loadUserByUsername("fantasma")).thenThrow(new UsernameNotFoundException("no existe"));

        // Act + Assert
        assertThatThrownBy(() -> service.login(new LoginRequest("fantasma", "x")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Usuario o contraseña incorrectos");
    }

    @Test
    @DisplayName("login: relanza los errores inesperados sin tragárselos")
    void errorInesperado() {
        // Arrange
        when(userDetailsService.loadUserByUsername("admin")).thenThrow(new IllegalStateException("BD caída"));

        // Act + Assert
        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "x")))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------- administrador inicial ----------

    @Test
    @DisplayName("asegurarAdministrador: crea el administrador con la clave como hash BCrypt, nunca en claro")
    void creaAlAdministrador() {
        // Arrange
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.empty());

        // Act
        boolean creado = service.asegurarAdministrador("admin", "clave");

        // Assert
        assertThat(creado).isTrue();
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.ADMIN);
        assertThat(guardado.getValue().getPasswordHash()).isNotEqualTo("clave").startsWith("$2");
        assertThat(passwordEncoder.matches("clave", guardado.getValue().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("asegurarAdministrador: si ya existe no lo toca (idempotente)")
    void noRecreaAlAdministrador() {
        // Arrange
        when(usuarioRepository.findByUsername("admin"))
                .thenReturn(Optional.of(Datos.usuario("admin", "$2a$hash", Rol.ADMIN)));

        // Act
        boolean creado = service.asegurarAdministrador("admin", "otra");

        // Assert
        assertThat(creado).isFalse();
        verify(usuarioRepository, never()).save(any());
    }
}

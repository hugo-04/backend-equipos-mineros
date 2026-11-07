package com.mineria.equipos.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.auth.entity.Usuario;
import com.mineria.equipos.auth.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsService service;

    @Test
    @DisplayName("carga el usuario con su hash y su rol como ROLE_*")
    void cargaElUsuario() {
        // Arrange
        when(usuarioRepository.findByUsername("admin"))
                .thenReturn(Optional.of(Usuario.nuevo("admin", "$2a$hash", Rol.ADMIN)));

        // Act
        UserDetails detalles = service.loadUserByUsername("admin");

        // Assert
        assertThat(detalles.getUsername()).isEqualTo("admin");
        assertThat(detalles.getPassword()).isEqualTo("$2a$hash");
        assertThat(detalles.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("lanza UsernameNotFoundException si el usuario no existe")
    void usuarioInexistente() {
        // Arrange
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.loadUserByUsername("nadie")).isInstanceOf(UsernameNotFoundException.class);
    }
}

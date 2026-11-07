package com.mineria.equipos.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.auth.entity.Usuario;
import com.mineria.equipos.testsupport.PersistenciaTest;

@PersistenciaTest
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    @Test
    @DisplayName("guarda y busca por username")
    void guardaYBusca() {
        // Arrange
        Usuario nuevo = Usuario.nuevo("operador1", "$2a$hash", Rol.OPERADOR);

        // Act
        Usuario guardado = repository.saveAndFlush(nuevo);
        Optional<Usuario> encontrado = repository.findByUsername("operador1");
        Optional<Usuario> inexistente = repository.findByUsername("nadie");

        // Assert
        assertThat(guardado.getId()).isNotNull();
        assertThat(encontrado).get().extracting(Usuario::getRol).isEqualTo(Rol.OPERADOR);
        assertThat(encontrado.get().getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(inexistente).isEmpty();
    }

    @Test
    @DisplayName("BD: el username es único")
    void usernameUnico() {
        // Arrange
        repository.saveAndFlush(Usuario.nuevo("repetido", "$2a$h1", Rol.ADMIN));

        // Act + Assert
        assertThatThrownBy(() -> repository.saveAndFlush(Usuario.nuevo("repetido", "$2a$h2", Rol.OPERADOR)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

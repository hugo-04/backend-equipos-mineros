package com.mineria.equipos.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.domain.model.Usuario;
import com.mineria.equipos.testsupport.PersistenciaTest;

/** Adaptador de usuarios (transitorio: pasa a repositorio en el módulo auth). */
@PersistenciaTest
class OtrosAdaptadoresPersistenciaTest {

    @Autowired
    private UsuarioPersistenceAdapter usuarios;

    @Test
    @DisplayName("usuarios: guarda y busca por username")
    void usuarios() {
        // Arrange
        Usuario nuevo = Usuario.builder().username("operador1").passwordHash("$2a$hash").rol(Rol.OPERADOR).build();

        // Act
        Usuario guardado = usuarios.guardar(nuevo);
        Optional<Usuario> encontrado = usuarios.buscarPorUsername("operador1");
        Optional<Usuario> inexistente = usuarios.buscarPorUsername("nadie");

        // Assert
        assertThat(guardado.getId()).isNotNull();
        assertThat(encontrado).get().extracting(Usuario::getRol).isEqualTo(Rol.OPERADOR);
        assertThat(inexistente).isEmpty();
    }
}

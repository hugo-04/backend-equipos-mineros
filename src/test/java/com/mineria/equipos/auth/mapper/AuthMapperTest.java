package com.mineria.equipos.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.auth.entity.Usuario;
import com.mineria.equipos.generated.model.LoginResponse;

class AuthMapperTest {

    private final AuthMapper mapper = new AuthMapperImpl();

    @Test
    @DisplayName("arma la respuesta del login con el token, el tipo y la duración")
    void loginResponse() {
        // Act
        LoginResponse response = mapper.toLoginResponse("jwt", "Bearer", 3600L);

        // Assert
        assertThat(response.getAccessToken()).isEqualTo("jwt");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("toEntity: arma el usuario con su hash y su rol, sin id")
    void usuario() {
        // Act
        Usuario usuario = mapper.toEntity("admin", "$2a$hash", Rol.ADMIN);

        // Assert
        assertThat(usuario.getId()).isNull();
        assertThat(usuario.getUsername()).isEqualTo("admin");
        assertThat(usuario.getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(usuario.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(mapper.toEntity(null, null, null)).isNull();
    }

    @Test
    @DisplayName("sin datos devuelve null")
    void nulos() {
        // Act + Assert
        assertThat(mapper.toLoginResponse(null, null, null)).isNull();
    }
}

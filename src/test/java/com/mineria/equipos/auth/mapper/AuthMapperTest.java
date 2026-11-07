package com.mineria.equipos.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("sin datos devuelve null")
    void nulos() {
        // Act + Assert
        assertThat(mapper.toLoginResponse(null, null, null)).isNull();
    }
}

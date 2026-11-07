package com.mineria.equipos.infrastructure.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.mineria.equipos.application.port.in.AutenticarUsuarioUseCase;
import com.mineria.equipos.application.view.SesionView;
import com.mineria.equipos.generated.model.LoginRequest;
import com.mineria.equipos.generated.model.LoginResponse;
import com.mineria.equipos.testsupport.Datos;

@SpringJUnitConfig(AuthWebMapperImpl.class)
class MappersWebTest {

    @Autowired
    private AuthWebMapper authMapper;

    @Test
    @DisplayName("auth: convierte el login y la sesión")
    void auth() {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "clave");
        SesionView sesion = SesionView.builder().accessToken("jwt").tokenType("Bearer").expiresIn(3600).build();

        // Act
        AutenticarUsuarioUseCase.Comando comando = authMapper.toComando(request);
        LoginResponse response = authMapper.toResponse(sesion);

        // Assert
        assertThat(comando).isEqualTo(new AutenticarUsuarioUseCase.Comando("admin", "clave"));
        assertThat(response.getAccessToken()).isEqualTo("jwt");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("todos los mappers web devuelven null (o lista vacía) cuando reciben null")
    void nulos() {
        // Act + Assert
        assertThat(authMapper.toComando(null)).isNull();
        assertThat(authMapper.toResponse(null)).isNull();
    }
}

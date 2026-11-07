package com.mineria.equipos.infrastructure.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.mineria.equipos.application.port.in.AutenticarUsuarioUseCase;
import com.mineria.equipos.application.port.in.RegistrarMantenimientoUseCase;
import com.mineria.equipos.application.view.SesionView;
import com.mineria.equipos.generated.model.LoginRequest;
import com.mineria.equipos.generated.model.LoginResponse;
import com.mineria.equipos.generated.model.MantenimientoRequest;
import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.testsupport.Datos;

@SpringJUnitConfig({MantenimientoWebMapperImpl.class, AuthWebMapperImpl.class})
class MappersWebTest {

    @Autowired
    private MantenimientoWebMapper mantenimientoMapper;
    @Autowired
    private AuthWebMapper authMapper;

    @Test
    @DisplayName("mantenimiento: arma el comando con el id del equipo y las notas")
    void comandoDeMantenimiento() {
        // Arrange
        MantenimientoRequest request = new MantenimientoRequest().notas("Cambio de filtros");

        // Act
        RegistrarMantenimientoUseCase.Comando comando = mantenimientoMapper.toComando(9L, request);

        // Assert
        assertThat(comando).isEqualTo(new RegistrarMantenimientoUseCase.Comando(9L, "Cambio de filtros"));
        assertThat(mantenimientoMapper.toComando(9L, null).notas()).isNull();
        assertThat(mantenimientoMapper.toComando(null, null)).isNull();
        assertThat(mantenimientoMapper.toComando(null, request).notas()).isEqualTo("Cambio de filtros");
    }

    @Test
    @DisplayName("mantenimiento: convierte el view y las listas")
    void viewDeMantenimiento() {
        // Arrange
        var view = Datos.mantenimientoView(3L, 9L, "250.0");

        // Act
        MantenimientoResponse response = mantenimientoMapper.toResponse(view);

        // Assert
        assertThat(response.getEquipoId()).isEqualTo(9L);
        assertThat(response.getHorometroAlRegistrar()).isEqualByComparingTo("250.0");
        assertThat(mantenimientoMapper.toResponses(List.of(view))).hasSize(1);
    }

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
        assertThat(mantenimientoMapper.toResponse(null)).isNull();
        assertThat(mantenimientoMapper.toResponses(null)).isNull();
        assertThat(authMapper.toComando(null)).isNull();
        assertThat(authMapper.toResponse(null)).isNull();
    }
}

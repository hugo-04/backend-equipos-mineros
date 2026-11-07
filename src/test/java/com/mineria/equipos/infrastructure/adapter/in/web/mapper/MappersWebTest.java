package com.mineria.equipos.infrastructure.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.mineria.equipos.application.port.in.ActualizarEquipoUseCase;
import com.mineria.equipos.application.port.in.AutenticarUsuarioUseCase;
import com.mineria.equipos.application.port.in.CrearEquipoUseCase;
import com.mineria.equipos.application.port.in.RegistrarMantenimientoUseCase;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.application.view.EstadoSalud;
import com.mineria.equipos.application.view.SesionView;
import com.mineria.equipos.application.view.TipoEquipoView;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.EquipoRequest;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.EquipoResponse;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.HealthResponse;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.LoginRequest;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.LoginResponse;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.MantenimientoRequest;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.MantenimientoResponse;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.TipoEquipoResponse;
import com.mineria.equipos.testsupport.Datos;

@SpringJUnitConfig({EquipoWebMapperImpl.class, MantenimientoWebMapperImpl.class, AuthWebMapperImpl.class,
        HealthWebMapperImpl.class})
class MappersWebTest {

    @Autowired
    private EquipoWebMapper equipoMapper;
    @Autowired
    private MantenimientoWebMapper mantenimientoMapper;
    @Autowired
    private AuthWebMapper authMapper;
    @Autowired
    private HealthWebMapper healthMapper;

    @Test
    @DisplayName("equipo: el request se convierte en el comando de crear")
    void requestAComandoDeCrear() {
        // Arrange
        EquipoRequest request = new EquipoRequest("CAM-001", 1L, new BigDecimal("12.5"));

        // Act
        CrearEquipoUseCase.Comando comando = equipoMapper.toCrearComando(request);

        // Assert
        assertThat(comando).isEqualTo(new CrearEquipoUseCase.Comando("CAM-001", 1L, new BigDecimal("12.5")));
    }

    @Test
    @DisplayName("equipo: el id del path y el body forman el comando de actualizar")
    void requestAComandoDeActualizar() {
        // Arrange
        EquipoRequest request = new EquipoRequest("CAM-001", 2L, new BigDecimal("99.0"));

        // Act
        ActualizarEquipoUseCase.Comando comando = equipoMapper.toActualizarComando(7L, request);

        // Assert
        assertThat(comando).isEqualTo(new ActualizarEquipoUseCase.Comando(7L, "CAM-001", 2L, new BigDecimal("99.0")));
    }

    @Test
    @DisplayName("equipo: actualizar acepta solo el id o solo el body sin romperse")
    void actualizarConPartesNulas() {
        // Act + Assert
        assertThat(equipoMapper.toActualizarComando(null, null)).isNull();
        assertThat(equipoMapper.toActualizarComando(3L, null).id()).isEqualTo(3L);
        assertThat(equipoMapper.toActualizarComando(null, new EquipoRequest("A-1", 1L, BigDecimal.ONE)).codigo())
                .isEqualTo("A-1");
    }

    @Test
    @DisplayName("equipo: el view aplanado se expone con el objeto 'tipo' anidado")
    void viewAResponseConTipoAnidado() {
        // Arrange
        EquipoView view = Datos.equipoView(5L, "CAM-005", "260.0", "0", 250).toBuilder()
                .requiereMantenimiento(true).estado("Requiere mantenimiento").build();

        // Act
        EquipoResponse response = equipoMapper.toResponse(view);

        // Assert
        assertThat(response.getTipo().getId()).isEqualTo(1L);
        assertThat(response.getTipo().getNombre()).isEqualTo("Camión");
        assertThat(response.getTipo().getIntervaloHoras()).isEqualTo(250);
        assertThat(response.getRequiereMantenimiento()).isTrue();
        assertThat(response.getEstado()).isEqualTo("Requiere mantenimiento");
        assertThat(response.getCreadoEn()).isEqualTo(Datos.AHORA);
    }

    @Test
    @DisplayName("equipo: convierte listas de views y tipos")
    void listas() {
        // Arrange
        List<EquipoView> views = List.of(Datos.equipoView(1L, "A-1", "1", "0", 250));
        List<TipoEquipoView> tipos = List.of(TipoEquipoView.builder().id(1L).nombre("Camión").intervaloHoras(250).build());

        // Act
        List<EquipoResponse> equipos = equipoMapper.toResponses(views);
        List<TipoEquipoResponse> tiposResponse = equipoMapper.toTipoResponses(tipos);

        // Assert
        assertThat(equipos).hasSize(1);
        assertThat(tiposResponse).extracting(TipoEquipoResponse::getNombre).containsExactly("Camión");
    }

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
    @DisplayName("health: arma la respuesta con el estado y la versión")
    void health() {
        // Act
        HealthResponse up = healthMapper.toResponse(EstadoSalud.UP, "abc123");
        HealthResponse down = healthMapper.toResponse(EstadoSalud.DOWN, null);

        // Assert
        assertThat(up.getStatus()).isEqualTo(HealthResponse.StatusEnum.UP);
        assertThat(up.getVersion()).isEqualTo("abc123");
        assertThat(down.getStatus()).isEqualTo(HealthResponse.StatusEnum.DOWN);
        assertThat(healthMapper.toResponse(null, null)).isNull();
        assertThat(healthMapper.toResponse(null, "solo-version").getStatus()).isNull();
    }

    @Test
    @DisplayName("todos los mappers web devuelven null (o lista vacía) cuando reciben null")
    void nulos() {
        // Act + Assert
        assertThat(equipoMapper.toCrearComando(null)).isNull();
        assertThat(equipoMapper.toResponse((EquipoView) null)).isNull();
        assertThat(equipoMapper.toResponse((TipoEquipoView) null)).isNull();
        assertThat(equipoMapper.toResponses(null)).isNull();
        assertThat(equipoMapper.toTipoResponses(null)).isNull();
        assertThat(mantenimientoMapper.toResponse(null)).isNull();
        assertThat(mantenimientoMapper.toResponses(null)).isNull();
        assertThat(authMapper.toComando(null)).isNull();
        assertThat(authMapper.toResponse(null)).isNull();
    }
}

package com.mineria.equipos.shared.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.generated.model.HealthResponse;
import com.mineria.equipos.shared.config.AppProperties;

/**
 * El /health da UP solo si la conexión a la base es válida; si no se puede abrir o no es válida, da DOWN sin lanzar nada.
 */
@ExtendWith(MockitoExtension.class)
class HealthServiceImplTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection conexion;

    private HealthServiceImpl service;

    @BeforeEach
    void setUp() {
        AppProperties propiedades = new AppProperties("abc1234", new AppProperties.Jwt("x".repeat(32), 30),
                new AppProperties.Admin("a", "b"), new AppProperties.Cors(List.of("http://localhost:3000")));
        service = new HealthServiceImpl(dataSource, new HealthMapperImpl(), propiedades);
    }

    @Test
    @DisplayName("UP con la versión: la conexión es válida y se cierra")
    void disponible() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.isValid(2)).thenReturn(true);

        // Act
        HealthResponse respuesta = service.verificar();

        // Assert
        assertThat(respuesta.getStatus()).isEqualTo(HealthResponse.StatusEnum.UP);
        assertThat(respuesta.getVersion()).isEqualTo("abc1234");
        verify(conexion).close();
    }

    @Test
    @DisplayName("DOWN: la conexión existe pero no es válida")
    void conexionInvalida() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.isValid(2)).thenReturn(false);

        // Act + Assert
        assertThat(service.verificar().getStatus()).isEqualTo(HealthResponse.StatusEnum.DOWN);
    }

    @Test
    @DisplayName("DOWN: no se puede ni abrir la conexión (no lanza, responde DOWN)")
    void sinConexion() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new SQLException("Communications link failure"));

        // Act
        HealthResponse respuesta = service.verificar();

        // Assert
        assertThat(respuesta.getStatus()).isEqualTo(HealthResponse.StatusEnum.DOWN);
        assertThat(respuesta.getVersion()).isEqualTo("abc1234");
    }
}

package com.mineria.equipos.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaludBaseDatosAdapterTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection conexion;

    @InjectMocks
    private SaludBaseDatosAdapter adapter;

    @Test
    @DisplayName("disponible: la conexión es válida y se cierra")
    void disponible() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.isValid(2)).thenReturn(true);

        // Act
        boolean disponible = adapter.estaDisponible();

        // Assert
        assertThat(disponible).isTrue();
        verify(conexion).close();
    }

    @Test
    @DisplayName("no disponible: la conexión existe pero no es válida")
    void conexionInvalida() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.isValid(2)).thenReturn(false);

        // Act + Assert
        assertThat(adapter.estaDisponible()).isFalse();
    }

    @Test
    @DisplayName("no disponible: no se puede ni abrir la conexión (no lanza, devuelve false)")
    void sinConexion() throws SQLException {
        // Arrange
        when(dataSource.getConnection()).thenThrow(new SQLException("Communications link failure"));

        // Act + Assert
        assertThat(adapter.estaDisponible()).isFalse();
    }
}

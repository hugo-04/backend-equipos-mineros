package com.mineria.equipos.mantenimiento.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.testsupport.Datos;

class MantenimientoMapperTest {

    private final MantenimientoMapper mapper = new MantenimientoMapperImpl();

    @Test
    @DisplayName("proyección -> DTO conserva todos los campos")
    void proyeccionADto() {
        // Act
        MantenimientoResponse response = mapper.toResponse(Datos.mantenimientoProjection(3L, 9L, "250.0"));

        // Assert
        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getEquipoId()).isEqualTo(9L);
        assertThat(response.getHorometroAlRegistrar()).isEqualByComparingTo("250.0");
        assertThat(response.getNotas()).isEqualTo("Cambio de aceite");
        assertThat(response.getFecha()).isEqualTo(Datos.AHORA);
    }

    @Test
    @DisplayName("convierte listas y devuelve null cuando recibe null")
    void listasYNulos() {
        // Act + Assert
        assertThat(mapper.toResponses(List.of(Datos.mantenimientoProjection(1L, 9L, "250.0")))).hasSize(1);
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toResponses(null)).isNull();
    }
}

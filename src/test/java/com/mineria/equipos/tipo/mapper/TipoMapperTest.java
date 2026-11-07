package com.mineria.equipos.tipo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mineria.equipos.generated.model.TipoEquipoResponse;
import com.mineria.equipos.tipo.repository.projection.TipoProjection;

/**
 * Que el mapper pase la proyección del tipo al DTO.
 */
class TipoMapperTest {

    private final TipoMapper mapper = new TipoMapperImpl();

    private TipoProjection proyeccion(Integer intervalo) {
        return new TipoProjection() {
            public Long getId() { return 1L; }
            public String getNombre() { return "Camión"; }
            public Integer getIntervaloHoras() { return intervalo; }
        };
    }

    @Test
    @DisplayName("proyección -> DTO conserva id, nombre e intervalo")
    void proyeccionADto() {
        // Act
        TipoEquipoResponse respuesta = mapper.toResponse(proyeccion(250));

        // Assert
        assertThat(respuesta).isEqualTo(new TipoEquipoResponse(1L, "Camión", 250));
    }

    @Test
    @DisplayName("una proyección sin intervalo no rompe el mapeo (queda null)")
    void sinIntervalo() {
        // Act + Assert
        assertThat(mapper.toResponse(proyeccion(null)).getIntervaloHoras()).isNull();
    }

    @Test
    @DisplayName("convierte listas y devuelve null cuando recibe null")
    void listasYNulos() {
        // Act + Assert
        assertThat(mapper.toResponses(List.of(proyeccion(250), proyeccion(500)))).hasSize(2);
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toResponses(null)).isNull();
    }
}

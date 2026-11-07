package com.mineria.equipos.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Los valores por defecto y los límites de la paginación.
 */
class PaginacionTest {

    @Test
    @DisplayName("sin page ni size: primera página de 20")
    void valoresPorDefecto() {
        // Act
        Pageable pageable = Paginacion.de(null, null);

        // Assert
        assertThat(pageable).isEqualTo(PageRequest.of(0, 20));
        assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName("valores válidos se respetan tal cual")
    void valoresValidos() {
        // Act + Assert
        assertThat(Paginacion.de(3, 50)).isEqualTo(PageRequest.of(3, 50));
        assertThat(Paginacion.de(0, 100)).isEqualTo(PageRequest.of(0, 100));
        assertThat(Paginacion.de(0, 1)).isEqualTo(PageRequest.of(0, 1));
    }

    @ParameterizedTest(name = "page={0}, size={1} -> página {2}, tamaño {3}")
    @CsvSource({"-1,20,0,20", "0,0,0,1", "0,-7,0,1", "0,101,0,100", "2,5000,2,100", "-9,-9,0,1"})
    @DisplayName("valores fuera de rango se ajustan al rango permitido (defensa extra al contrato)")
    void ajustaFueraDeRango(int pagina, int tamano, int paginaEsperada, int tamanoEsperado) {
        // Act
        Pageable pageable = Paginacion.de(pagina, tamano);

        // Assert
        assertThat(pageable.getPageNumber()).isEqualTo(paginaEsperada);
        assertThat(pageable.getPageSize()).isEqualTo(tamanoEsperado);
    }
}

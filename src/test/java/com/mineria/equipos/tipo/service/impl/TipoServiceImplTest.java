package com.mineria.equipos.tipo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.generated.model.TipoEquipoResponse;
import com.mineria.equipos.shared.exception.TipoEquipoNotFoundException;
import com.mineria.equipos.tipo.mapper.TipoMapperImpl;
import com.mineria.equipos.tipo.repository.TipoRepository;
import com.mineria.equipos.tipo.repository.projection.TipoProjection;

@ExtendWith(MockitoExtension.class)
class TipoServiceImplTest {

    @Mock
    private TipoRepository repository;

    private TipoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TipoServiceImpl(repository, new TipoMapperImpl());
    }

    @Test
    @DisplayName("listar: devuelve el catálogo como DTOs del contrato")
    void listar() {
        // Arrange
        TipoProjection camion = new TipoProjection() {
            public Long getId() { return 1L; }
            public String getNombre() { return "Camión"; }
            public Integer getIntervaloHoras() { return 250; }
        };
        when(repository.findAllProjectedByOrderByIdAsc()).thenReturn(List.of(camion));

        // Act
        List<TipoEquipoResponse> resultado = service.listar();

        // Assert
        assertThat(resultado).singleElement().satisfies(t -> {
            assertThat(t.getNombre()).isEqualTo("Camión");
            assertThat(t.getIntervaloHoras()).isEqualTo(250);
        });
    }

    @Test
    @DisplayName("verificarExiste: no lanza si el tipo existe")
    void existe() {
        // Arrange
        when(repository.existsById(1L)).thenReturn(true);

        // Act + Assert
        assertThatCode(() -> service.verificarExiste(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("verificarExiste: TipoEquipoNotFoundException si no existe")
    void noExiste() {
        // Arrange
        when(repository.existsById(99L)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.verificarExiste(99L))
                .isInstanceOf(TipoEquipoNotFoundException.class)
                .hasMessageContaining("99");
    }
}

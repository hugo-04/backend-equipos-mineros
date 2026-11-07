package com.mineria.equipos.tipo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import com.mineria.equipos.generated.model.PaginaTipoEquipoResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
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

import com.mineria.equipos.shared.exception.TipoEquipoNotFoundException;
import com.mineria.equipos.tipo.mapper.TipoMapperImpl;
import com.mineria.equipos.tipo.repository.TipoRepository;
import com.mineria.equipos.tipo.repository.projection.TipoProjection;

/**
 * Listar el catálogo y comprobar que un tipo exista.
 */
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
        Pageable pagina = PageRequest.of(0, 20);
        when(repository.findAllProjectedByOrderByIdAsc(pagina)).thenReturn(new PageImpl<>(List.of(camion), pagina, 1));

        // Act
        PaginaTipoEquipoResponse resultado = service.listar(pagina);

        // Assert
        assertThat(resultado.getContent()).singleElement().satisfies(t -> {
            assertThat(t.getNombre()).isEqualTo("Camión");
            assertThat(t.getIntervaloHoras()).isEqualTo(250);
        });
        assertThat(resultado.getPage()).isZero();
        assertThat(resultado.getSize()).isEqualTo(20);
        assertThat(resultado.getTotalElements()).isEqualTo(1L);
        assertThat(resultado.getTotalPages()).isEqualTo(1);
        assertThat(resultado.getFirst()).isTrue();
        assertThat(resultado.getLast()).isTrue();
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

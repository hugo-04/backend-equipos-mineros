package com.mineria.equipos.tipo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mineria.equipos.testsupport.PersistenciaTest;
import com.mineria.equipos.tipo.entity.TipoEquipo;
import com.mineria.equipos.tipo.repository.projection.TipoProjection;

@PersistenciaTest
class TipoRepositoryTest {

    @Autowired
    private TipoRepository repository;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("la migración deja camión 250, excavadora 500 y perforadora 300")
    void tiposIniciales() {
        // Act
        Page<TipoProjection> pagina = repository.findAllProjectedByOrderByIdAsc(PageRequest.of(0, 20));

        // Assert
        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getContent())
                .extracting(TipoProjection::getNombre, TipoProjection::getIntervaloHoras)
                .containsExactly(
                        tuple("Camión", 250),
                        tuple("Excavadora", 500),
                        tuple("Perforadora", 300));
    }

    @Test
    @DisplayName("pagina: con tamaño 2 el catálogo se reparte en 2 páginas, en orden")
    void paginaElCatalogo() {
        // Act
        Page<TipoProjection> primera = repository.findAllProjectedByOrderByIdAsc(PageRequest.of(0, 2));
        Page<TipoProjection> segunda = repository.findAllProjectedByOrderByIdAsc(PageRequest.of(1, 2));

        // Assert
        assertThat(primera.getContent()).extracting(TipoProjection::getNombre).containsExactly("Camión", "Excavadora");
        assertThat(primera.getTotalPages()).isEqualTo(2);
        assertThat(primera.isFirst()).isTrue();
        assertThat(primera.isLast()).isFalse();
        assertThat(segunda.getContent()).extracting(TipoProjection::getNombre).containsExactly("Perforadora");
        assertThat(segunda.isLast()).isTrue();
    }

    @Test
    @DisplayName("una página fuera de rango viene vacía pero con el total correcto")
    void paginaFueraDeRango() {
        // Act
        Page<TipoProjection> pagina = repository.findAllProjectedByOrderByIdAsc(PageRequest.of(5, 2));

        // Assert
        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("findById y existsById devuelven el tipo o vacío")
    void buscarPorId() {
        // Act + Assert
        assertThat(repository.findById(1L)).get().extracting(TipoEquipo::getNombre).isEqualTo("Camión");
        assertThat(repository.findById(99L)).isEmpty();
        assertThat(repository.existsById(2L)).isTrue();
        assertThat(repository.existsById(99L)).isFalse();
    }

    @Test
    @DisplayName("BD: no deja un intervalo de horas menor o igual a 0")
    void checkIntervaloPositivo() {
        // Act + Assert
        assertThatThrownBy(() -> jdbc.update("insert into tipo_equipo (nombre, intervalo_horas) values ('Malo', 0)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("BD: el nombre de un tipo de equipo es único")
    void nombreDeTipoUnico() {
        // Act + Assert
        assertThatThrownBy(() -> jdbc.update("insert into tipo_equipo (nombre, intervalo_horas) values ('Camión', 100)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

package com.mineria.equipos.mantenimiento.repository;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mineria.equipos.equipo.entity.Equipo;
import com.mineria.equipos.mantenimiento.entity.Mantenimiento;
import com.mineria.equipos.equipo.repository.EquipoRepository;
import com.mineria.equipos.mantenimiento.repository.projection.MantenimientoProjection;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.testsupport.PersistenciaTest;

@PersistenciaTest
class MantenimientoRepositoryTest {

    @Autowired
    private MantenimientoRepository repository;
    @Autowired
    private EquipoRepository equipos;

    private Equipo equipoGuardado(String codigo, String horometro) {
        return equipos.saveAndFlush(Datos.nuevoEquipo(codigo, 1L, horometro));
    }

    @Test
    @DisplayName("el historial sale del más reciente al más antiguo")
    void historialOrdenado() {
        // Arrange
        Equipo equipo = equipoGuardado("CAM-070", "500.0");
        repository.saveAndFlush(Datos.mantenimiento(equipo.getId(), "250.0", "Viejo",
                Instant.parse("2026-01-01T00:00:00Z")));
        repository.saveAndFlush(Datos.mantenimiento(equipo.getId(), "500.0", "Nuevo",
                Instant.parse("2026-03-01T00:00:00Z")));

        // Act
        List<MantenimientoProjection> historial = repository
                .findByEquipoIdOrderByFechaDescIdDesc(equipo.getId(), PageRequest.of(0, 20)).getContent();

        // Assert
        assertThat(historial).extracting(MantenimientoProjection::getNotas).containsExactly("Nuevo", "Viejo");
        assertThat(historial.get(0).getHorometroAlRegistrar()).isEqualByComparingTo("500.0");
    }

    @Test
    @DisplayName("el historial de otro equipo no se mezcla")
    void soloElHistorialDelEquipo() {
        // Arrange
        Equipo a = equipoGuardado("CAM-072", "300.0");
        Equipo b = equipoGuardado("CAM-073", "300.0");
        repository.saveAndFlush(Datos.mantenimiento(a.getId(), "300.0", "De A", Datos.AHORA));
        repository.saveAndFlush(Datos.mantenimiento(b.getId(), "300.0", "De B", Datos.AHORA));

        // Act + Assert
        assertThat(repository.findByEquipoIdOrderByFechaDescIdDesc(a.getId(), PageRequest.of(0, 20)).getContent())
                .extracting(MantenimientoProjection::getNotas).containsExactly("De A");
    }

    @Test
    @DisplayName("pagina el historial: 3 registros con tamaño 2 -> 2 páginas, el más reciente primero")
    void paginaElHistorial() {
        // Arrange
        Equipo equipo = equipoGuardado("CAM-074", "900.0");
        repository.saveAndFlush(Datos.mantenimiento(equipo.getId(), "300.0", "Tercero", Instant.parse("2026-01-01T00:00:00Z")));
        repository.saveAndFlush(Datos.mantenimiento(equipo.getId(), "600.0", "Segundo", Instant.parse("2026-02-01T00:00:00Z")));
        repository.saveAndFlush(Datos.mantenimiento(equipo.getId(), "900.0", "Primero", Instant.parse("2026-03-01T00:00:00Z")));

        // Act
        Page<MantenimientoProjection> primera = repository.findByEquipoIdOrderByFechaDescIdDesc(equipo.getId(), PageRequest.of(0, 2));
        Page<MantenimientoProjection> segunda = repository.findByEquipoIdOrderByFechaDescIdDesc(equipo.getId(), PageRequest.of(1, 2));

        // Assert
        assertThat(primera.getTotalElements()).isEqualTo(3);
        assertThat(primera.getContent()).extracting(MantenimientoProjection::getNotas).containsExactly("Primero", "Segundo");
        assertThat(segunda.getContent()).extracting(MantenimientoProjection::getNotas).containsExactly("Tercero");
        assertThat(segunda.isLast()).isTrue();
    }

    @Test
    @DisplayName("findProjectedById devuelve la proyección o vacío")
    void buscarPorId() {
        // Arrange
        Equipo equipo = equipoGuardado("CAM-071", "260.0");
        Mantenimiento guardado = repository.saveAndFlush(
                Datos.mantenimiento(equipo.getId(), "260.0", null, Datos.AHORA));

        // Act
        Optional<MantenimientoProjection> existente = repository.findProjectedById(guardado.getId());
        Optional<MantenimientoProjection> inexistente = repository.findProjectedById(987654L);

        // Assert
        assertThat(existente).isPresent();
        assertThat(existente.get().getNotas()).isNull();
        assertThat(existente.get().getFecha()).isEqualTo(Datos.AHORA);
        assertThat(inexistente).isEmpty();
    }
}

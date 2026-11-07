package com.mineria.equipos.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.equipo.entity.Equipo;
import com.mineria.equipos.equipo.repository.EquipoRepository;
import com.mineria.equipos.domain.model.Mantenimiento;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.domain.model.Usuario;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.testsupport.PersistenciaTest;

/** Adaptadores de mantenimientos y usuarios (transitorio: pasan a repositorios en los siguientes módulos). */
@PersistenciaTest
class OtrosAdaptadoresPersistenciaTest {

    @Autowired
    private EquipoRepository equipos;
    @Autowired
    private MantenimientoPersistenceAdapter mantenimientos;
    @Autowired
    private UsuarioPersistenceAdapter usuarios;

    private Equipo nuevoEquipo(String codigo, String horometro) {
        Equipo equipo = new Equipo();
        equipo.setCodigo(codigo);
        equipo.setTipoId(1L);
        equipo.setHorometro(new BigDecimal(horometro));
        equipo.darDeAlta(Datos.AHORA);
        return equipo;
    }

    @Test
    @DisplayName("mantenimientos: el historial sale del más reciente al más antiguo")
    void historialOrdenado() {
        // Arrange
        Equipo equipo = equipos.saveAndFlush(nuevoEquipo("CAM-070", "500.0"));
        mantenimientos.guardar(Mantenimiento.builder().equipoId(equipo.getId())
                .fecha(Instant.parse("2026-01-01T00:00:00Z")).horometroAlRegistrar(new BigDecimal("250.0")).notas("Viejo").build());
        mantenimientos.guardar(Mantenimiento.builder().equipoId(equipo.getId())
                .fecha(Instant.parse("2026-03-01T00:00:00Z")).horometroAlRegistrar(new BigDecimal("500.0")).notas("Nuevo").build());

        // Act
        List<MantenimientoView> historial = mantenimientos.listarPorEquipo(equipo.getId());

        // Assert
        assertThat(historial).extracting(MantenimientoView::getNotas).containsExactly("Nuevo", "Viejo");
        assertThat(historial.get(0).getHorometroAlRegistrar()).isEqualByComparingTo("500.0");
    }

    @Test
    @DisplayName("mantenimientos: buscarPorId devuelve la view o vacío")
    void buscarMantenimiento() {
        // Arrange
        Equipo equipo = equipos.saveAndFlush(nuevoEquipo("CAM-071", "260.0"));
        Mantenimiento guardado = mantenimientos.guardar(
                Mantenimiento.registrarPara(equipo.getId(), equipo.getHorometro(), null, Datos.AHORA));

        // Act
        Optional<MantenimientoView> existente = mantenimientos.buscarPorId(guardado.getId());
        Optional<MantenimientoView> inexistente = mantenimientos.buscarPorId(987654L);

        // Assert
        assertThat(existente).isPresent();
        assertThat(existente.get().getNotas()).isNull();
        assertThat(inexistente).isEmpty();
    }

    @Test
    @DisplayName("usuarios: guarda y busca por username")
    void usuarios() {
        // Arrange
        Usuario nuevo = Usuario.builder().username("operador1").passwordHash("$2a$hash").rol(Rol.OPERADOR).build();

        // Act
        Usuario guardado = usuarios.guardar(nuevo);
        Optional<Usuario> encontrado = usuarios.buscarPorUsername("operador1");
        Optional<Usuario> inexistente = usuarios.buscarPorUsername("nadie");

        // Assert
        assertThat(guardado.getId()).isNotNull();
        assertThat(encontrado).get().extracting(Usuario::getRol).isEqualTo(Rol.OPERADOR);
        assertThat(inexistente).isEmpty();
    }
}

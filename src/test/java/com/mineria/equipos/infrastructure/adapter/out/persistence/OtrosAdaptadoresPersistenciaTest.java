package com.mineria.equipos.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.domain.model.Mantenimiento;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.domain.model.TipoEquipo;
import com.mineria.equipos.domain.model.Usuario;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.testsupport.PersistenciaTest;

/** Adaptadores de tipos, mantenimientos y usuarios, más las restricciones que definen las migraciones. */
@PersistenciaTest
class OtrosAdaptadoresPersistenciaTest {

    @Autowired
    private TipoEquipoPersistenceAdapter tipos;
    @Autowired
    private EquipoPersistenceAdapter equipos;
    @Autowired
    private MantenimientoPersistenceAdapter mantenimientos;
    @Autowired
    private UsuarioPersistenceAdapter usuarios;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("mantenimientos: el historial sale del más reciente al más antiguo")
    void historialOrdenado() {
        // Arrange
        Equipo equipo = equipos.guardar(Equipo.nuevo("CAM-070", tipos.buscarPorId(1L).orElseThrow(),
                new BigDecimal("500.0"), Datos.AHORA));
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
        Equipo equipo = equipos.guardar(Equipo.nuevo("CAM-071", tipos.buscarPorId(1L).orElseThrow(),
                new BigDecimal("260.0"), Datos.AHORA));
        Mantenimiento guardado = mantenimientos.guardar(Mantenimiento.registrarPara(equipo, null, Datos.AHORA));

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

    @Test
    @DisplayName("BD: no deja guardar un horómetro menor al del último mantenimiento")
    void checkHorometroMayorOIgual() {
        // Act + Assert
        assertThatThrownBy(() -> jdbc.update(
                "insert into equipo (codigo, tipo_id, horometro, horometro_ultimo_mant) values ('BAD-001', 1, 10.0, 20.0)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

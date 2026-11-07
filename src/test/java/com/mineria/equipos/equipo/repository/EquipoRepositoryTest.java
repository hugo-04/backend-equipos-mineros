package com.mineria.equipos.equipo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mineria.equipos.equipo.entity.Equipo;
import com.mineria.equipos.equipo.repository.projection.EquipoProjection;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.testsupport.PersistenciaTest;

import jakarta.persistence.EntityManager;

/**
 * Las consultas de equipos contra la base real (H2 en modo MySQL con las migraciones): guardar, actualizar, el JOIN
 * con el tipo, el código único y la paginación.
 */
@PersistenciaTest
class EquipoRepositoryTest {

    @Autowired
    private EquipoRepository repository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;

    private Equipo nuevo(String codigo, long tipoId, String horometro) {
        return Datos.nuevoEquipo(codigo, tipoId, horometro);
    }

    @Test
    @DisplayName("guarda un equipo nuevo y lo recupera con sus datos")
    void guardaYRecupera() {
        // Arrange
        Equipo nuevo = nuevo("EXC-001", 2L, "80.5");

        // Act
        Equipo guardado = repository.saveAndFlush(nuevo);
        entityManager.clear();
        Optional<Equipo> recuperado = repository.findById(guardado.getId());

        // Assert
        assertThat(guardado.getId()).isNotNull();
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getCodigo()).isEqualTo("EXC-001");
        assertThat(recuperado.get().getTipoId()).isEqualTo(2L);
        assertThat(recuperado.get().getHorometroUltimoMant()).isEqualByComparingTo("0");
        assertThat(recuperado.get().getCreadoEn()).isEqualTo(Datos.AHORA);
    }

    @Test
    @DisplayName("actualiza un equipo existente sin tocar su fecha de creación")
    void actualiza() {
        // Arrange
        Equipo guardado = repository.saveAndFlush(nuevo("CAM-010", 1L, "10.0"));

        // Act
        guardado.setHorometro(new BigDecimal("55.5"));
        repository.saveAndFlush(guardado);
        entityManager.clear();
        Equipo recuperado = repository.findById(guardado.getId()).orElseThrow();

        // Assert
        assertThat(recuperado.getHorometro()).isEqualByComparingTo("55.5");
        assertThat(recuperado.getCreadoEn()).isEqualTo(Datos.AHORA);
    }

    @Test
    @DisplayName("la lectura devuelve proyecciones con los datos del tipo ya unidos")
    void listaProyeccionesConElTipo() {
        // Arrange
        repository.saveAndFlush(nuevo("CAM-020", 1L, "1.0"));
        repository.saveAndFlush(nuevo("PER-020", 3L, "2.0"));
        entityManager.clear();

        // Act
        List<EquipoProjection> proyecciones = repository.listarProyecciones(PageRequest.of(0, 100)).getContent();

        // Assert
        assertThat(proyecciones).extracting(EquipoProjection::getCodigo).contains("CAM-020", "PER-020");
        assertThat(proyecciones).filteredOn(p -> p.getCodigo().equals("PER-020")).singleElement().satisfies(p -> {
            assertThat(p.getTipoNombre()).isEqualTo("Perforadora");
            assertThat(p.getIntervaloHoras()).isEqualTo(300);
            assertThat(p.getTipoId()).isEqualTo(3L);
        });
    }

    @Test
    @DisplayName("pagina: 3 equipos con tamaño 2 -> 2 páginas, en orden de id, con el total correcto")
    void paginaLosEquipos() {
        // Arrange
        repository.saveAndFlush(nuevo("PAG-001", 1L, "1.0"));
        repository.saveAndFlush(nuevo("PAG-002", 2L, "2.0"));
        repository.saveAndFlush(nuevo("PAG-003", 3L, "3.0"));
        entityManager.clear();

        // Act
        Page<EquipoProjection> primera = repository.listarProyecciones(PageRequest.of(0, 2));
        Page<EquipoProjection> segunda = repository.listarProyecciones(PageRequest.of(1, 2));

        // Assert: el COUNT escrito a mano cuenta los equipos con tipo (3), no las filas de la página
        assertThat(primera.getTotalElements()).isEqualTo(3);
        assertThat(primera.getTotalPages()).isEqualTo(2);
        assertThat(primera.getContent()).extracting(EquipoProjection::getCodigo).containsExactly("PAG-001", "PAG-002");
        assertThat(primera.isFirst()).isTrue();
        assertThat(segunda.getContent()).extracting(EquipoProjection::getCodigo).containsExactly("PAG-003");
        assertThat(segunda.isLast()).isTrue();
    }

    @Test
    @DisplayName("una página fuera de rango viene vacía pero con el total correcto")
    void paginaFueraDeRango() {
        // Arrange
        repository.saveAndFlush(nuevo("PAG-010", 1L, "1.0"));

        // Act
        Page<EquipoProjection> pagina = repository.listarProyecciones(PageRequest.of(4, 10));

        // Assert
        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("buscarProyeccionPorId: devuelve la proyección si existe y vacío si no")
    void buscarProyeccion() {
        // Arrange
        Equipo guardado = repository.saveAndFlush(nuevo("CAM-030", 1L, "3.0"));
        entityManager.clear();

        // Act
        Optional<EquipoProjection> existente = repository.buscarProyeccionPorId(guardado.getId());
        Optional<EquipoProjection> inexistente = repository.buscarProyeccionPorId(987654L);

        // Assert
        assertThat(existente).isPresent();
        assertThat(existente.get().getTipoId()).isEqualTo(1L);
        assertThat(inexistente).isEmpty();
    }

    @Test
    @DisplayName("existsById, existsByCodigo y existsByCodigoAndIdNot")
    void existencias() {
        // Arrange
        Equipo a = repository.saveAndFlush(nuevo("CAM-040", 1L, "1"));
        Equipo b = repository.saveAndFlush(nuevo("CAM-041", 1L, "1"));

        // Act + Assert
        assertThat(repository.existsById(a.getId())).isTrue();
        assertThat(repository.existsById(987654L)).isFalse();
        assertThat(repository.existsByCodigo("CAM-040")).isTrue();
        assertThat(repository.existsByCodigo("NO-EXISTE")).isFalse();
        assertThat(repository.existsByCodigoAndIdNot("CAM-040", a.getId())).isFalse();
        assertThat(repository.existsByCodigoAndIdNot("CAM-040", b.getId())).isTrue();
    }

    @Test
    @DisplayName("BD: un código repetido viola la restricción única uk_equipo_codigo")
    void codigoDuplicado() {
        // Arrange
        repository.saveAndFlush(nuevo("CAM-050", 1L, "1"));

        // Act + Assert
        assertThatThrownBy(() -> repository.saveAndFlush(nuevo("CAM-050", 1L, "1")))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class, e ->
                        assertThat(e.getMostSpecificCause().getMessage().toLowerCase()).contains("uk_equipo_codigo"));
    }

    @Test
    @DisplayName("BD: un tipo inexistente viola la llave foránea")
    void tipoInexistente() {
        // Act + Assert
        assertThatThrownBy(() -> repository.saveAndFlush(nuevo("FAN-001", 9999L, "1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("BD: no deja guardar un horómetro menor al del último mantenimiento")
    void checkHorometroMayorOIgual() {
        // Act + Assert
        assertThatThrownBy(() -> jdbc.update(
                "insert into equipo (codigo, tipo_id, horometro, horometro_ultimo_mant) values ('BAD-001', 1, 10.0, 20.0)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("eliminar un equipo borra también su historial (ON DELETE CASCADE)")
    void eliminarBorraElHistorial() {
        // Arrange
        Equipo equipo = repository.saveAndFlush(nuevo("CAM-060", 1L, "300.0"));
        jdbc.update("insert into mantenimiento (equipo_id, fecha, horometro_al_registrar, notas) "
                + "values (?, current_timestamp, 300.0, 'Primero')", equipo.getId());
        jdbc.update("insert into mantenimiento (equipo_id, fecha, horometro_al_registrar, notas) "
                + "values (?, current_timestamp, 300.0, 'Segundo')", equipo.getId());
        assertThat(jdbc.queryForObject("select count(*) from mantenimiento", Integer.class)).isEqualTo(2);

        // Act
        repository.deleteById(equipo.getId());
        repository.flush();
        entityManager.clear();

        // Assert
        assertThat(repository.existsById(equipo.getId())).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from mantenimiento", Integer.class)).isZero();
    }

    @Test
    @DisplayName("el tipo no se puede cambiar: aunque se llame al setter, el UPDATE no toca tipo_id")
    void elTipoNoSeActualiza() {
        // Arrange
        Equipo guardado = repository.saveAndFlush(nuevo("CAM-020", 1L, "10.0"));

        // Act: se intenta pasar el equipo a excavadora y se cambia otro dato para que haya UPDATE
        guardado.setTipoId(2L);
        guardado.setHorometro(new BigDecimal("20.0"));
        repository.saveAndFlush(guardado);
        entityManager.clear();
        Equipo recuperado = repository.findById(guardado.getId()).orElseThrow();

        // Assert: el horómetro sí se guardó, el tipo sigue siendo el de origen
        assertThat(recuperado.getHorometro()).isEqualByComparingTo("20.0");
        assertThat(recuperado.getTipoId()).isEqualTo(1L);
    }
}

package com.mineria.equipos.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.shared.exception.CodigoDuplicadoException;
import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.domain.model.Mantenimiento;
import com.mineria.equipos.domain.model.TipoEquipo;
import com.mineria.equipos.infrastructure.adapter.out.persistence.repository.MantenimientoJpaRepository;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.testsupport.PersistenciaTest;

import jakarta.persistence.EntityManager;

@PersistenciaTest
class EquipoPersistenceAdapterTest {

    @Autowired
    private EquipoPersistenceAdapter adapter;
    @Autowired
    private TipoEquipoPersistenceAdapter tipos;
    @Autowired
    private MantenimientoPersistenceAdapter mantenimientos;
    @Autowired
    private MantenimientoJpaRepository mantenimientoRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("guarda un equipo nuevo y lo recupera con su tipo (sin N+1)")
    void guardaYRecupera() {
        // Arrange
        TipoEquipo excavadora = tipos.buscarPorId(2L).orElseThrow();
        Equipo nuevo = Equipo.nuevo("EXC-001", excavadora, new BigDecimal("80.5"), Datos.AHORA);

        // Act
        Equipo guardado = adapter.guardar(nuevo);
        entityManager.clear();
        Optional<Equipo> recuperado = adapter.buscarPorId(guardado.getId());

        // Assert
        assertThat(guardado.getId()).isNotNull();
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getCodigo()).isEqualTo("EXC-001");
        assertThat(recuperado.get().getTipo().getNombre()).isEqualTo("Excavadora");
        assertThat(recuperado.get().getHorometroUltimoMant()).isEqualByComparingTo("0");
        assertThat(recuperado.get().getCreadoEn()).isEqualTo(Datos.AHORA);
    }

    @Test
    @DisplayName("actualiza un equipo existente sin tocar su fecha de creación")
    void actualiza() {
        // Arrange
        Equipo guardado = adapter.guardar(Equipo.nuevo("CAM-010", tipos.buscarPorId(1L).orElseThrow(),
                new BigDecimal("10.0"), Datos.AHORA));

        // Act
        adapter.guardar(guardado.actualizar("CAM-010", guardado.getTipo(), new BigDecimal("55.5")));
        entityManager.clear();
        Equipo recuperado = adapter.buscarPorId(guardado.getId()).orElseThrow();

        // Assert
        assertThat(recuperado.getHorometro()).isEqualByComparingTo("55.5");
        assertThat(recuperado.getCreadoEn()).isEqualTo(Datos.AHORA);
    }

    @Test
    @DisplayName("la lectura devuelve proyecciones con los datos del tipo ya unidos")
    void listaVistasConElTipo() {
        // Arrange
        adapter.guardar(Equipo.nuevo("CAM-020", tipos.buscarPorId(1L).orElseThrow(), new BigDecimal("1.0"), Datos.AHORA));
        adapter.guardar(Equipo.nuevo("PER-020", tipos.buscarPorId(3L).orElseThrow(), new BigDecimal("2.0"), Datos.AHORA));
        entityManager.clear();

        // Act
        List<EquipoView> vistas = adapter.listar();

        // Assert
        assertThat(vistas).extracting(EquipoView::getCodigo).contains("CAM-020", "PER-020");
        assertThat(vistas).filteredOn(v -> v.getCodigo().equals("PER-020")).singleElement().satisfies(v -> {
            assertThat(v.getTipoNombre()).isEqualTo("Perforadora");
            assertThat(v.getIntervaloHoras()).isEqualTo(300);
        });
    }

    @Test
    @DisplayName("buscarVistaPorId: devuelve la view si existe y vacío si no")
    void buscarVista() {
        // Arrange
        Equipo guardado = adapter.guardar(Equipo.nuevo("CAM-030", tipos.buscarPorId(1L).orElseThrow(),
                new BigDecimal("3.0"), Datos.AHORA));
        entityManager.clear();

        // Act
        Optional<EquipoView> existente = adapter.buscarVistaPorId(guardado.getId());
        Optional<EquipoView> inexistente = adapter.buscarVistaPorId(987654L);

        // Assert
        assertThat(existente).isPresent();
        assertThat(existente.get().getTipoId()).isEqualTo(1L);
        assertThat(inexistente).isEmpty();
    }

    @Test
    @DisplayName("existePorId y existePorCodigo / existePorCodigoDistintoDe")
    void existencias() {
        // Arrange
        Equipo a = adapter.guardar(Equipo.nuevo("CAM-040", tipos.buscarPorId(1L).orElseThrow(), BigDecimal.ONE, Datos.AHORA));
        Equipo b = adapter.guardar(Equipo.nuevo("CAM-041", tipos.buscarPorId(1L).orElseThrow(), BigDecimal.ONE, Datos.AHORA));

        // Act + Assert
        assertThat(adapter.existePorId(a.getId())).isTrue();
        assertThat(adapter.existePorId(987654L)).isFalse();
        assertThat(adapter.existePorCodigo("CAM-040")).isTrue();
        assertThat(adapter.existePorCodigo("NO-EXISTE")).isFalse();
        assertThat(adapter.existePorCodigoDistintoDe("CAM-040", a.getId())).isFalse();
        assertThat(adapter.existePorCodigoDistintoDe("CAM-040", b.getId())).isTrue();
    }

    @Test
    @DisplayName("un código repetido se traduce a CodigoDuplicadoException")
    void codigoDuplicado() {
        // Arrange
        TipoEquipo camion = tipos.buscarPorId(1L).orElseThrow();
        adapter.guardar(Equipo.nuevo("CAM-050", camion, BigDecimal.ONE, Datos.AHORA));

        // Act + Assert
        assertThatThrownBy(() -> adapter.guardar(Equipo.nuevo("CAM-050", camion, BigDecimal.ONE, Datos.AHORA)))
                .isInstanceOf(CodigoDuplicadoException.class);
    }

    @Test
    @DisplayName("otras violaciones de integridad (tipo inexistente) se relanzan tal cual")
    void otraViolacionDeIntegridad() {
        // Arrange
        TipoEquipo tipoFantasma = TipoEquipo.builder().id(9999L).nombre("Fantasma").intervaloHoras(10).build();
        Equipo conTipoInexistente = Equipo.nuevo("FAN-001", tipoFantasma, BigDecimal.ONE, Datos.AHORA);

        // Act + Assert
        assertThatThrownBy(() -> adapter.guardar(conTipoInexistente))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("eliminar un equipo borra también su historial (ON DELETE CASCADE)")
    void eliminarBorraElHistorial() {
        // Arrange
        Equipo equipo = adapter.guardar(Equipo.nuevo("CAM-060", tipos.buscarPorId(1L).orElseThrow(),
                new BigDecimal("300.0"), Datos.AHORA));
        mantenimientos.guardar(Mantenimiento.registrarPara(equipo, "Primero", Datos.AHORA));
        mantenimientos.guardar(Mantenimiento.registrarPara(equipo, "Segundo", Datos.AHORA));
        assertThat(mantenimientoRepository.count()).isEqualTo(2);

        // Act
        adapter.eliminarPorId(equipo.getId());
        entityManager.clear();

        // Assert
        assertThat(adapter.existePorId(equipo.getId())).isFalse();
        assertThat(mantenimientoRepository.count()).isZero();
    }
}

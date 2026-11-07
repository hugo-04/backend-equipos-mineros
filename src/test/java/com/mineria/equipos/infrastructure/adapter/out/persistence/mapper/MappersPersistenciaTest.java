package com.mineria.equipos.infrastructure.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.domain.model.Mantenimiento;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.domain.model.TipoEquipo;
import com.mineria.equipos.domain.model.Usuario;
import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.EquipoEntity;
import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.MantenimientoEntity;
import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import com.mineria.equipos.infrastructure.adapter.out.persistence.repository.EquipoProjection;
import com.mineria.equipos.infrastructure.adapter.out.persistence.repository.MantenimientoProjection;
import com.mineria.equipos.testsupport.Datos;

@SpringJUnitConfig({EquipoPersistenceMapperImpl.class, TipoEquipoPersistenceMapperImpl.class,
        MantenimientoPersistenceMapperImpl.class, UsuarioPersistenceMapperImpl.class})
class MappersPersistenciaTest {

    @Autowired
    private EquipoPersistenceMapper equipoMapper;
    @Autowired
    private TipoEquipoPersistenceMapper tipoMapper;
    @Autowired
    private MantenimientoPersistenceMapper mantenimientoMapper;
    @Autowired
    private UsuarioPersistenceMapper usuarioMapper;

    @Test
    @DisplayName("equipo: dominio -> entidad -> dominio conserva todos los campos")
    void equipoIdaYVuelta() {
        // Arrange
        Equipo dominio = Datos.equipo(4L, "CAM-004", "123.4");

        // Act
        EquipoEntity entidad = equipoMapper.toEntity(dominio);
        Equipo vuelta = equipoMapper.toDomain(entidad);

        // Assert
        assertThat(entidad.getTipo().getNombre()).isEqualTo("Camión");
        assertThat(vuelta).isEqualTo(dominio);
    }

    @Test
    @DisplayName("equipo: proyección -> view deja sin calcular el estado de mantenimiento")
    void equipoProyeccionAView() {
        // Arrange
        EquipoProjection proyeccion = new EquipoProjection() {
            public Long getId() { return 4L; }
            public String getCodigo() { return "CAM-004"; }
            public Long getTipoId() { return 1L; }
            public String getTipoNombre() { return "Camión"; }
            public Integer getIntervaloHoras() { return 250; }
            public BigDecimal getHorometro() { return new BigDecimal("12.0"); }
            public BigDecimal getHorometroUltimoMant() { return BigDecimal.ZERO; }
            public Instant getCreadoEn() { return Datos.AHORA; }
        };

        // Act
        EquipoView view = equipoMapper.toView(proyeccion);

        // Assert
        assertThat(view.getTipoNombre()).isEqualTo("Camión");
        assertThat(view.isRequiereMantenimiento()).isFalse();
        assertThat(view.getEstado()).isNull();
    }

    @Test
    @DisplayName("equipo: una proyección sin intervalo no rompe el mapeo")
    void equipoProyeccionSinIntervalo() {
        // Arrange
        EquipoProjection proyeccion = new EquipoProjection() {
            public Long getId() { return 1L; }
            public String getCodigo() { return "X-1"; }
            public Long getTipoId() { return 1L; }
            public String getTipoNombre() { return "T"; }
            public Integer getIntervaloHoras() { return null; }
            public BigDecimal getHorometro() { return BigDecimal.ZERO; }
            public BigDecimal getHorometroUltimoMant() { return BigDecimal.ZERO; }
            public Instant getCreadoEn() { return Datos.AHORA; }
        };

        // Act + Assert
        assertThat(equipoMapper.toView(proyeccion).getIntervaloHoras()).isZero();
    }

    @Test
    @DisplayName("tipo: dominio <-> entidad")
    void tipo() {
        // Arrange
        TipoEquipo dominio = Datos.camion();
        // Act
        com.mineria.equipos.tipo.entity.TipoEquipo entidad = tipoMapper.toEntity(dominio);

        // Assert
        assertThat(tipoMapper.toDomain(entidad)).isEqualTo(dominio);
    }

    @Test
    @DisplayName("mantenimiento: dominio <-> entidad y proyección -> view")
    void mantenimiento() {
        // Arrange
        Mantenimiento dominio = Mantenimiento.builder().id(2L).equipoId(4L).fecha(Datos.AHORA)
                .horometroAlRegistrar(new BigDecimal("250.0")).notas("Aceite").build();
        MantenimientoProjection proyeccion = new MantenimientoProjection() {
            public Long getId() { return 2L; }
            public Long getEquipoId() { return 4L; }
            public Instant getFecha() { return Datos.AHORA; }
            public BigDecimal getHorometroAlRegistrar() { return new BigDecimal("250.0"); }
            public String getNotas() { return "Aceite"; }
        };

        // Act
        MantenimientoEntity entidad = mantenimientoMapper.toEntity(dominio);
        MantenimientoView view = mantenimientoMapper.toView(proyeccion);

        // Assert
        assertThat(mantenimientoMapper.toDomain(entidad)).isEqualTo(dominio);
        assertThat(view.getNotas()).isEqualTo("Aceite");
    }

    @Test
    @DisplayName("usuario: dominio <-> entidad conserva el rol y el hash")
    void usuario() {
        // Arrange
        Usuario dominio = Usuario.builder().id(1L).username("admin").passwordHash("$2a$h").rol(Rol.ADMIN).build();

        // Act
        UsuarioEntity entidad = usuarioMapper.toEntity(dominio);

        // Assert
        assertThat(entidad.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(usuarioMapper.toDomain(entidad)).isEqualTo(dominio);
    }

    @Test
    @DisplayName("todos los mappers devuelven null cuando reciben null")
    void nulos() {
        // Act + Assert
        assertThat(equipoMapper.toDomain(null)).isNull();
        assertThat(equipoMapper.toEntity(null)).isNull();
        assertThat(equipoMapper.toView(null)).isNull();
        assertThat(tipoMapper.toDomain(null)).isNull();
        assertThat(tipoMapper.toEntity(null)).isNull();
        assertThat(mantenimientoMapper.toDomain(null)).isNull();
        assertThat(mantenimientoMapper.toEntity(null)).isNull();
        assertThat(mantenimientoMapper.toView(null)).isNull();
        assertThat(usuarioMapper.toDomain(null)).isNull();
        assertThat(usuarioMapper.toEntity(null)).isNull();
    }
}

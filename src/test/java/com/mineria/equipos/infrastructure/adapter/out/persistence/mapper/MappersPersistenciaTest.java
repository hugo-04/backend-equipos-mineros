package com.mineria.equipos.infrastructure.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.domain.model.Mantenimiento;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.domain.model.Usuario;
import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.MantenimientoEntity;
import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import com.mineria.equipos.infrastructure.adapter.out.persistence.repository.MantenimientoProjection;
import com.mineria.equipos.testsupport.Datos;

@SpringJUnitConfig({MantenimientoPersistenceMapperImpl.class, UsuarioPersistenceMapperImpl.class})
class MappersPersistenciaTest {

    @Autowired
    private MantenimientoPersistenceMapper mantenimientoMapper;
    @Autowired
    private UsuarioPersistenceMapper usuarioMapper;

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
        assertThat(mantenimientoMapper.toDomain(null)).isNull();
        assertThat(mantenimientoMapper.toEntity(null)).isNull();
        assertThat(mantenimientoMapper.toView(null)).isNull();
        assertThat(usuarioMapper.toDomain(null)).isNull();
        assertThat(usuarioMapper.toEntity(null)).isNull();
    }
}

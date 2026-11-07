package com.mineria.equipos.infrastructure.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.domain.model.Usuario;
import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import com.mineria.equipos.testsupport.Datos;

@SpringJUnitConfig(UsuarioPersistenceMapperImpl.class)
class MappersPersistenciaTest {

    @Autowired
    private UsuarioPersistenceMapper usuarioMapper;

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
        assertThat(usuarioMapper.toDomain(null)).isNull();
        assertThat(usuarioMapper.toEntity(null)).isNull();
    }
}

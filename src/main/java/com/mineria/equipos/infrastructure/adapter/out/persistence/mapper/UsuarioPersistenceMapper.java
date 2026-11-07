package com.mineria.equipos.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;

import com.mineria.equipos.domain.model.Usuario;
import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.UsuarioEntity;

/** Convierte entre la entidad JPA de usuario y el modelo de dominio. */
@Mapper
public interface UsuarioPersistenceMapper {

    Usuario toDomain(UsuarioEntity entity);

    UsuarioEntity toEntity(Usuario usuario);
}

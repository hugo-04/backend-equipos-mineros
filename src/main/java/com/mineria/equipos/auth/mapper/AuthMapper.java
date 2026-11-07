package com.mineria.equipos.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.auth.entity.Usuario;

import com.mineria.equipos.generated.model.LoginResponse;

/** Arma la respuesta del login y la entidad cuando se crea un usuario. */
@Mapper
public interface AuthMapper {

    LoginResponse toLoginResponse(String accessToken, String tokenType, Long expiresIn);

    @Mapping(target = "id", ignore = true)
    Usuario toEntity(String username, String passwordHash, Rol rol);
}

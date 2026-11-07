package com.mineria.equipos.auth.mapper;

import org.mapstruct.Mapper;

import com.mineria.equipos.generated.model.LoginResponse;

/** Arma la respuesta del login a partir del token emitido y sus datos. */
@Mapper
public interface AuthMapper {

    LoginResponse toLoginResponse(String accessToken, String tokenType, Long expiresIn);
}

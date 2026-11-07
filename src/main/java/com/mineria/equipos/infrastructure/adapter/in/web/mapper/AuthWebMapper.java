package com.mineria.equipos.infrastructure.adapter.in.web.mapper;

import org.mapstruct.Mapper;

import com.mineria.equipos.application.port.in.AutenticarUsuarioUseCase;
import com.mineria.equipos.application.view.SesionView;
import com.mineria.equipos.generated.model.LoginRequest;
import com.mineria.equipos.generated.model.LoginResponse;

/** Convierte entre los DTOs del login y el comando/view de la capa de aplicación. */
@Mapper
public interface AuthWebMapper {

    AutenticarUsuarioUseCase.Comando toComando(LoginRequest request);

    LoginResponse toResponse(SesionView sesion);
}

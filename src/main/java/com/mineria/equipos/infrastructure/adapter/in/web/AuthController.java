package com.mineria.equipos.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.mineria.equipos.application.port.in.AutenticarUsuarioUseCase;
import com.mineria.equipos.generated.api.AuthApi;
import com.mineria.equipos.generated.model.LoginRequest;
import com.mineria.equipos.generated.model.LoginResponse;
import com.mineria.equipos.infrastructure.adapter.in.web.mapper.AuthWebMapper;

import lombok.RequiredArgsConstructor;

/** Endpoint de login. Solo traduce el DTO y delega en el caso de uso. */
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AutenticarUsuarioUseCase autenticarUsuario;
    private final AuthWebMapper mapper;

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        return ResponseEntity.ok(mapper.toResponse(autenticarUsuario.autenticar(mapper.toComando(loginRequest))));
    }
}

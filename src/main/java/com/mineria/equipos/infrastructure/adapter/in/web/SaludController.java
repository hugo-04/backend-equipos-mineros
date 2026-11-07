package com.mineria.equipos.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.mineria.equipos.application.port.in.VerificarSaludUseCase;
import com.mineria.equipos.application.view.EstadoSalud;
import com.mineria.equipos.generated.api.SaludApi;
import com.mineria.equipos.generated.model.HealthResponse;
import com.mineria.equipos.infrastructure.adapter.in.web.mapper.HealthWebMapper;
import com.mineria.equipos.shared.config.AppProperties;

import lombok.RequiredArgsConstructor;

/**
 * Endpoint /health. Responde 200 si la base de datos contesta y 503 si no.
 * Incluye la versión (tag de la imagen) para que el despliegue pueda comprobar qué está corriendo.
 */
@RestController
@RequiredArgsConstructor
public class SaludController implements SaludApi {

    private final VerificarSaludUseCase verificarSalud;
    private final HealthWebMapper mapper;
    private final AppProperties properties;

    @Override
    public ResponseEntity<HealthResponse> health() {
        EstadoSalud estado = verificarSalud.verificar();
        HttpStatus http = estado == EstadoSalud.UP ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(http).body(mapper.toResponse(estado, properties.version()));
    }
}

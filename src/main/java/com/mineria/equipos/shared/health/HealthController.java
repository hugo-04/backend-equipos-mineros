package com.mineria.equipos.shared.health;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.mineria.equipos.generated.api.SaludApi;
import com.mineria.equipos.generated.model.HealthResponse;

import lombok.RequiredArgsConstructor;

/**
 * Endpoint /health. Responde 200 si la base de datos contesta y 503 si no.
 * Incluye la versión (tag de la imagen) para que el despliegue pueda comprobar qué está corriendo.
 */
@RestController
@RequiredArgsConstructor
public class HealthController implements SaludApi {

    private final HealthService healthService;

    @Override
    public ResponseEntity<HealthResponse> health() {
        HealthResponse salud = healthService.verificar();
        HttpStatus http = salud.getStatus() == HealthResponse.StatusEnum.UP
                ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(http).body(salud);
    }
}

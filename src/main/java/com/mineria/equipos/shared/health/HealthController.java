package com.mineria.equipos.shared.health;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.mineria.equipos.generated.api.SaludApi;
import com.mineria.equipos.generated.model.HealthResponse;

import lombok.RequiredArgsConstructor;

/**
 * El /health. Da 200 si la base de datos contesta y 503 si no. deploy.sh lo consulta después de cada despliegue
 * y por eso devuelve también la versión (el tag de la imagen): así confirma cuál quedó corriendo.
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

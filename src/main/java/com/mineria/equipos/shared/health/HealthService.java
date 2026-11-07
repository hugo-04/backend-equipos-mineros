package com.mineria.equipos.shared.health;

import com.mineria.equipos.generated.model.HealthResponse;

/** Servicio del health check: dice si el servicio y su base de datos están funcionando. */
public interface HealthService {

    HealthResponse verificar();
}

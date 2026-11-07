package com.mineria.equipos.shared.health;

import com.mineria.equipos.generated.model.HealthResponse;

/** Responde si el servicio está sano, que en la práctica es preguntarle algo a la base de datos. */
public interface HealthService {

    HealthResponse verificar();
}

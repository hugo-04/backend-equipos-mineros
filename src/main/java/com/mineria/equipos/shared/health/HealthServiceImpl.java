package com.mineria.equipos.shared.health;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import com.mineria.equipos.generated.model.HealthResponse;
import com.mineria.equipos.shared.config.AppProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Está "UP" solo si la base de datos contesta. Pido una conexión y la valido con un timeout corto; si falla
 * no lanzo nada, devuelvo DOWN, para que el /health responda 503 y no un 500 que confunda.
 * También informa desde cuándo corre esta instancia (el momento en que se creó este servicio, al arrancar la app):
 * así, tras un despliegue, se ve que la versión nueva es la que está arriba y que se reinició.
 */
@Slf4j
@Service
public class HealthServiceImpl implements HealthService {

    private static final int TIMEOUT_SEGUNDOS = 2;

    private final DataSource dataSource;
    private final HealthMapper mapper;
    private final AppProperties properties;
    private final Instant iniciadoEn;

    public HealthServiceImpl(DataSource dataSource, HealthMapper mapper, AppProperties properties, Clock clock) {
        this.dataSource = dataSource;
        this.mapper = mapper;
        this.properties = properties;
        this.iniciadoEn = clock.instant();
    }

    @Override
    public HealthResponse verificar() {
        return mapper.toResponse(baseDeDatosDisponible(), properties.version(), iniciadoEn);
    }

    private boolean baseDeDatosDisponible() {
        try (Connection conexion = dataSource.getConnection()) {
            return conexion.isValid(TIMEOUT_SEGUNDOS);
        } catch (SQLException e) {
            log.error("La base de datos no responde: {}", e.getMessage());
            return false;
        }
    }
}

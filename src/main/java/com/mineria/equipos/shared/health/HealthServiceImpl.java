package com.mineria.equipos.shared.health;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import com.mineria.equipos.generated.model.HealthResponse;
import com.mineria.equipos.shared.config.AppProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Está "UP" solo si la base de datos contesta. Pido una conexión y la valido con un timeout corto; si falla
 * no lanzo nada, devuelvo DOWN, para que el /health responda 503 y no un 500 que confunda.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private static final int TIMEOUT_SEGUNDOS = 2;

    private final DataSource dataSource;
    private final HealthMapper mapper;
    private final AppProperties properties;

    @Override
    public HealthResponse verificar() {
        return mapper.toResponse(baseDeDatosDisponible(), properties.version());
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

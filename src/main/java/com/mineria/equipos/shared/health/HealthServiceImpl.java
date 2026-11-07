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
 * El servicio está "UP" solo si la base de datos responde; si no, "DOWN" (el controller lo traduce a 503).
 * Comprueba la conexión pidiéndole una a la base y validándola; si algo falla no lanza, devuelve DOWN,
 * para que /health responda 503 en vez de 500.
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

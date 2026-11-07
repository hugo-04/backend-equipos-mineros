package com.mineria.equipos.infrastructure.adapter.out.persistence;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.mineria.equipos.application.port.out.SaludBaseDatosPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Comprueba la conexión a la base de datos pidiéndole una conexión y validándola.
 * Si algo falla no lanza: devuelve false para que /health responda 503 en vez de 500.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaludBaseDatosAdapter implements SaludBaseDatosPort {

    private static final int TIMEOUT_SEGUNDOS = 2;

    private final DataSource dataSource;

    @Override
    public boolean estaDisponible() {
        try (Connection conexion = dataSource.getConnection()) {
            return conexion.isValid(TIMEOUT_SEGUNDOS);
        } catch (SQLException e) {
            log.error("La base de datos no responde: {}", e.getMessage());
            return false;
        }
    }
}

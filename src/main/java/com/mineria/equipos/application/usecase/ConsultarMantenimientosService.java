package com.mineria.equipos.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.application.port.in.ConsultarMantenimientosUseCase;
import com.mineria.equipos.application.port.out.EquipoRepositoryPort;
import com.mineria.equipos.application.port.out.MantenimientoQueryPort;
import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.domain.exception.EquipoNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Devuelve el historial de un equipo. Primero confirma que el equipo exista: así un id
 * inexistente responde 404 en lugar de una lista vacía que parecería "sin mantenimientos".
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarMantenimientosService implements ConsultarMantenimientosUseCase {

    private final EquipoRepositoryPort equipoRepository;
    private final MantenimientoQueryPort mantenimientoQuery;

    @Override
    public List<MantenimientoView> listarPorEquipo(Long equipoId) {
        if (!equipoRepository.existePorId(equipoId)) {
            throw new EquipoNotFoundException(equipoId);
        }
        return mantenimientoQuery.listarPorEquipo(equipoId);
    }
}

package com.mineria.equipos.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.application.port.in.EliminarEquipoUseCase;
import com.mineria.equipos.application.port.out.EquipoRepositoryPort;
import com.mineria.equipos.domain.exception.DomainException;
import com.mineria.equipos.domain.exception.EquipoNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Elimina un equipo. El historial de mantenimientos se borra solo por el ON DELETE CASCADE de la BD. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EliminarEquipoService implements EliminarEquipoUseCase {

    private final EquipoRepositoryPort equipoRepository;

    @Override
    @Transactional
    public void eliminar(Long id) {
        try {
            if (!equipoRepository.existePorId(id)) {
                throw new EquipoNotFoundException(id);
            }
            equipoRepository.eliminarPorId(id);
            log.info("Equipo eliminado: id={}", id);
        } catch (DomainException e) {
            log.warn("No se pudo eliminar el equipo {}: {}", id, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Fallo inesperado al eliminar el equipo {}", id, e);
            throw e;
        }
    }
}

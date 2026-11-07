package com.mineria.equipos.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.application.port.in.ActualizarEquipoUseCase;
import com.mineria.equipos.application.port.out.EquipoRepositoryPort;
import com.mineria.equipos.application.port.out.TipoEquipoRepositoryPort;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.domain.exception.CodigoDuplicadoException;
import com.mineria.equipos.domain.exception.DomainException;
import com.mineria.equipos.domain.exception.EquipoNotFoundException;
import com.mineria.equipos.domain.exception.TipoEquipoNotFoundException;
import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.domain.model.TipoEquipo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Edita un equipo. La regla de que el horómetro no pueda bajar vive en el modelo
 * ({@link Equipo#actualizar}); este caso de uso solo coordina las búsquedas y el guardado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActualizarEquipoService implements ActualizarEquipoUseCase {

    private final EquipoRepositoryPort equipoRepository;
    private final TipoEquipoRepositoryPort tipoEquipoRepository;
    private final EquipoViewAssembler assembler;

    @Override
    @Transactional
    public EquipoView actualizar(Comando comando) {
        try {
            Equipo actual = equipoRepository.buscarPorId(comando.id())
                    .orElseThrow(() -> new EquipoNotFoundException(comando.id()));
            TipoEquipo tipo = tipoEquipoRepository.buscarPorId(comando.tipoId())
                    .orElseThrow(() -> new TipoEquipoNotFoundException(comando.tipoId()));

            // Lanza HorometroInvalidoException si el nuevo valor es menor al actual
            Equipo actualizado = actual.actualizar(comando.codigo(), tipo, comando.horometro());

            if (equipoRepository.existePorCodigoDistintoDe(comando.codigo(), comando.id())) {
                throw new CodigoDuplicadoException(comando.codigo());
            }

            equipoRepository.guardar(actualizado);
            log.info("Equipo actualizado: id={}, horometro={}", comando.id(), actualizado.getHorometro());
            return assembler.porId(comando.id());
        } catch (DomainException e) {
            log.warn("No se pudo actualizar el equipo {}: {}", comando.id(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Fallo inesperado al actualizar el equipo {}", comando.id(), e);
            throw e;
        }
    }
}

package com.mineria.equipos.application.usecase;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.application.port.in.CrearEquipoUseCase;
import com.mineria.equipos.application.port.out.EquipoRepositoryPort;
import com.mineria.equipos.application.port.out.TipoEquipoRepositoryPort;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.domain.exception.CodigoDuplicadoException;
import com.mineria.equipos.domain.exception.DomainException;
import com.mineria.equipos.domain.exception.TipoEquipoNotFoundException;
import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.domain.model.TipoEquipo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Da de alta un equipo: valida que el tipo exista y que el código no esté repetido,
 * y lo guarda con el ciclo de mantenimiento en 0.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrearEquipoService implements CrearEquipoUseCase {

    private final EquipoRepositoryPort equipoRepository;
    private final TipoEquipoRepositoryPort tipoEquipoRepository;
    private final EquipoViewAssembler assembler;
    private final Clock clock;

    @Override
    @Transactional
    public EquipoView crear(Comando comando) {
        try {
            TipoEquipo tipo = tipoEquipoRepository.buscarPorId(comando.tipoId())
                    .orElseThrow(() -> new TipoEquipoNotFoundException(comando.tipoId()));

            if (equipoRepository.existePorCodigo(comando.codigo())) {
                throw new CodigoDuplicadoException(comando.codigo());
            }

            Equipo guardado = equipoRepository.guardar(
                    Equipo.nuevo(comando.codigo(), tipo, comando.horometro(), clock.instant()));
            log.info("Equipo creado: id={}, codigo={}", guardado.getId(), guardado.getCodigo());
            return assembler.porId(guardado.getId());
        } catch (DomainException e) {
            // Error de negocio esperado: se avisa y se deja que la capa web lo traduzca a HTTP
            log.warn("No se pudo crear el equipo {}: {}", comando.codigo(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Fallo inesperado al crear el equipo {}", comando.codigo(), e);
            throw e;
        }
    }
}

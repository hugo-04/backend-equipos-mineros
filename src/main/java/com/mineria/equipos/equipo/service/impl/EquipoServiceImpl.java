package com.mineria.equipos.equipo.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.equipo.entity.Equipo;
import com.mineria.equipos.equipo.mapper.EquipoMapper;
import com.mineria.equipos.equipo.repository.EquipoRepository;
import com.mineria.equipos.equipo.service.EquipoService;
import com.mineria.equipos.generated.model.EquipoRequest;
import com.mineria.equipos.generated.model.EquipoResponse;
import com.mineria.equipos.shared.exception.CodigoDuplicadoException;
import com.mineria.equipos.shared.exception.DomainException;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.tipo.service.TipoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Casos de uso de equipos. Coordina las búsquedas y el guardado; las reglas propias del equipo
 * (el horómetro no baja, reinicio del ciclo) viven en la entidad y la de mantenimiento en la política.
 * Para saber si un tipo existe le pregunta a {@link TipoService}, nunca al repositorio del módulo tipo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipoServiceImpl implements EquipoService {

    /** Nombre de la restricción única del código en la migración V2. */
    private static final String RESTRICCION_CODIGO_UNICO = "uk_equipo_codigo";

    private final EquipoRepository repository;
    private final EquipoMapper mapper;
    private final TipoService tipoService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<EquipoResponse> listar() {
        return mapper.toResponses(repository.listarProyecciones());
    }

    @Override
    @Transactional(readOnly = true)
    public EquipoResponse obtener(Long id) {
        return consultar(id);
    }

    @Override
    @Transactional
    public EquipoResponse crear(EquipoRequest request) {
        try {
            tipoService.verificarExiste(request.getTipoId());
            if (repository.existsByCodigo(request.getCodigo())) {
                throw new CodigoDuplicadoException(request.getCodigo());
            }

            Equipo nuevo = mapper.toEntity(request);
            nuevo.darDeAlta(clock.instant());
            Equipo guardado = guardar(nuevo);
            log.info("Equipo creado: id={}, codigo={}", guardado.getId(), guardado.getCodigo());
            return consultar(guardado.getId());
        } catch (DomainException e) {
            // Error de negocio esperado: se avisa y se deja que el handler global lo traduzca a HTTP
            log.warn("No se pudo crear el equipo {}: {}", request.getCodigo(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Fallo inesperado al crear el equipo {}", request.getCodigo(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public EquipoResponse actualizar(Long id, EquipoRequest request) {
        try {
            Equipo equipo = repository.findById(id).orElseThrow(() -> new EquipoNotFoundException(id));
            tipoService.verificarExiste(request.getTipoId());

            // Lanza HorometroInvalidoException si el nuevo valor es menor al actual (antes de tocar nada)
            equipo.validarHorometroNuevo(request.getHorometro());

            if (repository.existsByCodigoAndIdNot(request.getCodigo(), id)) {
                throw new CodigoDuplicadoException(request.getCodigo());
            }

            equipo.actualizar(request.getCodigo(), request.getTipoId(), request.getHorometro());
            guardar(equipo);
            log.info("Equipo actualizado: id={}, horometro={}", id, equipo.getHorometro());
            return consultar(id);
        } catch (DomainException e) {
            log.warn("No se pudo actualizar el equipo {}: {}", id, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Fallo inesperado al actualizar el equipo {}", id, e);
            throw e;
        }
    }

    /** El historial de mantenimientos se borra solo por el ON DELETE CASCADE de la base de datos. */
    @Override
    @Transactional
    public void eliminar(Long id) {
        try {
            verificarExiste(id);
            repository.deleteById(id);
            repository.flush();
            log.info("Equipo eliminado: id={}", id);
        } catch (DomainException e) {
            log.warn("No se pudo eliminar el equipo {}: {}", id, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Fallo inesperado al eliminar el equipo {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void verificarExiste(Long id) {
        if (!repository.existsById(id)) {
            throw new EquipoNotFoundException(id);
        }
    }

    @Override
    @Transactional
    public BigDecimal reiniciarCiclo(Long id) {
        Equipo equipo = repository.findById(id).orElseThrow(() -> new EquipoNotFoundException(id));
        BigDecimal horometro = equipo.reiniciarCicloMantenimiento();
        repository.saveAndFlush(equipo);
        log.info("Ciclo de mantenimiento reiniciado: equipoId={}, horometro={}", id, horometro);
        return horometro;
    }

    private EquipoResponse consultar(Long id) {
        return repository.buscarProyeccionPorId(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EquipoNotFoundException(id));
    }

    private Equipo guardar(Equipo equipo) {
        try {
            // saveAndFlush fuerza el INSERT/UPDATE ya, para detectar aquí un código repetido
            return repository.saveAndFlush(equipo);
        } catch (DataIntegrityViolationException e) {
            // Dos altas simultáneas con el mismo código pasan el chequeo previo; la BD las frena
            if (violaCodigoUnico(e)) {
                throw new CodigoDuplicadoException(equipo.getCodigo());
            }
            throw e;
        }
    }

    private boolean violaCodigoUnico(DataIntegrityViolationException e) {
        return Optional.ofNullable(e.getMostSpecificCause().getMessage())
                .map(mensaje -> mensaje.toLowerCase(Locale.ROOT).contains(RESTRICCION_CODIGO_UNICO))
                .orElse(false);
    }
}

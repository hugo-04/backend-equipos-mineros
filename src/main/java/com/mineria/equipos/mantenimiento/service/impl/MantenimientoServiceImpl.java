package com.mineria.equipos.mantenimiento.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.equipo.service.EquipoService;
import com.mineria.equipos.generated.model.MantenimientoRequest;
import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.mantenimiento.entity.Mantenimiento;
import com.mineria.equipos.mantenimiento.mapper.MantenimientoMapper;
import com.mineria.equipos.mantenimiento.repository.MantenimientoRepository;
import com.mineria.equipos.mantenimiento.service.MantenimientoService;
import com.mineria.equipos.shared.exception.DomainException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registra y consulta mantenimientos. Para todo lo que toca al equipo (que exista, reiniciar su ciclo)
 * usa {@link EquipoService}: este módulo nunca accede al repositorio ni a la entidad de equipo.
 * Registrar ocurre en una sola transacción: si falla guardar el historial se deshace también el
 * reinicio del ciclo, así no queda nada a medias (ni historial sin reinicio, ni al revés).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MantenimientoServiceImpl implements MantenimientoService {

    private final MantenimientoRepository repository;
    private final MantenimientoMapper mapper;
    private final EquipoService equipoService;
    private final Clock clock;

    @Override
    @Transactional
    public MantenimientoResponse registrar(Long equipoId, MantenimientoRequest request) {
        try {
            // El módulo equipo reinicia el ciclo (404 si no existe) y devuelve el horómetro actual,
            // que es el que queda guardado en el historial
            BigDecimal horometro = equipoService.reiniciarCiclo(equipoId);
            Mantenimiento guardado = repository.saveAndFlush(
                    Mantenimiento.registrarPara(equipoId, horometro, request.getNotas(), clock.instant()));

            log.info("Mantenimiento registrado: equipoId={}, horometro={}", equipoId, guardado.getHorometroAlRegistrar());
            return repository.findProjectedById(guardado.getId())
                    .map(mapper::toResponse)
                    .orElseThrow(() -> new IllegalStateException(
                            "El mantenimiento " + guardado.getId() + " no se encontró después de guardarlo"));
        } catch (DomainException e) {
            log.warn("No se pudo registrar el mantenimiento del equipo {}: {}", equipoId, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Fallo inesperado al registrar el mantenimiento del equipo {}", equipoId, e);
            throw e;
        }
    }

    /**
     * Primero confirma que el equipo exista: así un id inexistente responde 404 en lugar de una
     * lista vacía que parecería "sin mantenimientos".
     */
    @Override
    @Transactional(readOnly = true)
    public List<MantenimientoResponse> listarPorEquipo(Long equipoId) {
        equipoService.verificarExiste(equipoId);
        return mapper.toResponses(repository.findByEquipoIdOrderByFechaDescIdDesc(equipoId));
    }
}

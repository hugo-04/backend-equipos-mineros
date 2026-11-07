package com.mineria.equipos.mantenimiento.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.equipo.service.EquipoService;
import com.mineria.equipos.generated.model.MantenimientoRequest;
import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.generated.model.PaginaMantenimientoResponse;
import com.mineria.equipos.mantenimiento.entity.Mantenimiento;
import com.mineria.equipos.mantenimiento.mapper.MantenimientoMapper;
import com.mineria.equipos.mantenimiento.repository.MantenimientoRepository;
import com.mineria.equipos.mantenimiento.service.MantenimientoService;
import com.mineria.equipos.shared.exception.DomainException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registra y consulta mantenimientos. Todo lo que tenga que ver con el equipo (que exista, reiniciar su
 * ciclo) lo pide a través de {@link EquipoService}; este módulo nunca toca el repositorio ni la entidad de
 * equipo. Registrar va en una sola transacción: si falla el guardado del historial también se deshace el
 * reinicio del ciclo, para que no quede un reinicio sin historial ni un historial sin reinicio.
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
            // El módulo equipo reinicia el ciclo (da 404 si el equipo no existe) y devuelve el horómetro
            // de ese momento, que es el que se guarda en el historial
            BigDecimal horometro = equipoService.reiniciarCiclo(equipoId);
            Mantenimiento guardado = repository.saveAndFlush(
                    mapper.toEntity(equipoId, horometro, request.getNotas(), clock.instant()));

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
     * Antes de listar confirma que el equipo exista. Si no, un id que no existe devolvería una lista vacía
     * y parecería un equipo sin mantenimientos, cuando lo correcto es un 404.
     */
    @Override
    @Transactional(readOnly = true)
    public PaginaMantenimientoResponse listarPorEquipo(Long equipoId, Pageable pageable) {
        equipoService.verificarExiste(equipoId);
        return mapper.toPagina(repository.findByEquipoIdOrderByFechaDescIdDesc(equipoId, pageable));
    }
}

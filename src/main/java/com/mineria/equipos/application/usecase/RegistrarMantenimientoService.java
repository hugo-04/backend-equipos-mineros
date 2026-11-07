package com.mineria.equipos.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.application.port.in.RegistrarMantenimientoUseCase;
import com.mineria.equipos.equipo.service.EquipoService;
import com.mineria.equipos.application.port.out.MantenimientoQueryPort;
import com.mineria.equipos.application.port.out.MantenimientoRepositoryPort;
import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.shared.exception.DomainException;
import com.mineria.equipos.domain.model.Mantenimiento;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registra un mantenimiento. Todo ocurre en una sola transacción: si falla guardar el historial
 * o reiniciar el ciclo del equipo, no queda nada a medias (ni historial sin reinicio, ni al revés).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrarMantenimientoService implements RegistrarMantenimientoUseCase {

    private final EquipoService equipoService;
    private final MantenimientoRepositoryPort mantenimientoRepository;
    private final MantenimientoQueryPort mantenimientoQuery;
    private final Clock clock;

    @Override
    @Transactional
    public MantenimientoView registrar(Comando comando) {
        try {
            // El módulo equipo reinicia el ciclo (dentro de esta misma transacción) y devuelve el horómetro actual,
            // que es el que queda guardado en el historial
            BigDecimal horometro = equipoService.reiniciarCiclo(comando.equipoId());
            Mantenimiento guardado = mantenimientoRepository.guardar(
                    Mantenimiento.registrarPara(comando.equipoId(), horometro, comando.notas(), clock.instant()));

            log.info("Mantenimiento registrado: equipoId={}, horometro={}",
                    comando.equipoId(), guardado.getHorometroAlRegistrar());
            return mantenimientoQuery.buscarPorId(guardado.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "El mantenimiento " + guardado.getId() + " no se encontró después de guardarlo"));
        } catch (DomainException e) {
            log.warn("No se pudo registrar el mantenimiento del equipo {}: {}", comando.equipoId(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Fallo inesperado al registrar el mantenimiento del equipo {}", comando.equipoId(), e);
            throw e;
        }
    }
}

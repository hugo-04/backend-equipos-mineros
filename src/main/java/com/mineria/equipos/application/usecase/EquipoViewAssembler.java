package com.mineria.equipos.application.usecase;

import java.util.List;

import org.springframework.stereotype.Component;

import com.mineria.equipos.application.port.out.EquipoQueryPort;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.domain.model.EstadoMantenimiento;
import com.mineria.equipos.domain.rule.MantenimientoPolicy;

import lombok.RequiredArgsConstructor;

/**
 * Arma los {@link EquipoView} que ve el cliente. La consulta trae los datos crudos y aquí
 * se les agrega, al vuelo, si requieren mantenimiento (aplicando la política del dominio).
 * Así la regla vive en un solo lugar y nunca se guarda un valor que pueda quedar desactualizado.
 */
@Component
@RequiredArgsConstructor
class EquipoViewAssembler {

    private final EquipoQueryPort equipoQuery;
    private final MantenimientoPolicy mantenimientoPolicy;

    EquipoView porId(Long id) {
        return equipoQuery.buscarVistaPorId(id)
                .map(this::conEstadoDeMantenimiento)
                .orElseThrow(() -> new EquipoNotFoundException(id));
    }

    List<EquipoView> todos() {
        return equipoQuery.listar().stream()
                .map(this::conEstadoDeMantenimiento)
                .toList();
    }

    private EquipoView conEstadoDeMantenimiento(EquipoView vista) {
        boolean requiere = mantenimientoPolicy.requiereMantenimiento(
                vista.getHorometro(), vista.getHorometroUltimoMant(), vista.getIntervaloHoras());
        return vista.toBuilder()
                .requiereMantenimiento(requiere)
                .estado(EstadoMantenimiento.de(requiere).getEtiqueta())
                .build();
    }
}

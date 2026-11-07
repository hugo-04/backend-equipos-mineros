package com.mineria.equipos.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.application.port.in.ConsultarEquiposUseCase;
import com.mineria.equipos.application.view.EquipoView;

import lombok.RequiredArgsConstructor;

/** Consultas de lectura de equipos. Delegan en el ensamblador, que calcula el estado de mantenimiento. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarEquiposService implements ConsultarEquiposUseCase {

    private final EquipoViewAssembler assembler;

    @Override
    public List<EquipoView> listar() {
        return assembler.todos();
    }

    @Override
    public EquipoView obtener(Long id) {
        return assembler.porId(id);
    }
}

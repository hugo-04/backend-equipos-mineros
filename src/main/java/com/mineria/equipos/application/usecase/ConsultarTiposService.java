package com.mineria.equipos.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.application.port.in.ConsultarTiposUseCase;
import com.mineria.equipos.application.port.out.TipoEquipoQueryPort;
import com.mineria.equipos.application.view.TipoEquipoView;

import lombok.RequiredArgsConstructor;

/** Lista el catálogo de tipos de equipo con su intervalo de mantenimiento. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarTiposService implements ConsultarTiposUseCase {

    private final TipoEquipoQueryPort tipoEquipoQuery;

    @Override
    public List<TipoEquipoView> listar() {
        return tipoEquipoQuery.listar();
    }
}

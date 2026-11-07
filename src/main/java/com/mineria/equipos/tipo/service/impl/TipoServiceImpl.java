package com.mineria.equipos.tipo.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.generated.model.TipoEquipoResponse;
import com.mineria.equipos.shared.exception.TipoEquipoNotFoundException;
import com.mineria.equipos.tipo.mapper.TipoMapper;
import com.mineria.equipos.tipo.repository.TipoRepository;
import com.mineria.equipos.tipo.service.TipoService;

import lombok.RequiredArgsConstructor;

/** Lista el catálogo de tipos de equipo con su intervalo de mantenimiento y verifica que un tipo exista. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TipoServiceImpl implements TipoService {

    private final TipoRepository repository;
    private final TipoMapper mapper;

    @Override
    public List<TipoEquipoResponse> listar() {
        return mapper.toResponses(repository.findAllProjectedByOrderByIdAsc());
    }

    @Override
    public void verificarExiste(Long id) {
        if (!repository.existsById(id)) {
            throw new TipoEquipoNotFoundException(id);
        }
    }
}

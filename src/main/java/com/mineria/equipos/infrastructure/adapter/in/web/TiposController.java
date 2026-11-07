package com.mineria.equipos.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.mineria.equipos.application.port.in.ConsultarTiposUseCase;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.api.TiposApi;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.TipoEquipoResponse;
import com.mineria.equipos.infrastructure.adapter.in.web.mapper.EquipoWebMapper;

import lombok.RequiredArgsConstructor;

/** Endpoint del catálogo de tipos de equipo. */
@RestController
@RequiredArgsConstructor
public class TiposController implements TiposApi {

    private final ConsultarTiposUseCase consultarTipos;
    private final EquipoWebMapper mapper;

    @Override
    public ResponseEntity<List<TipoEquipoResponse>> listarTipos() {
        return ResponseEntity.ok(mapper.toTipoResponses(consultarTipos.listar()));
    }
}

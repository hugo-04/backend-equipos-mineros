package com.mineria.equipos.tipo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.mineria.equipos.generated.api.TiposApi;
import com.mineria.equipos.generated.model.PaginaTipoEquipoResponse;
import com.mineria.equipos.shared.pagination.Paginacion;
import com.mineria.equipos.tipo.service.TipoService;

import lombok.RequiredArgsConstructor;

/** Endpoint (paginado) del catálogo de tipos de equipo. Solo delega en el servicio. */
@RestController
@RequiredArgsConstructor
public class TipoController implements TiposApi {

    private final TipoService tipoService;

    @Override
    public ResponseEntity<PaginaTipoEquipoResponse> listarTipos(Integer page, Integer size) {
        return ResponseEntity.ok(tipoService.listar(Paginacion.de(page, size)));
    }
}

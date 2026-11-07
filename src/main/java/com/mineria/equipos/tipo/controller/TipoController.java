package com.mineria.equipos.tipo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.mineria.equipos.generated.api.TiposApi;
import com.mineria.equipos.generated.model.TipoEquipoResponse;
import com.mineria.equipos.tipo.service.TipoService;

import lombok.RequiredArgsConstructor;

/** Endpoint del catálogo de tipos de equipo. Solo delega en el servicio. */
@RestController
@RequiredArgsConstructor
public class TipoController implements TiposApi {

    private final TipoService tipoService;

    @Override
    public ResponseEntity<List<TipoEquipoResponse>> listarTipos() {
        return ResponseEntity.ok(tipoService.listar());
    }
}

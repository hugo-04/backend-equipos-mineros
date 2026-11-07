package com.mineria.equipos.mantenimiento.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.mineria.equipos.generated.api.MantenimientosApi;
import com.mineria.equipos.generated.model.MantenimientoRequest;
import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.generated.model.PaginaMantenimientoResponse;
import com.mineria.equipos.mantenimiento.service.MantenimientoService;
import com.mineria.equipos.shared.pagination.Paginacion;

import lombok.RequiredArgsConstructor;

/** Endpoints del historial de mantenimientos de un equipo. Solo delega en el servicio. */
@RestController
@RequiredArgsConstructor
public class MantenimientoController implements MantenimientosApi {

    private final MantenimientoService mantenimientoService;

    @Override
    public ResponseEntity<MantenimientoResponse> registrarMantenimiento(Long id, MantenimientoRequest request) {
        MantenimientoResponse registrado = mantenimientoService.registrar(id, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(ubicacion).body(registrado);
    }

    @Override
    public ResponseEntity<PaginaMantenimientoResponse> listarMantenimientos(Long id, Integer page, Integer size) {
        return ResponseEntity.ok(mantenimientoService.listarPorEquipo(id, Paginacion.de(page, size)));
    }
}

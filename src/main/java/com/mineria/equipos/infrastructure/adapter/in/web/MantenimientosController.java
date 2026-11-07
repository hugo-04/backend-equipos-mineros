package com.mineria.equipos.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.mineria.equipos.application.port.in.ConsultarMantenimientosUseCase;
import com.mineria.equipos.application.port.in.RegistrarMantenimientoUseCase;
import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.generated.api.MantenimientosApi;
import com.mineria.equipos.generated.model.MantenimientoRequest;
import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.infrastructure.adapter.in.web.mapper.MantenimientoWebMapper;

import lombok.RequiredArgsConstructor;

/** Endpoints del historial de mantenimientos de un equipo. */
@RestController
@RequiredArgsConstructor
public class MantenimientosController implements MantenimientosApi {

    private final RegistrarMantenimientoUseCase registrarMantenimiento;
    private final ConsultarMantenimientosUseCase consultarMantenimientos;
    private final MantenimientoWebMapper mapper;

    @Override
    public ResponseEntity<MantenimientoResponse> registrarMantenimiento(Long id, MantenimientoRequest request) {
        MantenimientoView registrado = registrarMantenimiento.registrar(mapper.toComando(id, request));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(ubicacion).body(mapper.toResponse(registrado));
    }

    @Override
    public ResponseEntity<List<MantenimientoResponse>> listarMantenimientos(Long id) {
        return ResponseEntity.ok(mapper.toResponses(consultarMantenimientos.listarPorEquipo(id)));
    }
}

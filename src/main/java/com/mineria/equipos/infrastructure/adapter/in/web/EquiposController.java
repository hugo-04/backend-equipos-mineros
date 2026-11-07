package com.mineria.equipos.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.mineria.equipos.application.port.in.ActualizarEquipoUseCase;
import com.mineria.equipos.application.port.in.ConsultarEquiposUseCase;
import com.mineria.equipos.application.port.in.CrearEquipoUseCase;
import com.mineria.equipos.application.port.in.EliminarEquipoUseCase;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.api.EquiposApi;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.EquipoRequest;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.EquipoResponse;
import com.mineria.equipos.infrastructure.adapter.in.web.mapper.EquipoWebMapper;

import lombok.RequiredArgsConstructor;

/** Endpoints de equipos. Traduce DTOs a comandos, delega en los casos de uso y devuelve el DTO. */
@RestController
@RequiredArgsConstructor
public class EquiposController implements EquiposApi {

    private final CrearEquipoUseCase crearEquipo;
    private final ActualizarEquipoUseCase actualizarEquipo;
    private final EliminarEquipoUseCase eliminarEquipo;
    private final ConsultarEquiposUseCase consultarEquipos;
    private final EquipoWebMapper mapper;

    @Override
    public ResponseEntity<List<EquipoResponse>> listarEquipos() {
        return ResponseEntity.ok(mapper.toResponses(consultarEquipos.listar()));
    }

    @Override
    public ResponseEntity<EquipoResponse> crearEquipo(EquipoRequest equipoRequest) {
        EquipoView creado = crearEquipo.crear(mapper.toCrearComando(equipoRequest));
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(mapper.toResponse(creado));
    }

    @Override
    public ResponseEntity<EquipoResponse> obtenerEquipo(Long id) {
        return ResponseEntity.ok(mapper.toResponse(consultarEquipos.obtener(id)));
    }

    @Override
    public ResponseEntity<EquipoResponse> actualizarEquipo(Long id, EquipoRequest equipoRequest) {
        EquipoView actualizado = actualizarEquipo.actualizar(mapper.toActualizarComando(id, equipoRequest));
        return ResponseEntity.ok(mapper.toResponse(actualizado));
    }

    @Override
    public ResponseEntity<Void> eliminarEquipo(Long id) {
        eliminarEquipo.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

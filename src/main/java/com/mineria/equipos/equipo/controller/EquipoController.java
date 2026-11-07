package com.mineria.equipos.equipo.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.mineria.equipos.equipo.service.EquipoService;
import com.mineria.equipos.generated.api.EquiposApi;
import com.mineria.equipos.generated.model.EquipoRequest;
import com.mineria.equipos.generated.model.EquipoResponse;

import lombok.RequiredArgsConstructor;

/** Endpoints de equipos (implementa la interfaz generada desde el contrato). Solo delega en el servicio. */
@RestController
@RequiredArgsConstructor
public class EquipoController implements EquiposApi {

    private final EquipoService equipoService;

    @Override
    public ResponseEntity<List<EquipoResponse>> listarEquipos() {
        return ResponseEntity.ok(equipoService.listar());
    }

    @Override
    public ResponseEntity<EquipoResponse> crearEquipo(EquipoRequest equipoRequest) {
        EquipoResponse creado = equipoService.crear(equipoRequest);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @Override
    public ResponseEntity<EquipoResponse> obtenerEquipo(Long id) {
        return ResponseEntity.ok(equipoService.obtener(id));
    }

    @Override
    public ResponseEntity<EquipoResponse> actualizarEquipo(Long id, EquipoRequest equipoRequest) {
        return ResponseEntity.ok(equipoService.actualizar(id, equipoRequest));
    }

    @Override
    public ResponseEntity<Void> eliminarEquipo(Long id) {
        equipoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

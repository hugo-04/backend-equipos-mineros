package com.mineria.equipos.application.usecase;

import org.springframework.stereotype.Service;

import com.mineria.equipos.application.port.in.VerificarSaludUseCase;
import com.mineria.equipos.application.port.out.SaludBaseDatosPort;
import com.mineria.equipos.application.view.EstadoSalud;

import lombok.RequiredArgsConstructor;

/** El servicio está "UP" solo si la base de datos responde; si no, "DOWN" (la web lo traduce a 503). */
@Service
@RequiredArgsConstructor
public class VerificarSaludService implements VerificarSaludUseCase {

    private final SaludBaseDatosPort saludBaseDatos;

    @Override
    public EstadoSalud verificar() {
        return saludBaseDatos.estaDisponible() ? EstadoSalud.UP : EstadoSalud.DOWN;
    }
}

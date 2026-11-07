package com.mineria.equipos.application.port.in;

import com.mineria.equipos.application.view.EstadoSalud;

/** Caso de uso: saber si el servicio y su base de datos están funcionando. */
public interface VerificarSaludUseCase {

    EstadoSalud verificar();
}

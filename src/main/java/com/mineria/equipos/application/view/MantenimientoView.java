package com.mineria.equipos.application.view;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Builder;
import lombok.Value;

/** Modelo de lectura de una línea del historial de mantenimientos. */
@Value
@Builder
public class MantenimientoView {

    Long id;
    Long equipoId;
    Instant fecha;
    BigDecimal horometroAlRegistrar;
    String notas;
}

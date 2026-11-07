package com.mineria.equipos.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Builder;
import lombok.Value;

/**
 * Registro histórico de un mantenimiento hecho a un equipo.
 * Guarda el horómetro que tenía el equipo en ese momento, para poder auditar los ciclos.
 */
@Value
@Builder(toBuilder = true)
public class Mantenimiento {

    Long id;
    Long equipoId;
    Instant fecha;
    BigDecimal horometroAlRegistrar;
    String notas;

    /** Factory: arma el mantenimiento a partir del estado actual del equipo. */
    public static Mantenimiento registrarPara(Equipo equipo, String notas, Instant fecha) {
        return Mantenimiento.builder()
                .equipoId(equipo.getId())
                .fecha(fecha)
                .horometroAlRegistrar(equipo.getHorometro())
                .notas(notas)
                .build();
    }
}

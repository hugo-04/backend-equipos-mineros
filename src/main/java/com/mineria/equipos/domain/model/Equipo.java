package com.mineria.equipos.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.mineria.equipos.domain.exception.HorometroInvalidoException;
import com.mineria.equipos.domain.rule.MantenimientoPolicy;

import lombok.Builder;
import lombok.Value;

/**
 * Un equipo minero del registro. Es inmutable: cada cambio devuelve una copia nueva,
 * así el estado solo se modifica a través de los métodos que aplican las reglas de negocio.
 */
@Value
@Builder(toBuilder = true)
public class Equipo {

    Long id;
    String codigo;
    TipoEquipo tipo;
    BigDecimal horometro;
    BigDecimal horometroUltimoMant;
    Instant creadoEn;

    /** Factory: un equipo recién dado de alta empieza su ciclo de mantenimiento en 0. */
    public static Equipo nuevo(String codigo, TipoEquipo tipo, BigDecimal horometro, Instant creadoEn) {
        validarHorometroNoNegativo(horometro);
        return Equipo.builder()
                .codigo(codigo)
                .tipo(tipo)
                .horometro(horometro)
                .horometroUltimoMant(BigDecimal.ZERO)
                .creadoEn(creadoEn)
                .build();
    }

    /** Edita los datos del equipo. El horómetro puede quedarse igual o subir, nunca bajar. */
    public Equipo actualizar(String nuevoCodigo, TipoEquipo nuevoTipo, BigDecimal nuevoHorometro) {
        validarHorometroNoNegativo(nuevoHorometro);
        if (nuevoHorometro.compareTo(horometro) < 0) {
            throw new HorometroInvalidoException(
                    "El horómetro no puede bajar: actual " + horometro + ", recibido " + nuevoHorometro);
        }
        return toBuilder()
                .codigo(nuevoCodigo)
                .tipo(nuevoTipo)
                .horometro(nuevoHorometro)
                .build();
    }

    /** Reinicia el ciclo de mantenimiento: el horómetro actual pasa a ser el del último mantenimiento. */
    public Equipo reiniciarCicloMantenimiento() {
        return toBuilder().horometroUltimoMant(horometro).build();
    }

    public boolean requiereMantenimiento(MantenimientoPolicy policy) {
        return policy.requiereMantenimiento(horometro, horometroUltimoMant, tipo.getIntervaloHoras());
    }

    private static void validarHorometroNoNegativo(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new HorometroInvalidoException("El horómetro debe ser un número mayor o igual a 0");
        }
    }
}

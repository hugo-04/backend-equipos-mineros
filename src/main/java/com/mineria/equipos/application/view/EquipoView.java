package com.mineria.equipos.application.view;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Builder;
import lombok.Value;

/**
 * Modelo de lectura de un equipo: lo que devuelven las consultas GET.
 * Trae solo los campos que la API necesita (incluido el tipo "aplanado") para no
 * cargar entidades completas. "requiereMantenimiento" y "estado" no se guardan:
 * los calcula el caso de uso al momento de consultar.
 */
@Value
@Builder(toBuilder = true)
public class EquipoView {

    Long id;
    String codigo;
    Long tipoId;
    String tipoNombre;
    int intervaloHoras;
    BigDecimal horometro;
    BigDecimal horometroUltimoMant;
    Instant creadoEn;
    boolean requiereMantenimiento;
    String estado;
}

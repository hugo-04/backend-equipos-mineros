package com.mineria.equipos.equipo.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un equipo minero del registro (fila de la tabla equipo). Solo guarda datos: las reglas de negocio
 * (el horómetro no baja, el ciclo de mantenimiento) viven en {@code EquipoService}.
 * El tipo se guarda solo como {@code tipoId}: el módulo tipo es el dueño de esa entidad y este módulo
 * le pregunta a través de {@code TipoService}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "equipo")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(name = "tipo_id", nullable = false)
    private Long tipoId;

    @Column(nullable = false, precision = 10, scale = 1)
    private BigDecimal horometro;

    @Column(name = "horometro_ultimo_mant", nullable = false, precision = 10, scale = 1)
    private BigDecimal horometroUltimoMant;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;
}

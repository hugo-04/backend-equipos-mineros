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
 * Un equipo del registro. La entidad solo guarda datos; las reglas (que el horómetro no baje, el ciclo de
 * mantenimiento) están en {@code EquipoService}. El tipo se guarda solo como {@code tipoId} porque la
 * entidad de tipo es del módulo tipo; si hace falta algo de ahí se le pregunta a {@code TipoService}.
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

    // updatable = false: el tipo no cambia nunca después de crear el equipo. Así, aunque algún código llame al
    // setter, Hibernate no incluye tipo_id en el UPDATE.
    @Column(name = "tipo_id", nullable = false, updatable = false)
    private Long tipoId;

    @Column(nullable = false, precision = 10, scale = 1)
    private BigDecimal horometro;

    @Column(name = "horometro_ultimo_mant", nullable = false, precision = 10, scale = 1)
    private BigDecimal horometroUltimoMant;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;
}

package com.mineria.equipos.infrastructure.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.mineria.equipos.tipo.entity.TipoEquipo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fila de la tabla equipo. El tipo se carga perezoso para no traer datos que no se piden. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "equipo")
public class EquipoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false)
    private TipoEquipo tipo;

    @Column(nullable = false, precision = 10, scale = 1)
    private BigDecimal horometro;

    @Column(name = "horometro_ultimo_mant", nullable = false, precision = 10, scale = 1)
    private BigDecimal horometroUltimoMant;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;
}

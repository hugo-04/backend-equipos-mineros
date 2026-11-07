package com.mineria.equipos.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fila de la tabla tipo_equipo. Solo la usa la capa de persistencia; el dominio trabaja con TipoEquipo. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tipo_equipo")
public class TipoEquipoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "intervalo_horas", nullable = false)
    private int intervaloHoras;
}

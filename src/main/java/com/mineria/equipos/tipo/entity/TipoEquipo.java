package com.mineria.equipos.tipo.entity;

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
 * Categoría de un equipo (camión, excavadora, perforadora...), fila de la tabla tipo_equipo.
 * Define cada cuántas horas de uso el equipo necesita mantenimiento.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tipo_equipo")
public class TipoEquipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "intervalo_horas", nullable = false)
    private int intervaloHoras;
}

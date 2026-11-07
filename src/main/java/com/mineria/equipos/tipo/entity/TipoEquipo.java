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
 * La categoría de un equipo (camión, excavadora, perforadora...). Lo que importa de acá es el
 * intervaloHoras: cada cuántas horas de uso le toca mantenimiento a los equipos de ese tipo.
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

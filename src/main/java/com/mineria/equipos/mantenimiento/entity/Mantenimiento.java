package com.mineria.equipos.mantenimiento.entity;

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
 * Una línea del historial: un mantenimiento que se le hizo a un equipo. Guarda el horómetro que tenía el
 * equipo en ese momento, para poder revisar después cada ciclo. Solo lleva el id del equipo, sin relación
 * JPA, porque esa entidad es del módulo equipo; si el equipo se borra, la base borra su historial en cascada.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mantenimiento")
public class Mantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipo_id", nullable = false)
    private Long equipoId;

    @Column(nullable = false)
    private Instant fecha;

    @Column(name = "horometro_al_registrar", nullable = false, precision = 10, scale = 1)
    private BigDecimal horometroAlRegistrar;

    @Column(length = 500)
    private String notas;
}

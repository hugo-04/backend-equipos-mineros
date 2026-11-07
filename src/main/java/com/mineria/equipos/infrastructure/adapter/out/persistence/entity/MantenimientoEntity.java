package com.mineria.equipos.infrastructure.adapter.out.persistence.entity;

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
 * Fila de la tabla mantenimiento. Guarda solo el id del equipo (sin relación JPA) porque
 * el historial siempre se consulta por equipo y el borrado en cascada lo hace la base de datos.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mantenimiento")
public class MantenimientoEntity {

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

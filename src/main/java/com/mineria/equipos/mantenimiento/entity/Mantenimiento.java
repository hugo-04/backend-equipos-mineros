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
 * Registro histórico de un mantenimiento hecho a un equipo (fila de la tabla mantenimiento).
 * Guarda el horómetro que tenía el equipo en ese momento, para poder auditar los ciclos.
 * Solo guarda el id del equipo (sin relación JPA): el módulo equipo es el dueño de esa entidad
 * y el borrado en cascada del historial lo hace la base de datos.
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

    /** Factory: arma el mantenimiento con el horómetro que tenía el equipo al registrarlo. */
    public static Mantenimiento registrarPara(Long equipoId, BigDecimal horometro, String notas, Instant fecha) {
        Mantenimiento mantenimiento = new Mantenimiento();
        mantenimiento.equipoId = equipoId;
        mantenimiento.horometroAlRegistrar = horometro;
        mantenimiento.notas = notas;
        mantenimiento.fecha = fecha;
        return mantenimiento;
    }
}

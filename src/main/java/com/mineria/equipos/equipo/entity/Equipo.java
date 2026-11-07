package com.mineria.equipos.equipo.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.mineria.equipos.shared.exception.HorometroInvalidoException;

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
 * Un equipo minero del registro (fila de la tabla equipo). Además de los datos guarda las reglas que solo
 * dependen de sí mismo: el horómetro nunca baja y registrar un mantenimiento reinicia el ciclo.
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
    private BigDecimal horometroUltimoMant = BigDecimal.ZERO;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** Alta de un equipo nuevo: el ciclo de mantenimiento empieza en 0. */
    public void darDeAlta(Instant momento) {
        validarHorometroNoNegativo(horometro);
        this.horometroUltimoMant = BigDecimal.ZERO;
        this.creadoEn = momento;
    }

    /** Verifica que el horómetro pueda cambiar al valor dado: no negativo y nunca menor al actual. */
    public void validarHorometroNuevo(BigDecimal nuevoHorometro) {
        validarHorometroNoNegativo(nuevoHorometro);
        if (nuevoHorometro.compareTo(horometro) < 0) {
            throw new HorometroInvalidoException(
                    "El horómetro no puede bajar: actual " + horometro + ", recibido " + nuevoHorometro);
        }
    }

    /** Edita los datos del equipo. El horómetro puede quedarse igual o subir, nunca bajar. */
    public void actualizar(String nuevoCodigo, Long nuevoTipoId, BigDecimal nuevoHorometro) {
        validarHorometroNuevo(nuevoHorometro);
        this.codigo = nuevoCodigo;
        this.tipoId = nuevoTipoId;
        this.horometro = nuevoHorometro;
    }

    /**
     * Reinicia el ciclo de mantenimiento: el horómetro actual pasa a ser el del último mantenimiento.
     *
     * @return el horómetro en el que quedó el ciclo reiniciado
     */
    public BigDecimal reiniciarCicloMantenimiento() {
        this.horometroUltimoMant = horometro;
        return horometro;
    }

    private static void validarHorometroNoNegativo(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new HorometroInvalidoException("El horómetro debe ser un número mayor o igual a 0");
        }
    }
}

package com.mineria.equipos.testsupport;

import java.math.BigDecimal;
import java.time.Instant;

import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.domain.model.Usuario;
import com.mineria.equipos.equipo.entity.Equipo;
import com.mineria.equipos.equipo.repository.projection.EquipoProjection;
import com.mineria.equipos.generated.model.EquipoResponse;
import com.mineria.equipos.generated.model.TipoEquipoResponse;

import lombok.Builder;
import lombok.Value;

/** Datos de ejemplo reutilizables en las pruebas, para no repetir builders en cada clase. */
public final class Datos {

    public static final Instant AHORA = Instant.parse("2026-06-15T12:00:00Z");

    private Datos() {
    }

    /** Equipo (entidad) con el tipo Camión (id 1) y el ciclo de mantenimiento en 0. */
    public static Equipo equipo(Long id, String codigo, String horometro) {
        Equipo equipo = new Equipo();
        equipo.setId(id);
        equipo.setCodigo(codigo);
        equipo.setTipoId(1L);
        equipo.setHorometro(new BigDecimal(horometro));
        equipo.setHorometroUltimoMant(BigDecimal.ZERO);
        equipo.setCreadoEn(AHORA);
        return equipo;
    }

    /** Proyección de lectura de un equipo con los datos de su tipo (Camión) ya unidos. */
    public static EquipoProjection equipoProjection(Long id, String codigo, String horometro, String ultimoMant,
                                                    int intervalo) {
        return EquipoProyeccion.builder()
                .id(id).codigo(codigo).tipoId(1L).tipoNombre("Camión").intervaloHoras(intervalo)
                .horometro(new BigDecimal(horometro)).horometroUltimoMant(new BigDecimal(ultimoMant))
                .creadoEn(AHORA).build();
    }

    public static EquipoResponse equipoResponse(Long id, String codigo, boolean requiere) {
        return new EquipoResponse(id, codigo, new TipoEquipoResponse(1L, "Camión", 250), new BigDecimal("300.0"),
                BigDecimal.ZERO, requiere, requiere ? "Requiere mantenimiento" : "OK", AHORA);
    }

    public static MantenimientoView mantenimientoView(Long id, Long equipoId, String horometro) {
        return MantenimientoView.builder()
                .id(id)
                .equipoId(equipoId)
                .fecha(AHORA)
                .horometroAlRegistrar(new BigDecimal(horometro))
                .notas("Cambio de aceite")
                .build();
    }

    public static Usuario admin() {
        return Usuario.builder().id(1L).username("admin").passwordHash("$2a$hash").rol(Rol.ADMIN).build();
    }

    /** Implementación simple de la proyección de equipos para armar casos de prueba. */
    @Value
    @Builder
    public static class EquipoProyeccion implements EquipoProjection {
        Long id;
        String codigo;
        Long tipoId;
        String tipoNombre;
        Integer intervaloHoras;
        BigDecimal horometro;
        BigDecimal horometroUltimoMant;
        Instant creadoEn;
    }
}

package com.mineria.equipos.testsupport;

import java.math.BigDecimal;
import java.time.Instant;

import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.domain.model.TipoEquipo;
import com.mineria.equipos.domain.model.Usuario;

/** Datos de ejemplo reutilizables en las pruebas, para no repetir builders en cada clase. */
public final class Datos {

    public static final Instant AHORA = Instant.parse("2026-06-15T12:00:00Z");

    private Datos() {
    }

    public static TipoEquipo camion() {
        return TipoEquipo.builder().id(1L).nombre("Camión").intervaloHoras(250).build();
    }

    public static Equipo equipo(Long id, String codigo, String horometro) {
        return Equipo.builder()
                .id(id)
                .codigo(codigo)
                .tipo(camion())
                .horometro(new BigDecimal(horometro))
                .horometroUltimoMant(BigDecimal.ZERO)
                .creadoEn(AHORA)
                .build();
    }

    public static EquipoView equipoView(Long id, String codigo, String horometro, String ultimoMant, int intervalo) {
        return EquipoView.builder()
                .id(id)
                .codigo(codigo)
                .tipoId(1L)
                .tipoNombre("Camión")
                .intervaloHoras(intervalo)
                .horometro(new BigDecimal(horometro))
                .horometroUltimoMant(new BigDecimal(ultimoMant))
                .creadoEn(AHORA)
                .build();
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
}

package com.mineria.equipos.shared.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Convierte los parámetros {@code page} y {@code size} de la URL en el {@link Pageable} de Spring Data.
 * Acá, y solo acá, se fija el tamaño por defecto y el máximo: así ningún listado puede devolver filas sin
 * límite aunque el contrato cambie. La primera página es la 0, igual que en Spring. Uso
 * {@code PageRequest.of(...)} porque los constructores de PageRequest están obsoletos.
 */
public final class Paginacion {

    public static final int PAGINA_INICIAL = 0;
    public static final int TAMANO_POR_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {
    }

    /** Si no mandan nada se usan los valores por defecto; si mandan algo fuera de rango, se ajusta al límite. */
    public static Pageable de(Integer pagina, Integer tamano) {
        int paginaSegura = pagina == null ? PAGINA_INICIAL : Math.max(PAGINA_INICIAL, pagina);
        int tamanoSeguro = tamano == null ? TAMANO_POR_DEFECTO : Math.clamp(tamano, 1, TAMANO_MAXIMO);
        return PageRequest.of(paginaSegura, tamanoSeguro);
    }
}

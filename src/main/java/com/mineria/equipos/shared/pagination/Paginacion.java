package com.mineria.equipos.shared.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Arma el {@link Pageable} de Spring Data a partir de los parámetros {@code page} y {@code size} de la URL.
 * Es el único lugar donde se decide el tamaño por defecto y el máximo, así ningún listado puede devolver
 * una cantidad ilimitada de filas aunque el contrato llegara a cambiar. Usa {@code PageRequest.of(...)},
 * la forma vigente (los constructores de PageRequest están obsoletos).
 */
public final class Paginacion {

    public static final int PAGINA_INICIAL = 0;
    public static final int TAMANO_POR_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {
    }

    /** Valores ausentes -> los de por defecto; valores fuera de rango -> se ajustan al rango permitido. */
    public static Pageable de(Integer pagina, Integer tamano) {
        int paginaSegura = pagina == null ? PAGINA_INICIAL : Math.max(PAGINA_INICIAL, pagina);
        int tamanoSeguro = tamano == null ? TAMANO_POR_DEFECTO : Math.clamp(tamano, 1, TAMANO_MAXIMO);
        return PageRequest.of(paginaSegura, tamanoSeguro);
    }
}

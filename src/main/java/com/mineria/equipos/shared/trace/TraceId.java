package com.mineria.equipos.shared.trace;

import org.slf4j.MDC;

/** Acceso al id de correlación de la petición en curso (lo fija {@link TraceIdFilter} en el MDC de los logs). */
public final class TraceId {

    /** Clave del MDC; el patrón de logs la imprime como {@code traceId=...}. */
    public static final String CLAVE_MDC = "traceId";

    /** Cabecera HTTP por la que viaja el id (de entrada y de salida). */
    public static final String CABECERA = "X-Trace-Id";

    private TraceId() {
    }

    /** @return el id de la petición actual, o null si se llama fuera de una petición */
    public static String actual() {
        return MDC.get(CLAVE_MDC);
    }
}

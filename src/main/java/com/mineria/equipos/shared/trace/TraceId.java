package com.mineria.equipos.shared.trace;

import org.slf4j.MDC;

/** Para leer el id de correlación de la petición en curso; quien lo pone en el MDC es {@link TraceIdFilter}. */
public final class TraceId {

    /** Clave en el MDC. El patrón de logs la imprime como {@code traceId=...}. */
    public static final String CLAVE_MDC = "traceId";

    /** Cabecera por la que viaja el id, tanto al entrar como al salir. */
    public static final String CABECERA = "X-Trace-Id";

    private TraceId() {
    }

    /** @return el id de la petición actual, o null si se llama fuera de una petición */
    public static String actual() {
        return MDC.get(CLAVE_MDC);
    }
}

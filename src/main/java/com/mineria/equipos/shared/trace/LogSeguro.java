package com.mineria.equipos.shared.trace;

/**
 * Limpia texto que viene del usuario (nombre de usuario, rutas...) antes de escribirlo en un log. Sin esto, alguien
 * podría mandar saltos de línea en un campo y fabricar líneas de log falsas (inyección de logs).
 */
public final class LogSeguro {

    private static final int LARGO_MAXIMO = 80;

    private LogSeguro() {
    }

    /** Cambia los caracteres de control (saltos de línea, tabulaciones...) por "_" y recorta el texto. */
    public static String limpiar(String texto) {
        if (texto == null) {
            return "null";
        }
        String sinControl = texto.replaceAll("\\p{Cntrl}", "_");
        return sinControl.length() > LARGO_MAXIMO ? sinControl.substring(0, LARGO_MAXIMO) + "..." : sinControl;
    }
}

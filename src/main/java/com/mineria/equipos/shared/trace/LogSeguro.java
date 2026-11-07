package com.mineria.equipos.shared.trace;

/**
 * Limpia lo que escribe el usuario (nombre de usuario, rutas...) antes de mandarlo a un log. Sin esto alguien
 * podría meter saltos de línea en un campo y fabricar líneas falsas en el log.
 */
public final class LogSeguro {

    private static final int LARGO_MAXIMO = 80;

    private LogSeguro() {
    }

    /** Cambia los caracteres de control (saltos de línea, tabs...) por "_" y recorta el texto largo. */
    public static String limpiar(String texto) {
        if (texto == null) {
            return "null";
        }
        String sinControl = texto.replaceAll("\\p{Cntrl}", "_");
        return sinControl.length() > LARGO_MAXIMO ? sinControl.substring(0, LARGO_MAXIMO) + "..." : sinControl;
    }
}

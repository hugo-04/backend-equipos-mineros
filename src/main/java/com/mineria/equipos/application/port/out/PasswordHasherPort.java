package com.mineria.equipos.application.port.out;

/** Puerto de salida para cifrar contraseñas y compararlas con su hash (BCrypt en la infraestructura). */
public interface PasswordHasherPort {

    String hashear(String passwordEnClaro);

    boolean coincide(String passwordEnClaro, String hash);
}

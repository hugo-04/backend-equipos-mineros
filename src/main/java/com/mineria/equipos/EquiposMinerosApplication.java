package com.mineria.equipos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del servicio "Registro de equipos mineros".
 * No tiene lógica: solo arranca Spring Boot y deja que cada capa se cablee sola.
 */
@SpringBootApplication
public class EquiposMinerosApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquiposMinerosApplication.class, args);
    }
}

package com.mineria.equipos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Por acá arranca el servicio "Registro de equipos mineros". No tiene lógica, solo levanta Spring Boot.
 */
@SpringBootApplication
public class EquiposApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquiposApplication.class, args);
    }
}

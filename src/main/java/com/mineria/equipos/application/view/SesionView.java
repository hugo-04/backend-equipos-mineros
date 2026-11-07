package com.mineria.equipos.application.view;

import lombok.Builder;
import lombok.Value;

/** Resultado de un login correcto: el token y cuántos segundos dura. */
@Value
@Builder
public class SesionView {

    String accessToken;
    String tokenType;
    long expiresIn;
}

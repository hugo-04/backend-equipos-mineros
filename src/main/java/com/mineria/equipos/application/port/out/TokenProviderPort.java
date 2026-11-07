package com.mineria.equipos.application.port.out;

import com.mineria.equipos.application.view.SesionView;
import com.mineria.equipos.domain.model.Usuario;

/** Puerto de salida para emitir el token de sesión de un usuario ya autenticado (JWT en la infraestructura). */
public interface TokenProviderPort {

    SesionView emitir(Usuario usuario);
}

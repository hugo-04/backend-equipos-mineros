package com.mineria.equipos.infrastructure.adapter.in.web.error;

import java.time.Clock;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.ErrorDetail;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.ErrorResponse;

import lombok.RequiredArgsConstructor;

/**
 * Fábrica del {@link ErrorResponse} del contrato. La usan tanto el GlobalExceptionHandler
 * como los handlers de seguridad (401/403), para que todos los errores tengan exactamente el mismo formato.
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseFactory {

    private final Clock clock;

    public ErrorResponse crear(HttpStatus status, String codigo, String mensaje, String path) {
        return crear(status, codigo, mensaje, path, List.of());
    }

    public ErrorResponse crear(HttpStatus status, String codigo, String mensaje, String path,
                               List<ErrorDetail> detalles) {
        ErrorResponse respuesta = new ErrorResponse(
                clock.instant(), status.value(), status.getReasonPhrase(), codigo, mensaje, path);
        respuesta.setDetails(detalles);
        return respuesta;
    }

    public ErrorDetail detalle(String campo, String motivo) {
        return new ErrorDetail(campo, motivo);
    }
}

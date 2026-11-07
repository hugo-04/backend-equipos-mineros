package com.mineria.equipos.shared.exception;

import java.time.Clock;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.mineria.equipos.generated.model.ErrorDetail;
import com.mineria.equipos.generated.model.ErrorResponse;

import com.mineria.equipos.shared.trace.TraceId;

import lombok.RequiredArgsConstructor;

/**
 * Arma el {@link ErrorResponse} del contrato. Lo usan el GlobalExceptionHandler y los handlers de
 * seguridad (401/403) para que todos los errores salgan con el mismo formato, y acá se le pega el traceId.
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
        respuesta.setTraceId(TraceId.actual());
        return respuesta;
    }

    public ErrorDetail detalle(String campo, String motivo) {
        return new ErrorDetail(campo, motivo);
    }
}

package com.mineria.equipos.shared.exception;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import com.mineria.equipos.shared.exception.CodigoDuplicadoException;
import com.mineria.equipos.shared.exception.CredencialesInvalidasException;
import com.mineria.equipos.shared.exception.DomainException;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.shared.exception.TipoEquipoNotFoundException;
import com.mineria.equipos.generated.model.ErrorDetail;
import com.mineria.equipos.generated.model.ErrorResponse;
import com.mineria.equipos.shared.trace.LogSeguro;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Convierte cualquier excepción en el {@link ErrorResponse} del contrato. Extiende
 * ResponseEntityExceptionHandler para que los errores del propio Spring MVC (ruta que no existe, método
 * no permitido, media type...) también salgan con este formato. Los mensajes son genéricos a propósito:
 * trazas y detalles internos van al log, nunca al cliente.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String VALIDACION_FALLIDA = "VALIDACION_FALLIDA";
    static final String JSON_INVALIDO = "JSON_INVALIDO";
    static final String PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    static final String PROPIEDAD_DESCONOCIDA = "PROPIEDAD_DESCONOCIDA";
    static final String CONFLICTO_DE_DATOS = "CONFLICTO_DE_DATOS";
    static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";
    static final String ERROR_INTERNO = "ERROR_INTERNO";

    private final ErrorResponseFactory errores;

    // Errores de negocio

    @ExceptionHandler({EquipoNotFoundException.class, TipoEquipoNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoEncontrado(DomainException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getCodigo(), ex.getMessage(), request);
    }

    @ExceptionHandler(CodigoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleCodigoDuplicado(CodigoDuplicadoException ex,
                                                              HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), request);
    }

    // Reglas de negocio que la petición rompe aunque esté bien formada: 422
    @ExceptionHandler({HorometroInvalidoException.class, HorometroNoPuedeBajarException.class,
            TipoNoModificableException.class})
    public ResponseEntity<ErrorResponse> handleReglaDeNegocio(DomainException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCodigo(), ex.getMessage(), request);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> handleCredencialesInvalidas(CredencialesInvalidasException ex,
                                                                    HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getCodigo(), ex.getMessage(), request);
    }

    // Validación

    /** El body no pasó las validaciones del contrato (@Valid sobre los DTOs generados). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<ErrorDetail> detalles = Stream.concat(
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> errores.detalle(e.getField(), e.getDefaultMessage())),
                ex.getBindingResult().getGlobalErrors().stream()
                        .map(e -> errores.detalle(e.getObjectName(), e.getDefaultMessage()))
        ).toList();
        return responderValidacion(detalles, request);
    }

    /** Un parámetro no pasó la validación (por ejemplo el id del path con @Min(1)) y lo detectó Spring MVC. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<ErrorDetail> detalles = ex.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(e -> errores.detalle(resultado.getMethodParameter().getParameterName(),
                                e.getDefaultMessage())))
                .toList();
        return responderValidacion(detalles, request);
    }

    /** Lo mismo que el de arriba, pero cuando quien lo detecta es el proxy de @Validated de las interfaces generadas. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                  HttpServletRequest request) {
        List<ErrorDetail> detalles = ex.getConstraintViolations().stream()
                .map(v -> errores.detalle(nombreDelParametro(v), v.getMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, VALIDACION_FALLIDA,
                "La petición tiene datos inválidos", request, detalles);
    }

    /** JSON roto, un tipo que no cuadra (texto donde va un número) o propiedades que el contrato no conoce. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        HttpServletRequest http = servletRequest(request);
        if (ex.getCause() instanceof UnrecognizedPropertyException desconocida) {
            ErrorDetail detalle = errores.detalle(desconocida.getPropertyName(), "propiedad no permitida");
            return cuerpo(responder(HttpStatus.BAD_REQUEST, PROPIEDAD_DESCONOCIDA,
                    "El cuerpo contiene propiedades que la API no reconoce", http, List.of(detalle)));
        }
        if (ex.getCause() instanceof DatabindException mapeo && !mapeo.getPath().isEmpty()) {
            String campo = mapeo.getPath().get(mapeo.getPath().size() - 1).getPropertyName();
            ErrorDetail detalle = errores.detalle(campo, "valor con tipo o formato incorrecto");
            return cuerpo(responder(HttpStatus.BAD_REQUEST, JSON_INVALIDO,
                    "El cuerpo de la petición tiene un valor inválido", http, List.of(detalle)));
        }
        return cuerpo(responder(HttpStatus.BAD_REQUEST, JSON_INVALIDO,
                "El cuerpo de la petición no es un JSON válido", http));
    }

    /** Un parámetro de la URL con el tipo equivocado, por ejemplo /equipos/abc. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        ErrorDetail detalle = errores.detalle(ex.getPropertyName(), "valor con tipo incorrecto");
        return cuerpo(responder(HttpStatus.BAD_REQUEST, PARAMETRO_INVALIDO,
                "Un parámetro de la petición tiene un tipo incorrecto", servletRequest(request), List.of(detalle)));
    }

    // Base de datos y autenticación

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                            HttpServletRequest request) {
        log.warn("Violación de integridad de datos en {}: {}", LogSeguro.limpiar(request.getRequestURI()),
                ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, CONFLICTO_DE_DATOS,
                "La operación entra en conflicto con los datos existentes", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
                                                             HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, NO_AUTENTICADO,
                "Se requiere autenticación para acceder a este recurso", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, ACCESO_DENEGADO,
                "No tienes permiso para realizar esta operación", request);
    }

    // Lo demás

    /** Los errores que Spring MVC resuelve solo (ruta inexistente, método no permitido...), con nuestro formato. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        return cuerpo(responder(status, codigoGenerico(status), mensajeGenerico(status), servletRequest(request)));
    }

    /** Red de seguridad para lo imprevisto. El detalle se queda en el log; al cliente solo le llega un mensaje genérico. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInesperado(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {} {}", request.getMethod(), LogSeguro.limpiar(request.getRequestURI()), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_INTERNO,
                "Ocurrió un error inesperado. Intenta de nuevo más tarde", request);
    }

    // Auxiliares

    private ResponseEntity<Object> responderValidacion(List<ErrorDetail> detalles, WebRequest request) {
        return cuerpo(responder(HttpStatus.BAD_REQUEST, VALIDACION_FALLIDA,
                "La petición tiene datos inválidos", servletRequest(request), detalles));
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String codigo, String mensaje,
                                                    HttpServletRequest request) {
        return responder(status, codigo, mensaje, request, List.of());
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String codigo, String mensaje,
                                                    HttpServletRequest request, List<ErrorDetail> detalles) {
        ErrorResponse cuerpo = errores.crear(status, codigo, mensaje, request.getRequestURI(), detalles);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(cuerpo);
    }

    /** Los métodos heredados de Spring piden ResponseEntity&lt;Object&gt;, así que acá se adapta el de ErrorResponse. */
    private ResponseEntity<Object> cuerpo(ResponseEntity<ErrorResponse> respuesta) {
        return ResponseEntity.status(respuesta.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(respuesta.getBody());
    }

    private HttpServletRequest servletRequest(WebRequest request) {
        return ((ServletWebRequest) request).getRequest();
    }

    /** Del path "obtenerEquipo.id" se queda solo con "id". */
    private String nombreDelParametro(ConstraintViolation<?> violacion) {
        String ruta = violacion.getPropertyPath().toString();
        return ruta.substring(ruta.lastIndexOf('.') + 1);
    }

    private String codigoGenerico(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "RUTA_NO_ENCONTRADA";
            case METHOD_NOT_ALLOWED -> "METODO_NO_PERMITIDO";
            case UNSUPPORTED_MEDIA_TYPE -> "MEDIA_TYPE_NO_SOPORTADO";
            case NOT_ACCEPTABLE -> "FORMATO_NO_DISPONIBLE";
            default -> status.is4xxClientError() ? "PETICION_INVALIDA" : ERROR_INTERNO;
        };
    }

    private String mensajeGenerico(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "El recurso solicitado no existe";
            case METHOD_NOT_ALLOWED -> "Método HTTP no permitido para este recurso";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de contenido no soportado";
            case NOT_ACCEPTABLE -> "Formato de respuesta no disponible";
            default -> status.is4xxClientError() ? "La petición no se pudo procesar" : "Ocurrió un error inesperado";
        };
    }
}

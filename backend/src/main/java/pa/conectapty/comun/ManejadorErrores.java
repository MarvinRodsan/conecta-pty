package pa.conectapty.comun;

import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Errores con mensajes claros en español (RFC 9457 Problem Details).
 * Nunca se devuelve el texto interno de una excepción: puede revelar clases, rutas o versiones.
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ServicioNoDisponibleException.class)
    ProblemDetail noDisponible(ServicioNoDisponibleException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        p.setTitle("Servicio temporalmente no disponible");
        return p;
    }

    @ExceptionHandler(PeticionInvalidaException.class)
    ProblemDetail invalida(PeticionInvalidaException e) {
        return malo(e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail falta(MissingServletRequestParameterException e) {
        return malo("Falta el parámetro '" + e.getParameterName() + "'.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail tipo(MethodArgumentTypeMismatchException e) {
        return malo("El parámetro '" + e.getName() + "' tiene un formato inválido.");
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail validacion(HandlerMethodValidationException e) {
        return malo("Algún parámetro está fuera de rango (las coordenadas deben estar dentro de Panamá).");
    }

    /** Cuerpo JSON con campos inválidos: se dice cuál, pero no el valor recibido. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail cuerpoInvalido(MethodArgumentNotValidException e) {
        String campos = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField).distinct().sorted().collect(Collectors.joining(", "));
        return malo("Revisa estos campos: " + campos + ".");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail ilegible(HttpMessageNotReadableException e) {
        return malo("El cuerpo de la petición no es un JSON válido o tiene un valor no permitido.");
    }

    private static ProblemDetail malo(String detalle) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
        p.setTitle("Petición inválida");
        return p;
    }
}

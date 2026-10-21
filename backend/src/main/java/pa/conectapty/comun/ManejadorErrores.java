package pa.conectapty.comun;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Errores con mensajes claros en español (RFC 9457 Problem Details). */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ServicioNoDisponibleException.class)
    ProblemDetail noDisponible(ServicioNoDisponibleException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        p.setTitle("Servicio temporalmente no disponible");
        return p;
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class})
    ProblemDetail peticionInvalida(Exception e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Revisa los parámetros: " + e.getMessage());
        p.setTitle("Petición inválida");
        return p;
    }
}

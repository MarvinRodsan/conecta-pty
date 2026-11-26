package pa.conectapty.comun;

/** Error de datos de entrada con un mensaje seguro de mostrar al usuario. */
public class PeticionInvalidaException extends RuntimeException {
    public PeticionInvalidaException(String mensaje) {
        super(mensaje);
    }
}

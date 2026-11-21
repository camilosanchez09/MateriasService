package co.edu.uptc.materias.exception;

/** 409: la operación choca con el estado actual (ej. no quedan cupos). */
public class ConflictException extends RuntimeException {
    private final String codigo;

    public ConflictException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}

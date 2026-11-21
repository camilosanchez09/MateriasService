package co.edu.uptc.materias.exception;

/** 422: la solicitud es válida pero viola una regla de negocio (ej. curso INACTIVO). */
public class UnprocessableException extends RuntimeException {
    private final String codigo;

    public UnprocessableException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}

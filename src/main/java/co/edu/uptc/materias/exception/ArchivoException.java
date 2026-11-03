package co.edu.uptc.materias.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

/** Errores de los PDFs con su código HTTP: 400 sin archivo, 413 muy grande, 415 no es PDF, 503 S3 caído. */
@Getter
public class ArchivoException extends RuntimeException {

    private final HttpStatus status;

    private ArchivoException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ArchivoException requerido() {
        return new ArchivoException(HttpStatus.BAD_REQUEST, "Debe enviar el PDF en el campo 'file' (multipart/form-data)");
    }

    public static ArchivoException muyGrande() {
        return new ArchivoException(HttpStatus.PAYLOAD_TOO_LARGE, "El PDF supera el tamaño máximo de 10 MB");
    }

    public static ArchivoException noEsPdf() {
        return new ArchivoException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Solo se aceptan archivos PDF");
    }

    public static ArchivoException almacenamientoNoDisponible() {
        return new ArchivoException(HttpStatus.SERVICE_UNAVAILABLE, "No fue posible acceder al almacenamiento de archivos");
    }
}

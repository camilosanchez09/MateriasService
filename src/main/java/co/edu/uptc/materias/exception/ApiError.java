package co.edu.uptc.materias.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> detalles
) {
    public ApiError(int status, String error, String message) {
        this(LocalDateTime.now(), status, error, message, List.of());
    }

    public ApiError(int status, String error, String message, List<String> detalles) {
        this(LocalDateTime.now(), status, error, message, detalles);
    }
}

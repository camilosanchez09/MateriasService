package co.edu.uptc.materias.archivo.dto;

/** URL firmada y temporal para descargar el PDF; expiresIn en segundos. */
public record UrlDescargaResponse(String url, long expiresIn) {
}

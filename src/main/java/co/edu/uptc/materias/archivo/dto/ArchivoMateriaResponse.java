package co.edu.uptc.materias.archivo.dto;

import co.edu.uptc.materias.archivo.ArchivoMateria;
import java.time.Instant;

public record ArchivoMateriaResponse(
        Long id,
        Long materiaId,
        String nombreOriginal,
        String contentType,
        Long tamanoBytes,
        Instant subidoEn) {

    public static ArchivoMateriaResponse of(ArchivoMateria a, Long materiaId) {
        return new ArchivoMateriaResponse(a.getId(), materiaId, a.getNombreOriginal(), a.getContentType(),
                a.getTamanoBytes(), a.getSubidoEn());
    }
}

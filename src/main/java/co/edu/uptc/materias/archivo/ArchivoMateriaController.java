package co.edu.uptc.materias.archivo;

import co.edu.uptc.materias.archivo.dto.ArchivoMateriaResponse;
import co.edu.uptc.materias.archivo.dto.UrlDescargaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/materias/{materiaId}/archivos")
@RequiredArgsConstructor
@Tag(name = "Archivos de materia",
        description = "PDFs con el contenido temático. El archivo vive en un bucket S3 privado; la base guarda solo metadatos.")
public class ArchivoMateriaController {

    private final ArchivoMateriaService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir un PDF (campo 'file', máximo 10 MB)",
            description = "Se verifica que el contenido sea realmente PDF. 400 sin archivo, 404 materia inexistente, "
                    + "413 mayor a 10 MB, 415 no es PDF, 503 almacenamiento no disponible.")
    public ResponseEntity<ArchivoMateriaResponse> subir(@PathVariable Long materiaId,
                                                        @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.subir(materiaId, file));
    }

    @GetMapping
    @Operation(summary = "Listar los PDFs de una materia (más reciente primero)")
    public List<ArchivoMateriaResponse> listar(@PathVariable Long materiaId) {
        return service.listar(materiaId);
    }

    @GetMapping("/{archivoId}/url")
    @Operation(summary = "Obtener una URL temporal firmada para descargar el PDF",
            description = "El bucket es privado: sin firma, S3 responde 403. La URL vence en expiresIn segundos.")
    public UrlDescargaResponse urlDescarga(@PathVariable Long materiaId, @PathVariable Long archivoId) {
        return service.urlDescarga(materiaId, archivoId);
    }

    @DeleteMapping("/{archivoId}")
    @Operation(summary = "Eliminar un PDF (metadatos y objeto en el bucket)")
    public ResponseEntity<Void> eliminar(@PathVariable Long materiaId, @PathVariable Long archivoId) {
        service.eliminar(materiaId, archivoId);
        return ResponseEntity.noContent().build();
    }
}

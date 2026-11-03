package co.edu.uptc.materias.archivo;

import co.edu.uptc.materias.archivo.dto.ArchivoMateriaResponse;
import co.edu.uptc.materias.archivo.dto.UrlDescargaResponse;
import co.edu.uptc.materias.exception.ArchivoException;
import co.edu.uptc.materias.exception.ResourceNotFoundException;
import co.edu.uptc.materias.materia.Materia;
import co.edu.uptc.materias.materia.MateriaRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchivoMateriaService {

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final ArchivoMateriaRepository repository;
    private final MateriaRepository materiaRepository;
    private final PdfStorage storage;

    /**
     * 1) validar  2) subir el objeto con clave única  3) guardar metadatos.
     * Si el paso 3 falla, se borra el objeto del paso 2: no quedan PDFs huérfanos en el bucket.
     */
    @Transactional
    public ArchivoMateriaResponse subir(Long materiaId, MultipartFile file) {
        Materia materia = materiaRepository.findById(materiaId)
                .orElseThrow(() -> ResourceNotFoundException.of("Materia", materiaId));
        byte[] contenido = leerPdfValido(file);

        String key = "materias/" + materiaId + "/" + UUID.randomUUID() + ".pdf";
        try {
            storage.put(key, contenido);
        } catch (SdkException e) {
            log.error("No se pudo subir el PDF {} a S3: {}", key, e.getMessage());
            throw ArchivoException.almacenamientoNoDisponible();
        }

        try {
            ArchivoMateria guardado = repository.saveAndFlush(ArchivoMateria.builder()
                    .materia(materia)
                    .objectKey(key)
                    .nombreOriginal(nombreOriginal(file))
                    .contentType(ArchivoMateria.PDF)
                    .tamanoBytes((long) contenido.length)
                    .subidoEn(Instant.now())
                    .build());
            return ArchivoMateriaResponse.of(guardado, materiaId);
        } catch (RuntimeException e) {
            borrarEnSilencio(key);
            throw e;
        }
    }

    /** Más reciente primero: el frontend descarga el primero de la lista. */
    @Transactional(readOnly = true)
    public List<ArchivoMateriaResponse> listar(Long materiaId) {
        if (!materiaRepository.existsById(materiaId)) {
            throw ResourceNotFoundException.of("Materia", materiaId);
        }
        return repository.findByMateriaIdOrderBySubidoEnDescIdDesc(materiaId).stream()
                .map(a -> ArchivoMateriaResponse.of(a, materiaId))
                .toList();
    }

    @Transactional(readOnly = true)
    public UrlDescargaResponse urlDescarga(Long materiaId, Long archivoId) {
        ArchivoMateria archivo = buscar(materiaId, archivoId);
        return new UrlDescargaResponse(
                storage.presignedDownloadUrl(archivo.getObjectKey(), archivo.getNombreOriginal()),
                storage.urlTtlSeconds());
    }

    @Transactional
    public void eliminar(Long materiaId, Long archivoId) {
        ArchivoMateria archivo = buscar(materiaId, archivoId);
        repository.delete(archivo);
        borrarDespuesDelCommit(List.of(archivo.getObjectKey()));
    }

    /** Lo usa MateriaService al eliminar una materia: filas ahora, objetos solo si la transacción confirma. */
    @Transactional
    public void eliminarTodosDeMateria(Long materiaId) {
        List<String> keys = repository.findObjectKeysByMateriaId(materiaId);
        if (keys.isEmpty()) {
            return;
        }
        repository.deleteByMateriaId(materiaId);
        borrarDespuesDelCommit(keys);
    }

    private ArchivoMateria buscar(Long materiaId, Long archivoId) {
        return repository.findByIdAndMateriaId(archivoId, materiaId)
                .orElseThrow(() -> ResourceNotFoundException.of("Archivo de la materia " + materiaId, archivoId));
    }

    static byte[] leerPdfValido(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ArchivoException.requerido();
        }
        if (file.getSize() > ArchivoMateria.MAX_BYTES) {
            throw ArchivoException.muyGrande();
        }
        byte[] contenido;
        try {
            contenido = file.getBytes();
        } catch (IOException e) {
            throw ArchivoException.requerido();
        }
        // Se decide por los bytes, no por la extensión ni el Content-Type que declara el cliente.
        if (contenido.length < PDF_MAGIC.length
                || !Arrays.equals(Arrays.copyOf(contenido, PDF_MAGIC.length), PDF_MAGIC)) {
            throw ArchivoException.noEsPdf();
        }
        return contenido;
    }

    private static String nombreOriginal(MultipartFile file) {
        String nombre = file.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return "contenido.pdf";
        }
        nombre = nombre.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        return nombre.length() > 255 ? nombre.substring(0, 255) : nombre;
    }

    private void borrarDespuesDelCommit(List<String> keys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            keys.forEach(this::borrarEnSilencio);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keys.forEach(ArchivoMateriaService.this::borrarEnSilencio);
            }
        });
    }

    /** Borrar un objeto es limpieza: si falla se registra, pero no se rompe la operación del usuario. */
    private void borrarEnSilencio(String key) {
        try {
            storage.delete(key);
        } catch (SdkException e) {
            log.warn("No se pudo borrar el objeto {} en S3: {}", key, e.getMessage());
        }
    }
}

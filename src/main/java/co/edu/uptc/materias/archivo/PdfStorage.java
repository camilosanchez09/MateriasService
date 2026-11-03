package co.edu.uptc.materias.archivo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/** Único punto del módulo que habla con S3. */
@Component
@RequiredArgsConstructor
public class PdfStorage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties props;

    public void put(String key, byte[] content) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(key)
                        .contentType(ArchivoMateria.PDF)
                        .build(),
                RequestBody.fromBytes(content));
    }

    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(props.bucket()).key(key).build());
    }

    /** URL temporal de solo lectura: el bucket es privado, sin firma el objeto no es accesible. */
    public String presignedDownloadUrl(String key, String originalName) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(props.bucket())
                .key(key)
                .responseContentType(ArchivoMateria.PDF)
                // Cabeceras HTTP solo aceptan ASCII: el nombre original completo queda en la base.
                .responseContentDisposition("inline; filename=\"" + originalName.replaceAll("[^A-Za-z0-9._ -]", "_") + "\"")
                .build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(props.urlTtl())
                        .getObjectRequest(get)
                        .build())
                .url()
                .toString();
    }

    public long urlTtlSeconds() {
        return props.urlTtl().toSeconds();
    }
}

package co.edu.uptc.materias.archivo;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Almacenamiento de objetos (S3) de los PDFs. endpoint y publicEndpoint solo se usan en local
 * (RustFS); en AWS quedan vacíos y el SDK toma región y credenciales del entorno (rol de la tarea).
 */
@ConfigurationProperties(prefix = "storage.s3")
public record StorageProperties(
        String bucket,
        String region,
        String endpoint,
        String publicEndpoint,
        boolean pathStyle,
        Duration urlTtl) {
}

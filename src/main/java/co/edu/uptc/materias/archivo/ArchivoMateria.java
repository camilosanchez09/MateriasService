package co.edu.uptc.materias.archivo;

import co.edu.uptc.materias.materia.Materia;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadatos de un PDF de contenido temático. El archivo vive en el bucket S3 (clave objectKey);
 * aquí nunca se guarda el binario ni la URL, que se firma en cada descarga.
 */
@Entity
@Table(name = "materia_archivos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivoMateria {

    public static final String PDF = "application/pdf";
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(name = "nombre_original", nullable = false)
    private String nombreOriginal;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "subido_en", nullable = false)
    private Instant subidoEn;
}

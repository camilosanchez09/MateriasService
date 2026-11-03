package co.edu.uptc.materias.archivo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uptc.materias.exception.ArchivoException;
import co.edu.uptc.materias.exception.ResourceNotFoundException;
import co.edu.uptc.materias.materia.Materia;
import co.edu.uptc.materias.materia.MateriaRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;

@ExtendWith(MockitoExtension.class)
class ArchivoMateriaServiceTest {

    private static final byte[] PDF = "%PDF-1.7\n...contenido...".getBytes(StandardCharsets.US_ASCII);

    @Mock
    private ArchivoMateriaRepository repository;
    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private PdfStorage storage;
    @InjectMocks
    private ArchivoMateriaService service;

    private final Materia materia = Materia.builder().id(7L).nombre("Sistemas Distribuidos").build();

    @BeforeEach
    void materiaExiste() {
        lenient().when(materiaRepository.findById(7L)).thenReturn(Optional.of(materia));
    }

    private static MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("file", nombre, "application/pdf", contenido);
    }

    @Test
    void subeElPdfAlBucketYGuardaSoloMetadatos() {
        when(repository.saveAndFlush(any())).thenAnswer(inv -> {
            ArchivoMateria a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        var respuesta = service.subir(7L, archivo("unidad1.pdf", PDF));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).put(key.capture(), any());
        assertThat(key.getValue()).matches("materias/7/[0-9a-f-]{36}\\.pdf");
        assertThat(respuesta.nombreOriginal()).isEqualTo("unidad1.pdf");
        assertThat(respuesta.tamanoBytes()).isEqualTo(PDF.length);
        assertThat(respuesta.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void rechazaConUnsupportedMediaTypeUnArchivoQueNoEsPdfAunqueSeLlamePdf() {
        var falso = archivo("trampa.pdf", "\u0089PNG esto es una imagen".getBytes(StandardCharsets.ISO_8859_1));

        assertThatThrownBy(() -> service.subir(7L, falso))
                .isInstanceOfSatisfying(ArchivoException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
        verify(storage, never()).put(anyString(), any());
    }

    @Test
    void rechazaArchivoVacio() {
        assertThatThrownBy(() -> service.subir(7L, archivo("vacio.pdf", new byte[0])))
                .isInstanceOfSatisfying(ArchivoException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void siGuardarMetadatosFallaBorraElObjetoRecienSubido() {
        when(repository.saveAndFlush(any())).thenThrow(new IllegalStateException("BD caída"));

        assertThatThrownBy(() -> service.subir(7L, archivo("unidad1.pdf", PDF))).hasMessage("BD caída");

        ArgumentCaptor<String> subida = ArgumentCaptor.forClass(String.class);
        verify(storage).put(subida.capture(), any());
        verify(storage).delete(subida.getValue());
    }

    @Test
    void siS3NoEstaDisponibleResponde503YNoTocaLaBase() {
        doThrow(SdkClientException.create("Connection refused")).when(storage).put(anyString(), any());

        assertThatThrownBy(() -> service.subir(7L, archivo("unidad1.pdf", PDF)))
                .isInstanceOfSatisfying(ArchivoException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void materiaInexistenteDa404SinSubirNada() {
        when(materiaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subir(99L, archivo("unidad1.pdf", PDF)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(storage, never()).put(anyString(), any());
    }
}

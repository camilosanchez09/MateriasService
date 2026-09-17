package co.edu.uptc.materias.seed;

import co.edu.uptc.materias.curso.Curso;
import co.edu.uptc.materias.curso.CursoRepository;
import co.edu.uptc.materias.docente.Docente;
import co.edu.uptc.materias.docente.DocenteRepository;
import co.edu.uptc.materias.materia.Materia;
import co.edu.uptc.materias.materia.MateriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.text.Normalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Puebla la base de datos propia de este módulo al arrancar la aplicación:
 * - 20 materias ficticias.
 * - 15 docentes ficticios.
 * - 30 cursos ficticios (oferta real, combinando materias y docentes).
 * Solo corre si las tablas están vacías, para no duplicar datos en cada
 * reinicio.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final int TOTAL_MATERIAS = 20;
    private static final int TOTAL_DOCENTES = 15;
    private static final int TOTAL_CURSOS = 30;

    private static final String[] PROGRAMAS = {
            "Ingeniería de Sistemas", "Ingeniería Electrónica", "Ingeniería Industrial", "Licenciatura en Matemáticas"
    };
    private static final String[] PERIODOS = { "2026-1", "2026-2" };
    private static final String[] DIAS = { "Lunes-Miércoles", "Martes-Jueves", "Lunes-Viernes", "Miércoles-Viernes" };
    private static final String[] HORAS = { "6:00-8:00", "8:00-10:00", "10:00-12:00", "14:00-16:00", "16:00-18:00" };

    private final MateriaRepository materiaRepository;
    private final DocenteRepository docenteRepository;
    private final CursoRepository cursoRepository;

    @SuppressWarnings("deprecation")
    private final Faker faker = new Faker(new Locale("es"));
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        List<Materia> materias = seedMaterias();
        List<Docente> docentes = seedDocentes();
        seedCursos(materias, docentes);
    }

    private List<Materia> seedMaterias() {
        if (materiaRepository.count() > 0) {
            log.info("Materias ya tiene datos, se omite el seeding.");
            return materiaRepository.findAll();
        }

        List<Materia> materias = new ArrayList<>();
        for (int i = 1; i <= TOTAL_MATERIAS; i++) {
            Materia materia = Materia.builder()
                    .nombre(faker.educator().course())
                    .codigo("MAT-" + String.format("%03d", i))
                    .creditos(1 + random.nextInt(4))
                    .programa(PROGRAMAS[random.nextInt(PROGRAMAS.length)])
                    .build();
            materias.add(materia);
        }
        materias = materiaRepository.saveAll(materias);
        log.info("Se sembraron {} materias.", materias.size());
        return materias;
    }

    private List<Docente> seedDocentes() {
        if (docenteRepository.count() > 0) {
            log.info("Docentes ya tiene datos, se omite el seeding.");
            return docenteRepository.findAll();
        }

        List<Docente> docentes = new ArrayList<>();
        for (int i = 1; i <= TOTAL_DOCENTES; i++) {
            String nombre = faker.name().firstName();
            String apellido = faker.name().lastName();
            String correo = normalizarParaCorreo(nombre) + "." + normalizarParaCorreo(apellido) + i + "@uptc.edu.co";
            Docente docente = Docente.builder()
                    .nombre(nombre)
                    .apellido(apellido)
                    .correo(correo)
                    .especialidad(faker.educator().course())
                    .build();
            docentes.add(docente);
        }
        docentes = docenteRepository.saveAll(docentes);
        log.info("Se sembraron {} docentes.", docentes.size());
        return docentes;
    }

    private void seedCursos(List<Materia> materias, List<Docente> docentes) {
        if (cursoRepository.count() > 0) {
            log.info("Cursos ya tiene datos, se omite el seeding.");
            return;
        }
        if (materias.isEmpty() || docentes.isEmpty()) {
            log.warn("No hay materias o docentes para generar cursos.");
            return;
        }

        List<Curso> cursos = new ArrayList<>();
        for (int i = 0; i < TOTAL_CURSOS; i++) {
            Curso curso = Curso.builder()
                    .materia(materias.get(random.nextInt(materias.size())))
                    .docente(docentes.get(random.nextInt(docentes.size())))
                    .horario(DIAS[random.nextInt(DIAS.length)] + " " + HORAS[random.nextInt(HORAS.length)])
                    .periodo(PERIODOS[random.nextInt(PERIODOS.length)])
                    .cupo(20 + random.nextInt(20))
                    .build();
            cursos.add(curso);
        }
        cursoRepository.saveAll(cursos);
        log.info("Se sembraron {} cursos.", cursos.size());
    }

    private String normalizarParaCorreo(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}

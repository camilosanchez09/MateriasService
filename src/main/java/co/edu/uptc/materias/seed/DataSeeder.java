package co.edu.uptc.materias.seed;

import co.edu.uptc.materias.curso.Curso;
import co.edu.uptc.materias.curso.CursoRepository;
import co.edu.uptc.materias.docente.Docente;
import co.edu.uptc.materias.docente.DocenteRepository;
import co.edu.uptc.materias.materia.Materia;
import co.edu.uptc.materias.materia.MateriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos de demostración del módulo Materias.
 *
 * <p>Genera 7 pensums (ISIS, IIND, ICIV, ADEM, CPUB, DERE y TGRE) con 40
 * asignaturas cada uno. Las asignaturas están ordenadas de fácil a difícil:
 * 4 por semestre, del semestre 1 al 10. Para cada asignatura se oferta un
 * curso en cada uno de los 8 periodos académicos, de modo que un estudiante
 * pueda tener historial y matrícula vigente.</p>
 *
 * <p>La estructura es determinista: los ids de las materias y de los cursos
 * se generan en un orden conocido. El módulo Inscripciones usa ese mismo
 * orden para sembrar únicamente materias del programa de cada estudiante.</p>
 *
 * <p>Si el dataset cambia de versión, se borra y se reconstruye. El marcador
 * vive en la tabla {@code seed_control} para no resetear los datos en cada
 * arranque.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final int SEED_VERSION = 2;
    private static final String SEED_NAME = "materias";
    private static final int MATERIAS_POR_PROGRAMA = 40;
    private static final int MATERIAS_POR_SEMESTRE = 4;

    private static final String[] PERIODOS = {
            "2023-1", "2023-2", "2024-1", "2024-2",
            "2025-1", "2025-2", "2026-1", "2026-2"
    };
    private static final String[] DIAS = {
            "Lunes-Miércoles", "Martes-Jueves", "Lunes-Viernes",
            "Miércoles-Viernes", "Martes-Viernes"
    };
    private static final String[] HORAS = {
            "6:00-8:00", "8:00-10:00", "10:00-12:00",
            "14:00-16:00", "16:00-18:00", "18:00-20:00"
    };

    private final MateriaRepository materiaRepository;
    private final DocenteRepository docenteRepository;
    private final CursoRepository cursoRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS seed_control (
                    name    VARCHAR(50) PRIMARY KEY,
                    version INTEGER     NOT NULL
                )
                """);

        if (!necesitaReseed()) {
            log.info("Dataset de materias al día (versión {}); se omite el seeding.", SEED_VERSION);
            return;
        }

        log.info("Dataset de materias desactualizado: se reconstruye (versión {}).", SEED_VERSION);
        jdbcTemplate.execute("TRUNCATE TABLE cursos, materias, docentes RESTART IDENTITY CASCADE");

        List<Docente> docentes = seedDocentes();
        List<Materia> materias = seedMaterias();
        seedCursos(materias, docentes);

        jdbcTemplate.update("""
                INSERT INTO seed_control (name, version) VALUES (?, ?)
                ON CONFLICT (name) DO UPDATE SET version = EXCLUDED.version
                """, SEED_NAME, SEED_VERSION);
    }

    private boolean necesitaReseed() {
        List<Integer> versiones = jdbcTemplate.query(
                "SELECT version FROM seed_control WHERE name = ?",
                (rs, rowNum) -> rs.getInt(1), SEED_NAME);
        return versiones.isEmpty() || versiones.get(0) < SEED_VERSION;
    }

    private List<Docente> seedDocentes() {
        String[][] datos = {
                { "Andrés", "Vargas", "Ingeniería de Software" },
                { "Claudia", "Rincón", "Bases de Datos" },
                { "Jorge", "Moreno", "Redes de Computadores" },
                { "Patricia", "Salazar", "Matemáticas" },
                { "Ricardo", "Peña", "Física" },
                { "Liliana", "Castro", "Inteligencia Artificial" },
                { "Fernando", "Ortiz", "Sistemas Operativos" },
                { "Mónica", "Gutiérrez", "Arquitectura de Computadores" },
                { "Óscar", "Herrera", "Ingeniería de Software" },
                { "Sandra", "Mendoza", "Estadística" },
                { "Héctor", "Rojas", "Redes y Telecomunicaciones" },
                { "Carolina", "Navarro", "Gestión de Proyectos" },
                { "Álvaro", "Suárez", "Derecho Civil" },
                { "Beatriz", "Cárdenas", "Derecho Penal" },
                { "Gustavo", "Pardo", "Contabilidad" },
                { "Natalia", "Quintero", "Finanzas" },
                { "Eduardo", "Ramírez", "Ingeniería Civil" },
                { "Diana", "Torres", "Ingeniería Industrial" },
                { "Mauricio", "León", "Administración" },
                { "Tatiana", "Bermúdez", "Ciberseguridad" }
        };

        List<Docente> docentes = new ArrayList<>();
        for (int i = 0; i < datos.length; i++) {
            String nombre = datos[i][0];
            String apellido = datos[i][1];
            String correo = normalizarParaCorreo(nombre) + "." + normalizarParaCorreo(apellido)
                    + (i + 1) + "@uptc.edu.co";
            docentes.add(Docente.builder()
                    .nombre(nombre)
                    .apellido(apellido)
                    .correo(correo)
                    .especialidad(datos[i][2])
                    .build());
        }
        docentes = docenteRepository.saveAll(docentes);
        log.info("Se sembraron {} docentes.", docentes.size());
        return docentes;
    }

    private List<Materia> seedMaterias() {
        List<Materia> materias = new ArrayList<>();
        for (Programa programa : PROGRAMAS) {
            for (int i = 0; i < programa.materias().length; i++) {
                int semestre = (i / MATERIAS_POR_SEMESTRE) + 1;
                int creditos = creditosPorSemestre(semestre, i);
                materias.add(Materia.builder()
                        .nombre(programa.materias()[i])
                        .codigo(programa.codigo() + "-" + String.format("%03d", i + 1))
                        .creditos(creditos)
                        .programa(programa.nombre())
                        .programaCodigo(programa.codigo())
                        .semestre(semestre)
                        .build());
            }
        }
        materias = materiaRepository.saveAll(materias);
        log.info("Se sembraron {} materias ({} pensums de {} asignaturas).",
                materias.size(), PROGRAMAS.size(), MATERIAS_POR_PROGRAMA);
        return materias;
    }

    /**
     * Un curso por materia y periodo. El orden de inserción es periodo-mayor,
     * materia-menor: así el id del curso es {@code periodo * 280 + materia + 1}.
     * Inscripciones depende de ese orden para sembrar datos coherentes.
     */
    private void seedCursos(List<Materia> materias, List<Docente> docentes) {
        List<Curso> cursos = new ArrayList<>();
        for (int p = 0; p < PERIODOS.length; p++) {
            for (int m = 0; m < materias.size(); m++) {
                Materia materia = materias.get(m);
                Docente docente = docentes.get((m * 7 + p * 3) % docentes.size());
                String horario = DIAS[(m + p) % DIAS.length] + " " + HORAS[(m * 2 + p) % HORAS.length];
                int cupo = 20 + ((m * 13 + p * 5) % 21);

                cursos.add(Curso.builder()
                        .materia(materia)
                        .docente(docente)
                        .horario(horario)
                        .periodo(PERIODOS[p])
                        .cupo(cupo)
                        .build());
            }
        }
        cursoRepository.saveAll(cursos);
        log.info("Se sembraron {} cursos en {} periodos.", cursos.size(), PERIODOS.length);
    }

    private int creditosPorSemestre(int semestre, int indiceEnPensum) {
        if (semestre == 10 && indiceEnPensum % MATERIAS_POR_SEMESTRE == 0) {
            return 5; // trabajo de grado
        }
        if (indiceEnPensum % MATERIAS_POR_SEMESTRE == 3) {
            return 2; // electiva / asignatura de menor intensidad
        }
        return semestre <= 2 ? 3 : 4;
    }

    private String normalizarParaCorreo(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    private record Programa(String codigo, String nombre, String[] materias) {
    }

    private static final List<Programa> PROGRAMAS = List.of(
            new Programa("ISIS", "Ingeniería de Sistemas", new String[] {
                    "Introducción a la Ingeniería de Sistemas", "Cálculo Diferencial", "Álgebra Lineal", "Fundamentos de Programación",
                    "Cálculo Integral", "Física Mecánica", "Programación Orientada a Objetos", "Matemáticas Discretas",
                    "Cálculo Multivariado", "Física Electromagnética", "Estructuras de Datos", "Bases de Datos I",
                    "Ecuaciones Diferenciales", "Probabilidad y Estadística", "Análisis de Algoritmos", "Bases de Datos II",
                    "Arquitectura de Computadores", "Sistemas Operativos", "Redes de Computadores", "Ingeniería de Software I",
                    "Compiladores", "Sistemas Distribuidos", "Ingeniería de Software II", "Inteligencia Artificial",
                    "Seguridad Informática", "Gestión de Proyectos de Software", "Computación Gráfica", "Aprendizaje Automático",
                    "Big Data", "Computación en la Nube", "Auditoría de Sistemas", "Desarrollo Móvil",
                    "Arquitectura Empresarial", "Blockchain", "DevOps y Automatización", "Ética Profesional",
                    "Trabajo de Grado I", "Trabajo de Grado II", "Práctica Profesional", "Electiva Profesional"
            }),
            new Programa("IIND", "Ingeniería Industrial", new String[] {
                    "Introducción a la Ingeniería Industrial", "Cálculo Diferencial", "Álgebra Lineal", "Química General",
                    "Cálculo Integral", "Física Mecánica", "Probabilidad y Estadística", "Dibujo Técnico",
                    "Cálculo Multivariado", "Física Electromagnética", "Estadística Inferencial", "Procesos de Manufactura",
                    "Ecuaciones Diferenciales", "Investigación de Operaciones I", "Termodinámica", "Costos Industriales",
                    "Investigación de Operaciones II", "Ingeniería de Métodos", "Gestión de la Calidad", "Seguridad y Salud en el Trabajo",
                    "Diseño de Plantas", "Logística y Cadena de Suministro", "Gestión de Producción", "Simulación de Sistemas",
                    "Lean Manufacturing", "Gestión de Proyectos", "Mercadeo Industrial", "Ergonomía",
                    "Automatización Industrial", "Analítica de Datos", "Gestión Ambiental", "Finanzas Corporativas",
                    "Innovación y Emprendimiento", "Auditoría de Procesos", "Gestión del Talento Humano", "Ética Profesional",
                    "Trabajo de Grado I", "Trabajo de Grado II", "Práctica Profesional", "Electiva Profesional"
            }),
            new Programa("ICIV", "Ingeniería Civil", new String[] {
                    "Introducción a la Ingeniería Civil", "Cálculo Diferencial", "Álgebra Lineal", "Química General",
                    "Cálculo Integral", "Física Mecánica", "Geometría Descriptiva", "Topografía",
                    "Cálculo Multivariado", "Estática", "Materiales de Construcción", "Probabilidad y Estadística",
                    "Ecuaciones Diferenciales", "Dinámica", "Mecánica de Materiales", "Hidráulica",
                    "Análisis Estructural I", "Mecánica de Suelos", "Concreto Reforzado I", "Vías y Transporte",
                    "Análisis Estructural II", "Cimentaciones", "Concreto Reforzado II", "Acueductos y Alcantarillados",
                    "Diseño de Pavimentos", "Gestión de Obras", "Estructuras Metálicas", "Ingeniería Ambiental",
                    "Puentes", "Gerencia de Proyectos", "Sismorresistencia", "Costos y Presupuestos",
                    "Construcción Sostenible", "Interventoría de Obras", "Legislación de la Construcción", "Ética Profesional",
                    "Trabajo de Grado I", "Trabajo de Grado II", "Práctica Profesional", "Electiva Profesional"
            }),
            new Programa("ADEM", "Administración de Empresas", new String[] {
                    "Introducción a la Administración", "Contabilidad General", "Matemática Financiera", "Fundamentos de Economía",
                    "Microeconomía", "Contabilidad de Costos", "Estadística Descriptiva", "Teoría Organizacional",
                    "Macroeconomía", "Estadística Inferencial", "Derecho Comercial", "Comportamiento Organizacional",
                    "Administración Financiera I", "Mercadeo I", "Gestión de Operaciones", "Sistemas de Información Gerencial",
                    "Administración Financiera II", "Mercadeo II", "Gestión del Talento Humano", "Investigación de Mercados",
                    "Presupuestos", "Negocios Internacionales", "Emprendimiento", "Gestión de la Calidad",
                    "Planeación Estratégica", "Finanzas Internacionales", "Comercio Electrónico", "Responsabilidad Social",
                    "Dirección de Empresas", "Auditoría Administrativa", "Innovación Empresarial", "Analítica de Negocios",
                    "Gobierno Corporativo", "Ética Profesional", "Gestión del Cambio", "Electiva Profesional",
                    "Trabajo de Grado I", "Trabajo de Grado II", "Práctica Profesional", "Consultoría Empresarial"
            }),
            new Programa("CPUB", "Contaduría Pública", new String[] {
                    "Introducción a la Contaduría", "Contabilidad I", "Matemática Financiera", "Fundamentos de Economía",
                    "Contabilidad II", "Microeconomía", "Estadística Descriptiva", "Derecho Comercial",
                    "Contabilidad de Costos", "Macroeconomía", "Estadística Inferencial", "Legislación Tributaria I",
                    "Contabilidad Financiera I", "Auditoría I", "Legislación Tributaria II", "Sistemas de Información Contable",
                    "Contabilidad Financiera II", "Auditoría II", "Tributación de Personas Naturales", "Finanzas Corporativas",
                    "Contabilidad Pública", "Revisoría Fiscal", "Tributación de Personas Jurídicas", "Presupuestos",
                    "Normas Internacionales de Información Financiera", "Auditoría de Sistemas", "Control Interno", "Costos Gerenciales",
                    "Consolidación Financiera", "Análisis Financiero", "Derecho Laboral", "Gestión de Riesgos",
                    "Ética Profesional", "Tributación Internacional", "Auditoría Forense", "Electiva Profesional",
                    "Trabajo de Grado I", "Trabajo de Grado II", "Práctica Profesional", "Consultoría Contable"
            }),
            new Programa("DERE", "Derecho", new String[] {
                    "Introducción al Derecho", "Historia del Derecho", "Derecho Constitucional I", "Lógica Jurídica",
                    "Derecho Civil I", "Derecho Constitucional II", "Teoría del Estado", "Derecho Romano",
                    "Derecho Civil II", "Derecho Penal I", "Derecho Administrativo I", "Derechos Humanos",
                    "Derecho Civil III", "Derecho Penal II", "Derecho Administrativo II", "Derecho Laboral I",
                    "Derecho Comercial I", "Derecho Procesal Civil I", "Derecho Laboral II", "Derecho Tributario",
                    "Derecho Comercial II", "Derecho Procesal Civil II", "Derecho Procesal Penal", "Derecho Internacional Público",
                    "Derecho Procesal Laboral", "Derecho de Familia", "Derecho Ambiental", "Derecho Internacional Privado",
                    "Derecho de la Competencia", "Derecho de los Contratos", "Medicina Legal", "Criminología",
                    "Filosofía del Derecho", "Ética Profesional", "Mecanismos Alternativos de Solución de Conflictos", "Electiva Profesional",
                    "Trabajo de Grado I", "Trabajo de Grado II", "Práctica Profesional", "Consultorio Jurídico"
            }),
            new Programa("TGRE", "Tecnología en Gestión de Redes", new String[] {
                    "Introducción a las Redes", "Fundamentos de Programación", "Matemáticas Aplicadas", "Electrónica Básica",
                    "Redes Locales", "Programación de Scripts", "Física Aplicada", "Sistemas Operativos",
                    "Redes de Área Amplia", "Bases de Datos", "Seguridad Informática I", "Administración de Servidores",
                    "Enrutamiento y Conmutación", "Servicios de Red", "Seguridad Informática II", "Virtualización",
                    "Redes Inalámbricas", "Gestión de Redes", "Computación en la Nube", "Monitoreo de Redes",
                    "Redes Definidas por Software", "Telefonía IP", "Almacenamiento en Red", "Automatización de Redes",
                    "Ciberseguridad", "Internet de las Cosas", "Centros de Datos", "Proyectos de Redes",
                    "Auditoría de Redes", "Redes Ópticas", "Gestión de Servicios TI", "Analítica de Redes",
                    "Ética Profesional", "Normatividad de Telecomunicaciones", "Innovación Tecnológica", "Electiva Profesional I",
                    "Trabajo de Grado I", "Trabajo de Grado II", "Práctica Profesional", "Electiva Profesional II"
            })
    );
}

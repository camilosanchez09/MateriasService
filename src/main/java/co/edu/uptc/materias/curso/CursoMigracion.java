package co.edu.uptc.materias.curso;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migración idempotente que corre al arrancar (después de que Hibernate
 * agregó las columnas nuevas con ddl-auto=update): rellena los cupos
 * disponibles de los cursos que ya existían y agrega un CHECK para que
 * nunca queden negativos ni por encima del cupo total.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class CursoMigracion implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        int rellenados = jdbc.update("UPDATE cursos SET cupos_disponibles = cupo WHERE cupos_disponibles IS NULL");
        jdbc.execute("ALTER TABLE cursos ALTER COLUMN estado SET DEFAULT 'ACTIVO'");
        jdbc.update("UPDATE cursos SET estado = 'ACTIVO' WHERE estado IS NULL");
        jdbc.execute("ALTER TABLE cursos ALTER COLUMN estado SET NOT NULL");
        jdbc.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_cursos_cupos_disponibles') THEN
                    ALTER TABLE cursos ADD CONSTRAINT ck_cursos_cupos_disponibles
                      CHECK (cupos_disponibles IS NULL OR (cupos_disponibles >= 0 AND cupos_disponibles <= cupo));
                  END IF;
                END $$;
                """);
        if (rellenados > 0) {
            log.info("Migración de cursos: {} cursos con cupos_disponibles inicializados", rellenados);
        }
    }
}

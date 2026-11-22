-- Corre ANTES de que Hibernate actualice el esquema (spring.sql.init, ver application.properties).
--
-- Materia ahora tiene programa_codigo y semestre, obligatorios por @NotBlank / @NotNull.
-- Hibernate (ddl-auto=update) intentaría agregarlos como NOT NULL, y eso falla en una base
-- que ya tiene materias (las filas existentes quedarían en null): el módulo no arrancaría,
-- por ejemplo, sobre la base de RDS de la nube.
--
-- Aquí se crean como columnas opcionales si la tabla ya existe; el seeder (versión 2)
-- reconstruye el catálogo y las llena. En una base nueva la tabla aún no existe y esto no
-- hace nada: Hibernate la crea completa.
ALTER TABLE IF EXISTS materias ADD COLUMN IF NOT EXISTS programa_codigo varchar(20);
ALTER TABLE IF EXISTS materias ADD COLUMN IF NOT EXISTS semestre integer;

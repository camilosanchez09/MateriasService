# Módulo Materias — Lab 3 (Arquitectura Modular con API Gateway)

Servicio **independiente** del módulo Materias del Sistema Académico: contiene las
entidades **Materia** (catálogo), **Docente** y **Curso** (la oferta real: horario,
período, cupo). Es uno de los tres módulos que el API Gateway del equipo expone bajo
un único punto de entrada.

## Independencia del módulo

- **Repositorio propio**: este proyecto se clona y corre solo, sin código de los
  módulos Estudiantes o Inscripciones.
- **Base de datos propia**: PostgreSQL en su propio contenedor Docker (`materias-db`),
  sin foreign keys hacia las bases de datos de los otros módulos. Cuando Inscripciones
  necesite datos de un curso, se los pide a este servicio por HTTP en tiempo de
  ejecución — nunca los duplica.
- **Ciclo de vida propio**: se levanta, se cae y se reinicia sin tocar a los otros dos.

Dentro del módulo, `Curso` sí tiene relaciones reales (`@ManyToOne`) hacia `Materia`
y `Docente`, porque las tres viven en la misma base de datos — eso es distinto del
caso de un id "externo" hacia otro módulo.

## Stack

- Java 17 + Spring Boot 3.3.4
- Spring Data JPA (Hibernate)
- PostgreSQL 16 (contenedor Docker)
- Bean Validation (jakarta.validation)
- springdoc-openapi (Swagger UI)
- Datafaker (carga inicial de datos ficticios)
- Lombok

## 1. Requisitos

- JDK 17+
- Maven 3.9+
- Docker + Docker Compose

## 2. Levantar la base de datos

```bash
docker compose up -d
```

Levanta PostgreSQL en `localhost:5433` (host) -> `5432` (contenedor), con:
- DB: `materias`
- Usuario: `materias`
- Password: `materias`

El puerto host 5433 se eligió para no chocar con la base de datos del módulo
Estudiantes (que normalmente usa 5432) cuando los 4 procesos corren al tiempo en la
misma máquina. Ya está configurado en `src/main/resources/application.properties`.

## 3. Ejecutar la aplicación

```bash
mvn spring-boot:run
```

Al arrancar por primera vez:
- Hibernate crea automáticamente las tablas (`spring.jpa.hibernate.ddl-auto=update`).
- El `DataSeeder` puebla la base con **20 materias**, **15 docentes** y **30 cursos**
  ficticios (solo si las tablas están vacías, no duplica en reinicios).

La API queda disponible en `http://localhost:8081`.

> Ajusta `server.port` en `application.properties` si tu equipo acordó otros puertos
> para Estudiantes / Inscripciones / Gateway y hay choque.

## 4. Documentación / pruebas (Swagger)

- Swagger UI: **http://localhost:8081/swagger-ui.html**
- Contrato OpenAPI (JSON): http://localhost:8081/v3/api-docs

Desde Swagger UI puedes usar "Try it out" para probar cada endpoint sin leer el
código fuente. Hay 3 grupos de endpoints (Materias, Docentes, Cursos), todos bajo el
prefijo `/api/materias`, que es el que el Gateway enruta hacia este servicio.

## 5. Endpoints principales

| Recurso  | Base                     |
|----------|--------------------------|
| Materias | `/api/materias`          |
| Docentes | `/api/materias/docentes` |
| Cursos   | `/api/materias/cursos`   |

Cada recurso soporta:
- `GET` (lista paginada) — `pageNumber`, `pageSize`, `sortBy`, `sortDirection`.
- `GET /{id}`, `POST`, `PUT /{id}`, `PATCH /{id}`, `DELETE /{id}`.

### Filtros disponibles

**Materias** (`GET /api/materias`)
- `programa` — coincidencia exacta, case-insensitive.
- `search` — texto parcial en nombre o código.
- `creditosMin` — créditos mayores o iguales al valor dado.

**Docentes** (`GET /api/materias/docentes`)
- `especialidad` — coincidencia exacta, case-insensitive.
- `search` — texto parcial en nombre, apellido o correo.

**Cursos** (`GET /api/materias/cursos`)
- `periodo` — coincidencia exacta (ej. `2026-2`).
- `materiaId` / `docenteId` — cursos de una materia o docente puntual.
- `cupoMin` — cupo mayor o igual al valor dado.

### Endpoint que consume el Gateway

`GET /api/materias/cursos/{id}` devuelve el curso ya "aplanado" (con el nombre de la
materia y el id del docente resueltos), y `GET /api/materias/docentes/{id}` devuelve
el docente completo. Son los dos que el Gateway llama en los pasos 3 y 4 de
`/api/estudiantes/{id}/detalle` (ver sección 6.1 del enunciado del laboratorio).

## 6. Manejo de errores

Respuestas de error consistentes vía `GlobalExceptionHandler`:
- `400` — validación fallida o parámetro con formato inválido.
- `404` — recurso (materia, docente o curso) inexistente.
- `409` — conflicto de integridad (código/correo duplicado).
- `500` — error inesperado.

## 7. Estructura del código

```
src/main/java/co/edu/uptc/materias/
├── MateriasServiceApplication.java
├── config/OpenApiConfig.java
├── exception/            (ApiError, ResourceNotFoundException, GlobalExceptionHandler)
├── materia/              (Materia, MateriaController, MateriaService, MateriaRepository, dto/)
├── docente/              (Docente, DocenteController, DocenteService, DocenteRepository, dto/)
├── curso/                (Curso, CursoController, CursoService, CursoRepository, dto/)
└── seed/DataSeeder.java
```

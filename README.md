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

Además levanta **RustFS** (S3 local para los PDFs, puertos 9100/9101) y crea el bucket
`materias-pdfs` (ver sección 8).

El puerto host se puede cambiar con la variable `DB_PORT` (por defecto 5433): el módulo
Estudiantes también publica su base en 5433, así que si corren los dos en la misma máquina
pon, por ejemplo, `DB_PORT=5436` en un archivo `.env` (copia de `.env.example`) y exporta
la misma variable antes de `mvn spring-boot:run`.

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
| PDFs de una materia | `/api/materias/{materiaId}/archivos` (ver sección 8) |

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
- `400` — validación fallida, parámetro con formato inválido o falta el archivo.
- `404` — recurso (materia, docente, curso o archivo) inexistente, o ruta que no existe.
- `409` — conflicto de integridad (código/correo duplicado).
- `413` — el PDF supera 10 MB.
- `415` — el archivo no es PDF.
- `500` — error inesperado.
- `503` — el almacenamiento de archivos (S3) no está disponible.

## 7. Estructura del código

```
src/main/java/co/edu/uptc/materias/
├── MateriasServiceApplication.java
├── config/OpenApiConfig.java
├── exception/            (ApiError, ResourceNotFoundException, GlobalExceptionHandler)
├── materia/              (Materia, MateriaController, MateriaService, MateriaRepository, dto/)
├── docente/              (Docente, DocenteController, DocenteService, DocenteRepository, dto/)
├── curso/                (Curso, CursoController, CursoService, CursoRepository, dto/)
├── archivo/              (ArchivoMateria, controlador, servicio, PdfStorage, S3Config, dto/)
└── seed/DataSeeder.java
```

## 8. PDFs de las materias (Laboratorio 4)

Requisito 5.2: subir y listar PDFs con el contenido temático de cada materia (máximo 10 MB),
guardados en el **almacenamiento de objetos**, nunca en la base ni en el disco del contenedor.

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/materias/{materiaId}/archivos` | Sube un PDF (multipart, campo `file`) → 201 |
| `GET` | `/api/materias/{materiaId}/archivos` | Lista los PDFs (más reciente primero) |
| `GET` | `/api/materias/{materiaId}/archivos/{archivoId}/url` | URL firmada y temporal para descargar → `{ url, expiresIn }` |
| `DELETE` | `/api/materias/{materiaId}/archivos/{archivoId}` | Elimina metadatos y objeto |

- **Solo PDF de verdad:** se valida por los primeros bytes (`%PDF-`), no por la extensión ni
  el Content-Type que manda el cliente (`415` si no lo es). Más de 10 MB → `413`.
- **La base guarda solo metadatos** en la tabla `materia_archivos`: clave del objeto, nombre
  original, tipo, tamaño y fecha. Ni el binario ni la URL.
- **Bucket privado:** la descarga es con una URL **firmada** (SigV4) que vence en `PDF_URL_TTL`
  (10 min por defecto). Sin firma, S3 responde `403`.
- Si guardar los metadatos falla, se borra el objeto recién subido (sin huérfanos). Al eliminar
  una materia se eliminan sus PDFs del bucket, solo si la eliminación se confirma.
- A través del Gateway: subir exige rol `ADMIN`; listar y descargar, cualquier usuario autenticado.
  Estas rutas no se guardan en el caché del Gateway (las URLs firmadas vencen).

### Local: RustFS

`docker compose up -d` levanta RustFS (compatible con S3; MinIO ya no publica imágenes) en
`localhost:9100` (consola en `:9101`) y crea el bucket con `amazon/aws-cli`. Para que la API lo use,
exporta las variables del `.env.example` antes de arrancar:

```powershell
# PowerShell
$env:S3_ENDPOINT="http://localhost:9100"; $env:S3_FORCE_PATH_STYLE="true"
$env:AWS_ACCESS_KEY_ID="materias_s3_local"; $env:AWS_SECRET_ACCESS_KEY="<la del .env>"
mvn spring-boot:run
```

```bash
curl -F "file=@unidad1.pdf" http://localhost:8081/api/materias/1/archivos
curl http://localhost:8081/api/materias/1/archivos/1/url
```

### En AWS

- Bucket S3 **privado** (Block Public Access activado). Variables: `S3_BUCKET`, `S3_REGION`,
  `PDF_URL_TTL`. **No** se definen `S3_ENDPOINT`, `S3_FORCE_PATH_STYLE` ni claves `AWS_*`:
  el SDK usa las credenciales temporales del rol de la tarea (sin secretos en git ni en la imagen).
- Permisos mínimos del módulo, solo sobre su bucket: `s3:PutObject`, `s3:GetObject`,
  `s3:DeleteObject` en `arn:aws:s3:::<bucket>/materias/*`.
- El SDK usa el cliente HTTP del JDK (`url-connection-client`): los clientes Apache del SDK
  chocan con la versión de HttpClient que fija Spring Boot 3.3.

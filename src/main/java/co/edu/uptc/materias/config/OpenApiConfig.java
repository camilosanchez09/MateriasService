package co.edu.uptc.materias.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI materiasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Módulo Materias - API")
                        .description("Lab 3 - Arquitectura Modular con API Gateway. "
                                + "Servicio independiente con su propia base de datos para "
                                + "Materia (catálogo), Docente y Curso (oferta real).")
                        .version("v1"));
    }
}

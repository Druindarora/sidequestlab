package dev.sidequestlab.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(info = @Info(title = "MemoQuiz API", version = "v1", description = "API for MemoQuiz"))
@Configuration
public class OpenApiConfig {

}

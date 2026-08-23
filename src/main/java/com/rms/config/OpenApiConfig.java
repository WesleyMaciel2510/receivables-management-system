package com.rms.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Sistema de Gestão de Recebíveis API", version = "v1",
                description = "API para cadastro de clientes e controle de recebíveis."),
        servers = @Server(url = "http://localhost:8080", description = "Ambiente local"))
public class OpenApiConfig {
}

package com.zez.consumer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Consumer API")
                        .version("1.0.0")
                        .description("""
                                Query API for processed payment notifications.
                                Events are consumed from Apache Kafka, deduplicated via idempotency keys,
                                persisted to PostgreSQL, and forwarded to the WebSocket Gateway.
                                """)
                        .contact(new Contact()
                                .name("ZEZ Engineering")
                                .email("engineering@zez.io"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local Development")
                ));
    }
}

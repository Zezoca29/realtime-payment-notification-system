package com.zez.payment.config;

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
                        .title("Payment Producer API")
                        .version("1.0.0")
                        .description("""
                                REST API for initiating payment events in the
                                Realtime Payment Notification System.
                                Events are published to Apache Kafka for downstream processing.
                                """)
                        .contact(new Contact()
                                .name("ZEZ Engineering")
                                .email("engineering@zez.io"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local Development")
                ));
    }
}

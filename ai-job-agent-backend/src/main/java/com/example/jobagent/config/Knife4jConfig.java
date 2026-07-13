package com.example.jobagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI 求职投递助手 Agent API")
                        .description("面向大学生、实习生和初级开发者的 AI 求职投递助手")
                        .version("1.0.0"));
    }
}

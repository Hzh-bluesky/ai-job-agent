package com.example.jobagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.jobagent.mapper")
@SpringBootApplication
public class JobAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobAgentApplication.class, args);
    }
}

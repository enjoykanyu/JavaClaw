package com.kanyu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.kanyu")
@EnableScheduling
public class JavaClawApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaClawApplication.class, args);
    }
}

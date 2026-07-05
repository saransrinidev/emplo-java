package com.emplo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EmploApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmploApplication.class, args);
    }
}

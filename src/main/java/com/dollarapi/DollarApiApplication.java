package com.dollarapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DollarApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DollarApiApplication.class, args);
    }
}

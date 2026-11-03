package com.keyloop.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ServiceSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceSchedulerApplication.class, args);
    }
}

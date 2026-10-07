package com.meditrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeditrackApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeditrackApplication.class, args);
    }
}

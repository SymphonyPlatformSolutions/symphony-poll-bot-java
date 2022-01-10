package com.symphony.devrel.pollbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class PollBot {
    public static void main(String[] args) {
        SpringApplication.run(PollBot.class, args);
    }
}
